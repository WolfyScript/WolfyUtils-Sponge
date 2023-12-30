package com.wolfyscript.utilities.sponge.gui;

import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.gui.*;
import com.wolfyscript.utilities.platform.gui.GuiUtils;
import com.wolfyscript.utilities.sponge.WolfyUtilsSponge;
import com.wolfyscript.utilities.sponge.adapters.PlayerImpl;
import net.kyori.adventure.identity.Identity;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.*;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class GuiUtilsImpl implements GuiUtils {

    @Override
    public RenderContext createRenderContext(Window window, GuiViewManager viewManager, UUID viewer) {
        WolfyUtils wolfyUtils = window.getWolfyUtils();

        return Sponge.server().player(viewer).map(serverPlayer -> {
            Optional<Container> openInv = serverPlayer.openInventory();
            if (openInv.isPresent()) {
                Container container = openInv.get();
                Optional<InventoryMenu> menuOptional = container.currentMenu();
                if (menuOptional.isPresent()) {
                    InventoryMenu menu = menuOptional.get();
                    if (menu.inventory() instanceof CarriedInventory<?> carriedInventory) {
                        Optional<?> carrier = carriedInventory.carrier();
                        if (carrier.isPresent() && carrier.get() instanceof GuiHolderImpl holder)
                            if (Objects.equals(holder.getCurrentWindow(), window)) {
                                // Still in the same window, we can just update it.
                                return new RenderContextImpl(holder, menu, viewManager.getRouter(), window);
                            }
                    }
                }
            }
            Logger logger = ((WolfyUtilsSponge) wolfyUtils).getPluginContainer().logger();

            // No active Window or it is another Window, need to recreate inventory
            final InventoryMenu menu;
            final GuiHolder holder = new GuiHolderImpl(window, viewManager, new PlayerImpl(serverPlayer));
            final GuiCarrier carrier = new GuiCarrier(holder);
            final net.kyori.adventure.text.Component title = window.createTitle(holder);

            menu = ViewableInventory.builder()
                    .type(() -> getInventoryType(window))
                    .completeStructure()
                    .carrier(carrier)
                    .plugin(((WolfyUtilsSponge) wolfyUtils).getPluginContainer()) // TODO
                    .build().asMenu();
            carrier.setMenu(menu);
            return new RenderContextImpl(holder, menu, viewManager.getRouter(), window);
        }).orElse(null);
    }

    private static ContainerType getInventoryType(Window window) {
        return window.getType().map(type -> switch (type) {
            case CUSTOM -> window.getSize().flatMap(size -> {
                int rows = size / 9;
                return ContainerTypes.registry().<ContainerType>findValue(ResourceKey.minecraft("generic_9x" + rows));
            }).orElse(ContainerTypes.GENERIC_9X6.get());
            case HOPPER -> ContainerTypes.HOPPER.get();
            case DROPPER, DISPENSER -> ContainerTypes.GENERIC_3X3.get();
        }).orElseGet(() ->
                window.getSize().flatMap(size -> {
                    int rows = size / 9;
                    return ContainerTypes.registry().<ContainerType>findValue(ResourceKey.minecraft("generic_9x" + rows));
                }).orElse(ContainerTypes.GENERIC_9X6.get())
        );
    }

}
