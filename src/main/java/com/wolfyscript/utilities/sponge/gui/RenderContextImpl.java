package com.wolfyscript.utilities.sponge.gui;

import com.wolfyscript.utilities.eval.context.EvalContext;
import com.wolfyscript.utilities.gui.*;
import com.wolfyscript.utilities.sponge.adapters.ItemStackImpl;
import com.wolfyscript.utilities.sponge.adapters.PlayerImpl;
import com.wolfyscript.utilities.sponge.eval.context.EvalContextPlayer;
import com.wolfyscript.utilities.sponge.world.items.SpongeItemStackConfig;
import com.wolfyscript.utilities.world.items.ItemStackConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

import java.util.Objects;
import java.util.UUID;

public class RenderContextImpl implements RenderContext {

    private final GuiHolder holder;
    private final InventoryMenu menu;
    private final Window window;
    private final Router router;
    private Component currentNode;
    private int slotOffsetToParent;

    public RenderContextImpl(GuiHolder holder, InventoryMenu menu, Router router, Window window) {
        this.holder = holder;
        this.menu = menu;
        this.router = router;
        this.window = window;
        this.slotOffsetToParent = 0;
        this.currentNode = null;
    }

    public void setSlotOffsetToParent(int offset) {
        this.slotOffsetToParent = offset;
    }

    @Override
    public GuiHolder holder() {
        return holder;
    }

    public void enterNode(Component component) {
        this.currentNode = component;
    }

    public void exitNode() {
        this.currentNode = null;
    }

    InventoryMenu getMenu() {
        return menu;
    }

    @Override
    public Component getCurrentComponent() {
        return currentNode;
    }

    @Override
    public int currentOffset() {
        return slotOffsetToParent;
    }

    @Override
    public void renderStack(Position position, com.wolfyscript.utilities.platform.adapters.ItemStack itemStack) {
        if (itemStack == null) {
            setNativeStack(currentOffset() + position.slot(), ItemStack.empty());
            return;
        }
        if (!(itemStack instanceof ItemStackImpl stack))
            throw new IllegalArgumentException(String.format("Cannot render stack! Invalid stack config type! Expected '%s' but received '%s'.", ItemStackImpl.class.getName(), itemStack.getClass().getName()));

        setNativeStack(currentOffset() + position.slot(), stack.spongeRef());
    }

    @Override
    public void renderStack(Position position, ItemStackConfig itemStackConfig, ItemStackContext itemStackContext) {
        if (!(itemStackConfig instanceof SpongeItemStackConfig bukkitItemStackConfig))
            throw new IllegalArgumentException(String.format("Cannot render stack config! Invalid stack config type! Expected '%s' but received '%s'.", SpongeItemStackConfig.class.getName(), itemStackConfig.getClass().getName()));

        setNativeStack(
                currentOffset() + position.slot(),
                bukkitItemStackConfig.constructItemStack(null, router.getWolfyUtils().getChat().getMiniMessage(), itemStackContext.resolvers()).spongeRef()
        );
    }

    @Override
    public void setStack(int i, ItemStackConfig itemStackConfig) {
        if (itemStackConfig == null) {
            menu.inventory().set(i, ItemStack.empty());
            return;
        }
        if (!(itemStackConfig instanceof SpongeItemStackConfig stackConfig))
            throw new IllegalArgumentException(String.format("Cannot render stack config! Invalid stack config type! Expected '%s' but received '%s'.", SpongeItemStackConfig.class.getName(), itemStackConfig.getClass().getName()));

        setNativeStack(i, stackConfig.constructItemStack().spongeRef());
    }

    private void setNativeStack(int i, Object object) {
        //checkIfSlotInBounds(i);
        if (object == null) {
            menu.inventory().set(i, ItemStack.empty());
            return;
        }
        if (!(object instanceof ItemStack itemStack))
            throw new IllegalArgumentException(String.format("Cannot render native stack! Invalid native stack type! Expected '%s' but received '%s'.", ItemStack.class.getName(), object.getClass().getName()));

        menu.inventory().set(i, itemStack);
    }

    @Override
    public void updateTitle(GuiHolder guiHolder, net.kyori.adventure.text.Component component) {
        ((PlayerImpl) guiHolder.getPlayer()).spongeRef().openInventory()
                .flatMap(Container::currentMenu)
                .ifPresent(inventoryMenu -> inventoryMenu.setTitle(component));
    }

    @Override
    public ItemStackContext createContext(GuiHolder guiHolder, TagResolver tagResolver) {
        return new ItemStackContext() {
            @Override
            public TagResolver resolvers() {
                return tagResolver;
            }

            @Override
            public MiniMessage miniMessage() {
                return window.getWolfyUtils().getChat().getMiniMessage();
            }

            @Override
            public EvalContext evalContext() {
                return Sponge.server().player(guiHolder.getPlayer().uuid()).map(serverPlayer -> new EvalContextPlayer(serverPlayer)).orElse(null);
            }

            @Override
            public GuiHolder holder() {
                return guiHolder;
            }
        };
    }

    @Override
    public void openAndRenderMenuFor(GuiViewManager guiViewManager, UUID uuid) {
        Sponge.server().player(uuid).ifPresent(player -> {
            if (!Objects.equals(player.openInventory().flatMap(Container::currentMenu).orElse(null), menu)) {
                menu.open(player).ifPresent(container -> guiViewManager.getCurrentMenu().ifPresent(window -> {
                    if (container.viewed().get(0) instanceof CarriedInventory<?> viewableInventory) {
                        ((CarriedInventory<GuiCarrier>) viewableInventory).carrier().ifPresent(guiCarrier -> {
                            var dynamic = window.construct(guiCarrier.holder(), guiViewManager);
                            dynamic.open(guiViewManager);
                            dynamic.render(guiCarrier.holder(), guiViewManager, this);
                            ((GuiViewManagerImpl) guiViewManager).setCurrentRoot(dynamic);
                        });
                    }
                }));
            }
            guiViewManager.updateSignalQueue(this);
        });
    }

}
