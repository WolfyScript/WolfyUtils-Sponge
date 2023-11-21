package com.wolfyscript.utilities.sponge.gui.listeners;

import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.gui.GuiViewManager;
import com.wolfyscript.utilities.sponge.WolfyUtilsSponge;
import com.wolfyscript.utilities.sponge.gui.GUIHolder;
import com.wolfyscript.utilities.sponge.gui.GuiViewManagerImpl;
import com.wolfyscript.utilities.sponge.gui.RenderContextImpl;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;

import java.util.Optional;

public class GUIListeners {

    private PluginContainer plugin;

    public GUIListeners(PluginContainer plugin) {
        this.plugin = plugin;
    }

    private static Optional<GUIHolder> findGuiHolder(Inventory inventory) {
        if (inventory instanceof CarriedInventory<?> carriedInventory) {
            return carriedInventory.carrier().map(carrier -> {
                if (carrier instanceof GUIHolder holder) {
                    return holder;
                }
                return null;
            });
        }
        return Optional.empty();
    }

    @Listener
    public void onClickPrimary(ClickContainerEvent.Primary event) {
        Inventory inventory = event.inventory();
        findGuiHolder(inventory).ifPresent(holder -> {


        });
    }

    @Listener
    public void onClickDrag(ClickContainerEvent.Drag event) {
        Inventory inventory = event.inventory();
        findGuiHolder(inventory).ifPresent(holder -> {
            for (SlotTransaction transaction : event.transactions()) {
                transaction.slot().get(Keys.SLOT_INDEX).ifPresent(slotIndex -> {
                    if (slotIndex == 11) {
                        transaction.invalidate();
                    }
                });
            }
        });
    }

    @Listener
    public void onClickSlot(ClickContainerEvent event) {
        Inventory inventory = event.inventory();
        findGuiHolder(inventory).ifPresent(holder -> {


        });

    }


    private static void updateWindow(ServerPlayer serverPlayer, GuiViewManager viewManager, WolfyUtils wolfyUtils) {
        Task task = Task.builder().execute(() -> {
            viewManager.getRenderContext(serverPlayer.uniqueId()).ifPresent(context -> {
                ((GuiViewManagerImpl) viewManager).renderFor(serverPlayer, (RenderContextImpl) context);
            });
        }).delay(Ticks.of(2)).plugin(((WolfyUtilsSponge) wolfyUtils).getPluginContainer()).build();
        Sponge.server().scheduler().submit(task);
    }
}
