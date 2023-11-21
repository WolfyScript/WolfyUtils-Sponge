package com.wolfyscript.utilities.sponge.gui;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.wolfyscript.utilities.KeyedStaticId;
import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.callback.InteractionCallback;
import com.wolfyscript.utilities.common.gui.functions.SerializableSupplier;
import com.wolfyscript.utilities.sponge.WolfyUtilsSponge;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ContainerType;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;

import java.util.*;
import java.util.function.Consumer;

@KeyedStaticId(key = "window")
public final class WindowImpl implements Window {

    private final String id;
    private final Router router;
    private final WolfyUtils wolfyUtils;
    private final Consumer<WindowDynamicConstructor> rendererConstructor;
    private final Integer size;
    private final WindowType type;
    private String staticTitle = null;
    private SerializableSupplier<net.kyori.adventure.text.Component> dynamicTitle;
    private final InteractionCallback interactionCallback;
    final Multimap<Component, Integer> staticComponents;
    final Multimap<ComponentBuilder<?, ?>, Integer> nonRenderedComponents;

    WindowImpl(String id,
               Router router,
               Integer size,
               WindowType type,
               String staticTitle,
               InteractionCallback interactionCallback,
               Multimap<Component, Integer> staticComponents,
               Multimap<ComponentBuilder<?, ?>, Integer> nonRenderedComponents,
               Consumer<WindowDynamicConstructor> rendererConstructor) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(interactionCallback);
        Preconditions.checkArgument(size != null || type != null, "Either type or size must be specified!");
        this.id = id;
        this.router = router;
        this.wolfyUtils = router.getWolfyUtils();
        this.rendererConstructor = rendererConstructor;
        this.size = size;
        this.type = type;
        this.staticTitle = staticTitle;
        this.interactionCallback = interactionCallback;
        this.staticComponents = staticComponents;
        this.nonRenderedComponents = nonRenderedComponents;
    }

    WindowImpl(WindowImpl staticWindow) {
        this.id = staticWindow.id;
        this.router = staticWindow.router;
        this.wolfyUtils = staticWindow.router.getWolfyUtils();
        this.rendererConstructor = staticWindow.rendererConstructor;
        this.size = staticWindow.size;
        this.type = staticWindow.type;
        this.staticTitle = staticWindow.staticTitle;
        this.dynamicTitle = staticWindow.dynamicTitle;
        this.interactionCallback = staticWindow.interactionCallback;
        this.staticComponents = MultimapBuilder.hashKeys().arrayListValues().build(staticWindow.staticComponents);
        this.nonRenderedComponents = MultimapBuilder.hashKeys().arrayListValues().build(staticWindow.nonRenderedComponents);
    }

    public WindowImpl dynamicCopy(Multimap<Component, Integer> dynamicComponents, Multimap<ComponentBuilder<?, ?>, Integer> nonRenderedComponents, SerializableSupplier<net.kyori.adventure.text.Component> dynamicTitle) {
        WindowImpl copy = new WindowImpl(this);
        copy.staticComponents.putAll(dynamicComponents);
        copy.nonRenderedComponents.putAll(nonRenderedComponents);
        copy.dynamicTitle = dynamicTitle;
        return copy;
    }

    @Override
    public Window construct(GuiHolder guiHolder, GuiViewManager guiViewManager) {
        var builder = new WindowDynamicConstructorImpl(wolfyUtils, guiHolder, this);
        rendererConstructor.accept(builder);
        builder.usedSignals.forEach((s, signal) -> signal.update(o -> o));
        return builder.create(this);
    }

    @Override
    public void open(GuiViewManager guiViewManager) {

    }

    @Override
    public void render(GuiHolder guiHolder, GuiViewManager viewManager, RenderContext context) {
        if (!(context instanceof RenderContextImpl renderContext)) return;

        if (dynamicTitle != null) {
            ((GUIHolder) guiHolder).getNativePlayer().openInventory().flatMap(Container::currentMenu).ifPresent(inventoryMenu -> {
                inventoryMenu.setTitle(dynamicTitle.get());
            });
        }

        for (Map.Entry<Component, Integer> entry : staticComponents.entries()) {
            int slot = entry.getValue();
            Component component = entry.getKey();
            renderContext.setSlotOffsetToParent(slot);
            ((GuiViewManagerImpl) guiHolder.getViewManager()).updateLeaveNodes(component, slot);
            renderContext.enterNode(component);
            if (component.construct(guiHolder, viewManager) instanceof SignalledObject signalledObject) {
                signalledObject.update(viewManager, guiHolder, renderContext);
            }
            renderContext.exitNode();
        }
    }

    @Override
    public WolfyUtils getWolfyUtils() {
        return wolfyUtils;
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public Router router() {
        return router;
    }

    @Override
    public RenderContext createContext(GuiViewManager viewManager, UUID player) {
        return Sponge.server().player(player).map(serverPlayer -> {
            Optional<Container> openInv = serverPlayer.openInventory();
            if (openInv.isPresent()) {
                Container container = openInv.get();
                Optional<InventoryMenu> menuOptional = container.currentMenu();
                if (menuOptional.isPresent()) {
                    InventoryMenu menu = menuOptional.get();
                    if (menu.inventory() instanceof CarriedInventory<?> carriedInventory) {
                        Optional<?> carrier = carriedInventory.carrier();
                        if (carrier.isPresent() && carrier.get() instanceof GUIHolder holder)
                            if (Objects.equals(holder.getCurrentWindow(), this)) {
                                // Still in the same window, we can just update it.
                                return new RenderContextImpl(menu, viewManager.getRouter(), this);
                            }
                    }
                }
            }
            Logger logger = ((WolfyUtilsSponge) wolfyUtils).getPluginContainer().logger();

            // No active Window or it is another Window, need to recreate inventory
            final InventoryMenu menu;
            final GUIHolder holder = new GUIHolder(serverPlayer, viewManager, this);
            final net.kyori.adventure.text.Component title = createTitle(holder);

            menu = ViewableInventory.builder()
                    .type(this::getInventoryType)
                    .completeStructure()
                    .carrier(holder)
                    .plugin(((WolfyUtilsSponge) wolfyUtils).getPluginContainer()) // TODO
                    .build()
                    .asMenu();
            menu.setReadOnly(false);
//            menu.registerClick((cause, container, clickType) -> {
//                logger.info("Test!");
//                return true;
//            });
//            menu.registerSlotClick((cause, container, slot, slotIndex, clickType) -> {
//                // Update a tick later, so that this event is done and doesn't interfere.
//                updateWindow(serverPlayer, viewManager, wolfyUtils);
//
//                GuiViewManagerImpl guiViewManager = (GuiViewManagerImpl) viewManager;
//                return viewManager.getCurrentMenu().map(window -> {
//                    InteractionResult result = guiViewManager.getLeaveNode(slotIndex)
//                            .map(component -> {
//                                if (component instanceof Interactable interactable) {
//                                    return interactable.interact(holder, new ClickInteractionDetailsImpl(clickType, slot, slotIndex, -1));
//                                }
//                                return InteractionResult.cancel(true);
//                            })
//                            .orElse(InteractionResult.cancel(true));
//                    return !result.isCancelled();
//                }).orElse(false);
//            });
//            menu.registerChange((cause, container, slot, slotIndex, oldStack, newStack) -> {
//                logger.info("Test 2!");
//                // Update a tick later, so that this event is done and doesn't interfere.
//                updateWindow(serverPlayer, viewManager, wolfyUtils);
//
//                return true;
//            });
//            menu.registerKeySwap((cause, container, slot, slotIndex, clickType, slot2) -> {
//                // Update a tick later, so that this event is done and doesn't interfere.
//                updateWindow(serverPlayer, viewManager, wolfyUtils);
//
//                GuiViewManagerImpl guiViewManager = (GuiViewManagerImpl) viewManager;
//                return viewManager.getCurrentMenu().map(window -> {
//                    InteractionResult result = guiViewManager.getLeaveNode(slotIndex)
//                            .map(component -> {
//                                if (component instanceof Interactable interactable) {
//                                    return interactable.interact(holder, new ClickInteractionDetailsImpl(clickType, slot, slotIndex, slot2.get(Keys.SLOT_INDEX).orElse(-1)));
//                                }
//                                return InteractionResult.cancel(true);
//                            })
//                            .orElse(InteractionResult.cancel(true));
//                    return !result.isCancelled();
//                }).orElse(false);
//            });
            menu.setTitle(title);
            holder.setActiveMenu(menu);
            return new RenderContextImpl(menu, viewManager.getRouter(), this);
        }).orElse(null);
    }

    private static void updateWindow(ServerPlayer serverPlayer, GuiViewManager viewManager, WolfyUtils wolfyUtils) {
        Task task = Task.builder().execute(() -> {
            viewManager.getRenderContext(serverPlayer.uniqueId()).ifPresent(context -> {
                ((GuiViewManagerImpl) viewManager).renderFor(serverPlayer, (RenderContextImpl) context);
            });
        }).delay(Ticks.of(2)).plugin(((WolfyUtilsSponge) wolfyUtils).getPluginContainer()).build();
        Sponge.server().scheduler().submit(task);
    }

    private ContainerType getInventoryType() {
        return getType().map(type -> switch (type) {
            case CUSTOM -> getSize().flatMap(size -> {
                int rows = size / 9;
                return ContainerTypes.registry().<ContainerType>findValue(ResourceKey.minecraft("generic_9x" + rows));
            }).orElse(ContainerTypes.GENERIC_9X6.get());
            case HOPPER -> ContainerTypes.HOPPER.get();
            case DROPPER, DISPENSER -> ContainerTypes.GENERIC_3X3.get();
        }).orElseGet(() ->
                getSize().flatMap(size -> {
                    int rows = size / 9;
                    return ContainerTypes.registry().<ContainerType>findValue(ResourceKey.minecraft("generic_9x" + rows));
                }).orElse(ContainerTypes.GENERIC_9X6.get())
        );
    }

    @Override
    public InteractionResult interact(GuiHolder holder, InteractionDetails interactionDetails) {
        return null;
    }

    @Override
    public InteractionCallback interactCallback() {
        return interactionCallback;
    }

    @Override
    public Set<? extends Component> childComponents() {
        return Set.of();
    }

    @Override
    public Optional<Component> getChild(String id) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getSize() {
        return Optional.ofNullable(size);
    }

    @Override
    public Optional<WindowType> getType() {
        return Optional.ofNullable(type);
    }

    @Override
    public net.kyori.adventure.text.Component createTitle(GuiHolder holder) {
        return wolfyUtils.getChat().getMiniMessage().deserialize(staticTitle);
    }

    public String getStaticTitle() {
        return staticTitle;
    }

    @Override
    public int width() {
        return size / height();
    }

    @Override
    public int height() {
        return size / 9;
    }

}
