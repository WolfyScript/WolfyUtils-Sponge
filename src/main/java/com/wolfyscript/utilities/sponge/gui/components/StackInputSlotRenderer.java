package com.wolfyscript.utilities.sponge.gui.components;

import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.components.StackInputSlot;
import com.wolfyscript.utilities.sponge.gui.RenderContextImpl;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class StackInputSlotRenderer implements Renderer {

    private final StackInputSlot stackInputSlot;

    public StackInputSlotRenderer(StackInputSlot stackInputSlot) {
        this.stackInputSlot = stackInputSlot;
    }

    @Override
    public int getWidth() {
        return stackInputSlot.width();
    }

    @Override
    public int getHeight() {
        return stackInputSlot.height();
    }

    @Override
    public void render(GuiHolder guiHolder, RenderContext context) {
        if (!(context instanceof RenderContextImpl renderContext)) return;
        // TODO
        // ItemStackImpl value = (ItemStackImpl) stackInputSlot.signal().get();
        // renderContext.setNativeStack(renderContext.getCurrentOffset(), value != null ? value.getBukkitRef() : ItemStack.empty());
    }

    @Override
    public Map<String, Signal<?>> getSignals() {
        return new HashMap<>();
    }

    @Override
    public NativeRendererModule<?> getNativeModule() {
        return null;
    }

}
