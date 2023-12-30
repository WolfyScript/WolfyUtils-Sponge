package com.wolfyscript.utilities.sponge.gui.listeners;

import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.gui.*;
import com.wolfyscript.utilities.sponge.WolfyUtilsSponge;
import com.wolfyscript.utilities.sponge.adapters.PlayerImpl;
import com.wolfyscript.utilities.sponge.gui.ClickInteractionDetailsImpl;
import com.wolfyscript.utilities.sponge.gui.GuiCarrier;
import com.wolfyscript.utilities.sponge.gui.RenderContextImpl;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.item.inventory.TransferInventoryEvent;
import org.spongepowered.api.event.item.inventory.container.ClickContainerEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.item.inventory.menu.handler.CloseHandler;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;

import java.awt.event.ContainerEvent;
import java.util.Optional;

public class GUIListeners {

    private PluginContainer plugin;

    public GUIListeners(PluginContainer plugin) {
        this.plugin = plugin;
    }

    private static Optional<GuiHolder> findGuiHolder(Inventory inventory) {
        if (inventory instanceof CarriedInventory<?> carriedInventory) {
            return carriedInventory.carrier().map(carrier -> {
                if (carrier instanceof GuiCarrier holder) {
                    return holder.holder();
                }
                return null;
            });
        }
        return Optional.empty();
    }

    @Listener
    public void onClickPrimary(ClickContainerEvent.Primary event) {
        event.slot().ifPresent(slot -> slot.get(Keys.SLOT_INDEX).ifPresent(index -> onClick(event, ClickTypes.CLICK_LEFT.get(), slot, index)));
    }

    @Listener
    public void onClickSecondary(ClickContainerEvent.Secondary event) {
        event.slot().ifPresent(slot -> slot.get(Keys.SLOT_INDEX).ifPresent(index -> onClick(event, ClickTypes.CLICK_RIGHT.get(), slot, index)));
    }

    @Listener
    public void onClickMiddle(ClickContainerEvent.Middle event) {
        event.slot().ifPresent(slot -> slot.get(Keys.SLOT_INDEX).ifPresent(index -> onClick(event, ClickTypes.CLICK_MIDDLE.get(), slot, index)));
    }

    private void onClick(ClickContainerEvent event, ClickType<?> clickType, Slot slot, int slotIndex) {
        findGuiHolder(event.inventory()).ifPresent(holder -> {
            GuiViewManagerImpl guiViewManager = (GuiViewManagerImpl) holder.getViewManager();
            guiViewManager.blockedByInteraction();
            boolean cancel = guiViewManager.getCurrentMenu().map(currentWindow -> {
                InteractionResult result = guiViewManager.getLeaveNode(slotIndex)
                        .map(component -> {
                            if (component instanceof Interactable interactable) {
                                return interactable.interact(holder, new ClickInteractionDetailsImpl(clickType, slot, slotIndex, -1));
                            }
                            return InteractionResult.cancel(true);
                        })
                        .orElse(InteractionResult.cancel(true));
                return result.isCancelled();
            }).orElse(false);

            if (cancel) {
                event.cursorTransaction().invalidate();
                for (SlotTransaction transaction : event.transactions()) {
                    transaction.invalidate();
                }
            }
            updateWindow(((PlayerImpl) holder.getPlayer()).spongeRef(), holder.getViewManager(), holder.getViewManager().getWolfyUtils());
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
    public void onClose(InteractContainerEvent.Close event) {
        Inventory inventory = event.inventory();
        findGuiHolder(inventory).ifPresent(holder -> {
            holder.getViewManager().getCurrentMenu().ifPresent(window -> {
                window.close(holder.getViewManager());
            });
        });
    }

    private static void updateWindow(ServerPlayer serverPlayer, GuiViewManager viewManager, WolfyUtils wolfyUtils) {
        Task task = Task.builder().execute(() -> {
            viewManager.unblockedByInteraction();
            viewManager.getRenderContext(serverPlayer.uniqueId()).ifPresent(context -> {
                context.openAndRenderMenuFor(viewManager, serverPlayer.uniqueId());
            });
        }).delay(Ticks.of(1)).plugin(((WolfyUtilsSponge) wolfyUtils).getPluginContainer()).build();
        Sponge.server().scheduler().submit(task);
    }
}
