package com.wolfyscript.utilities.sponge.gui.components;

import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.components.Button;
import com.wolfyscript.utilities.sponge.eval.context.EvalContextPlayer;
import com.wolfyscript.utilities.sponge.gui.GUIHolder;
import com.wolfyscript.utilities.sponge.gui.RenderContextImpl;
import com.wolfyscript.utilities.sponge.world.items.SpongeItemStackConfig;

import java.util.HashMap;
import java.util.Map;

public class ButtonRenderer implements Renderer {

    private final Button button;

    public ButtonRenderer(Button button) {
        this.button = button;
    }

    @Override
    public int getWidth() {
        return button.width();
    }

    @Override
    public int getHeight() {
        return button.height();
    }

    @Override
    public void render(GuiHolder guiHolder, RenderContext context) {
        if (!(context instanceof RenderContextImpl renderContext)) return;
        renderContext.setNativeStack(renderContext.getCurrentOffset(),
                ((SpongeItemStackConfig) button.icon().getStack()).constructItemStack(
                        new EvalContextPlayer(((GUIHolder) guiHolder).getNativePlayer()),
                        guiHolder.getViewManager().getWolfyUtils().getChat().getMiniMessage(),
                        button.icon().getResolvers()
                )
        );
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
