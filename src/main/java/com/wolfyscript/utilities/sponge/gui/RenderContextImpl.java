package com.wolfyscript.utilities.sponge.gui;

import com.wolfyscript.utilities.common.gui.Component;
import com.wolfyscript.utilities.common.gui.RenderContext;
import com.wolfyscript.utilities.common.gui.Router;
import com.wolfyscript.utilities.common.gui.Window;
import com.wolfyscript.utilities.common.items.ItemStackConfig;
import com.wolfyscript.utilities.sponge.world.items.SpongeItemStackConfig;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;

public class RenderContextImpl implements RenderContext {

    private final InventoryMenu inventory;
    private final Window window;
    private final Router router;
    private Component currentNode;
    private int slotOffsetToParent;

    public RenderContextImpl(InventoryMenu inventory, Router router, Window window) {
        this.inventory = inventory;
        this.router = router;
        this.window = window;
        this.slotOffsetToParent = 0;
        this.currentNode = null;
    }

    public void setSlotOffsetToParent(int offset) {
        this.slotOffsetToParent = offset;
    }

    @Override
    public int getCurrentOffset() {
        return slotOffsetToParent;
    }

    public void enterNode(Component component) {
        this.currentNode = component;
    }

    public void exitNode() {
        this.currentNode = null;
    }

    InventoryMenu getInventory() {
        return inventory;
    }

    @Override
    public Component getCurrentComponent() {
        return currentNode;
    }

    @Override
    public void setStack(int i, ItemStackConfig<?> itemStackConfig) {
        //checkIfSlotInBounds(i);
        if (!(itemStackConfig instanceof SpongeItemStackConfig bukkitItemStackConfig))
            throw new IllegalArgumentException(String.format("Cannot render stack config! Invalid stack config type! Expected '%s' but received '%s'.", SpongeItemStackConfig.class.getName(), itemStackConfig.getClass().getName()));

        inventory.inventory().set(i, bukkitItemStackConfig.constructItemStack());
    }

    @Override
    public void setNativeStack(int i, Object object) {
        //checkIfSlotInBounds(i);
        if (object == null) {
            inventory.inventory().set(i, null);
            return;
        }
        if (!(object instanceof ItemStack itemStack))
            throw new IllegalArgumentException(String.format("Cannot render native stack! Invalid native stack type! Expected '%s' but received '%s'.", ItemStack.class.getName(), object.getClass().getName()));

        inventory.inventory().set(i, itemStack);
    }

}
