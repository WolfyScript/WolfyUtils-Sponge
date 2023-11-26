package com.wolfyscript.utilities.sponge.gui;

import com.wolfyscript.utilities.common.gui.*;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.CarriedInventory;

public class GUIHolder extends GuiHolderCommonImpl implements Carrier {

    private InventoryMenu menu;
    private final ServerPlayer player;

    public GUIHolder(ServerPlayer player, GuiViewManager viewManager, Window window) {
        super(window, viewManager);
        this.player = player;
    }

    public ServerPlayer getNativePlayer() {
        return player;
    }

    @Override
    public com.wolfyscript.utilities.common.adapters.Player getPlayer() {
        return null;//new PlayerImpl(player);
    }

    void setActiveMenu(InventoryMenu menu) {
        this.menu = menu;
    }

    @Override
    public CarriedInventory<GUIHolder> inventory() {
        return (CarriedInventory<GUIHolder>) menu.inventory();
    }
}
