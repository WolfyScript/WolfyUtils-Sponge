package com.wolfyscript.utilities.sponge.gui;

import com.google.common.base.Preconditions;
import com.wolfyscript.utilities.gui.ClickInteractionDetails;
import com.wolfyscript.utilities.gui.ClickType;
import com.wolfyscript.utilities.gui.InteractionResult;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.HashMap;
import java.util.Map;

public class ClickInteractionDetailsImpl implements ClickInteractionDetails {

    private boolean cancelled = false;
    private final org.spongepowered.api.item.inventory.menu.ClickType<?> clickType;
    private final int slotIndex;
    private final int hotbarSlot;
    private final ClickType wuClickType;
    private InteractionResult.ResultType resultType;

    private static final Map<org.spongepowered.api.item.inventory.menu.ClickType<?>, ClickType> clickTypeMapping;

    static {
        clickTypeMapping = new HashMap<>();
        clickTypeMapping.put(ClickTypes.CLICK_LEFT.get(), ClickType.PRIMARY);
        clickTypeMapping.put(ClickTypes.CLICK_RIGHT.get(), ClickType.SECONDARY);
        clickTypeMapping.put(ClickTypes.KEY_THROW_ONE.get(), ClickType.DROP);
        clickTypeMapping.put(ClickTypes.KEY_THROW_ALL.get(), ClickType.CONTROL_DROP);
        clickTypeMapping.put(ClickTypes.SHIFT_CLICK_LEFT.get(), ClickType.SHIFT_PRIMARY);
        clickTypeMapping.put(ClickTypes.SHIFT_CLICK_RIGHT.get(), ClickType.SHIFT_SECONDARY);
        clickTypeMapping.put(ClickTypes.CLICK_MIDDLE.get(), ClickType.MIDDLE);
        clickTypeMapping.put(ClickTypes.KEY_SWAP.get(), ClickType.NUMBER_KEY);
        clickTypeMapping.put(ClickTypes.DOUBLE_CLICK.get(), ClickType.DOUBLE_CLICK);
        clickTypeMapping.put(ClickTypes.CLICK_LEFT_OUTSIDE.get(), ClickType.CONTAINER_BORDER_PRIMARY);
        clickTypeMapping.put(ClickTypes.CLICK_RIGHT_OUTSIDE.get(), ClickType.CONTAINER_BORDER_SECONDARY);
    }

    public ClickInteractionDetailsImpl(org.spongepowered.api.item.inventory.menu.ClickType<?> clickType, Slot slot, int slotIndex, int hotbarSlot) {
        this.clickType = clickType;
        this.slotIndex = slotIndex;
        this.hotbarSlot = hotbarSlot;
        this.wuClickType = clickTypeMapping.get(clickType);
        Preconditions.checkState(wuClickType != null, "Unexpected click type: " + clickType.key(RegistryTypes.CLICK_TYPE));
        this.resultType = InteractionResult.ResultType.DEFAULT;
    }

    @Override
    public boolean isShift() {
        return wuClickType == ClickType.SHIFT_PRIMARY || wuClickType == ClickType.SHIFT_SECONDARY;
    }

    @Override
    public boolean isSecondary() {
        return wuClickType == ClickType.SECONDARY || wuClickType == ClickType.SHIFT_SECONDARY;
    }

    @Override
    public boolean isPrimary() {
        return wuClickType == ClickType.PRIMARY || wuClickType == ClickType.SHIFT_PRIMARY;
    }

    @Override
    public int getSlot() {
        return slotIndex;
    }

    @Override
    public int getRawSlot() {
        return slotIndex;
    }

    @Override
    public int getHotbarButton() {
        return hotbarSlot;
    }

    @Override
    public ClickType getClickType() {
        return wuClickType;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public InteractionResult.ResultType getResultType() {
        return resultType;
    }
}
