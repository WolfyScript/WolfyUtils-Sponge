package com.wolfyscript.utilities.sponge.gui.components;

import com.wolfyscript.utilities.KeyedStaticId;
import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.adapters.ItemStack;
import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.components.StackInputSlot;
import com.wolfyscript.utilities.common.gui.impl.AbstractComponentImpl;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Set;
import java.util.function.Consumer;

@KeyedStaticId(key = "stack_input_slot")
public class StackInputSlotImpl extends AbstractComponentImpl implements Interactable, StackInputSlot {

    private final Consumer<ItemStack> onValueChange;
    private final InteractionCallback interactionCallback;
    private final Signal<ItemStack> value;

    public StackInputSlotImpl(String internalID, WolfyUtils wolfyUtils, Component parent, Consumer<ItemStack> onValueChange, InteractionCallback interactionCallback, Signal<ItemStack> value, IntList slots) {
        super(internalID, wolfyUtils, parent, slots);
        this.onValueChange = onValueChange;
        this.interactionCallback = (holder, details) -> {
            // TODO
//            if (details instanceof ClickInteractionDetailsImpl clickInteractionDetails) {
//                InventoryClickEvent event = clickInteractionDetails.getClickEvent();
//                InteractionUtils.applyItemFromInteractionEvent(event.getSlot(), event, Set.of(), itemStack -> {
//                    onValueChange.accept(new ItemStackImpl((WolfyUtilsBukkit) holder.getViewManager().getWolfyUtils(), itemStack));
//                });
//            }
            return interactionCallback.interact(holder, details);
        };
        this.value = value;
    }

    @Override
    public Renderer getRenderer() {
        return new StackInputSlotRenderer(this);
    }

    @Override
    public Renderer construct(GuiViewManager guiViewManager) {
        return new StackInputSlotRenderer(this);
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }

    public Signal<ItemStack> getValue() {
        return value;
    }

    @Override
    public InteractionResult interact(GuiHolder guiHolder, InteractionDetails interactionDetails) {
        return interactionCallback.interact(guiHolder, interactionDetails);
    }

    @Override
    public InteractionCallback interactCallback() {
        return interactionCallback;
    }

    @Override
    public Signal<ItemStack> signal() {
        return value;
    }

    @Override
    public void update(GuiViewManager viewManager, GuiHolder guiHolder, RenderContext renderContext) {

    }
}