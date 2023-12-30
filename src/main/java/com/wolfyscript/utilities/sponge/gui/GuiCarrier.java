package com.wolfyscript.utilities.sponge.gui;

import com.wolfyscript.utilities.gui.GuiHolder;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.CarriedInventory;

public class GuiCarrier implements Carrier {

    private InventoryMenu menu;
    private final GuiHolder holder;

    public GuiCarrier(GuiHolder holder) {
        this.holder = holder;
    }

    public void setMenu(InventoryMenu menu) {
        this.menu = menu;
    }

    public GuiHolder holder() {
        return holder;
    }

    @Override
    public CarriedInventory<GuiCarrier> inventory() {
        return (CarriedInventory<GuiCarrier>) menu.inventory();
    }
}
