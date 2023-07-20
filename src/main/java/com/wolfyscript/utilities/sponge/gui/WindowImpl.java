package com.wolfyscript.utilities.sponge.gui;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.wolfyscript.utilities.KeyedStaticId;
import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.sponge.WolfyUtilsSponge;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ContainerType;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.api.util.Ticks;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@KeyedStaticId(key = "window")
public final class WindowImpl implements Window {

    private final String id;
    private final Router router;
    private final WolfyUtils wolfyUtils;
    private final BiMap<String, Component> children;
    private final Consumer<com.wolfyscript.utilities.common.gui.WindowRenderer.Builder> rendererConstructor;
    private final Integer size;
    private final WindowType type;
    private String staticTitle = null;
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
               Consumer<com.wolfyscript.utilities.common.gui.WindowRenderer.Builder> rendererConstructor) {
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
        this.children = HashBiMap.create();
    }

    @Override
    public WindowRenderer construct(GuiViewManager viewManager) {
        var rendererBuilder = new WindowRenderer.Builder(wolfyUtils, viewManager, this);
        rendererConstructor.accept(rendererBuilder);
        return rendererBuilder.create(this);
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
            menu.registerClick((cause, container, clickType) -> {
                logger.info("Test!");
                return true;
            });
            menu.registerSlotClick((cause, container, slot, slotIndex, clickType) -> {
                GuiViewManagerImpl guiViewManager = (GuiViewManagerImpl) viewManager;
                boolean execute = viewManager.getCurrentMenu().map(window -> {
                    InteractionResult result = guiViewManager.getLeaveNode(slotIndex)
                            .map(component -> {
                                if (component instanceof Interactable interactable) {
                                    return interactable.interact(holder, null); // TODO
                                }
                                return InteractionResult.cancel(true);
                            })
                            .orElse(InteractionResult.cancel(true));
                    return !result.isCancelled();
                }).orElse(false);

                Task task = Task.builder().execute(() -> {
                    viewManager.getRenderContext(player).ifPresent(context -> {
                        ((GuiViewManagerImpl) viewManager).renderFor(serverPlayer, (RenderContextImpl) context);
                    });
                }).delay(Ticks.of(1)).plugin(((WolfyUtilsSponge) wolfyUtils).getPluginContainer()).build();
                Sponge.server().scheduler().submit(task);

                return execute;
            });
            menu.registerChange((cause, container, slot, slotIndex, oldStack, newStack) -> {
                logger.info("Test 2!");
                // TODO
                return true;
            });
            menu.registerKeySwap((cause, container, slot, slotIndex, clickType, slot2) -> {
                logger.info("Test 3!");
                // TODO
                return false;
            });
            menu.setTitle(title);
            holder.setActiveMenu(menu);
            return new RenderContextImpl(menu, viewManager.getRouter(), this);
        }).orElse(null);
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
        return children.values();
    }

    @Override
    public Optional<Component> getChild(String id) {
        return Optional.ofNullable(children.get(id));
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

    @Override
    public WindowRenderer getRenderer() {
        return null;
    }

    @Override
    public void open(GuiViewManager guiViewManager) {

    }
}
