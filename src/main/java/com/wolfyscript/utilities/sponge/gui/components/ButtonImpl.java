package com.wolfyscript.utilities.sponge.gui.components;

import com.wolfyscript.utilities.KeyedStaticId;
import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.components.Button;
import com.wolfyscript.utilities.common.gui.components.ButtonIcon;
import com.wolfyscript.utilities.common.gui.impl.AbstractComponentImpl;
import com.wolfyscript.utilities.common.items.ItemStackConfig;
import com.wolfyscript.utilities.eval.context.EvalContext;
import com.wolfyscript.utilities.sponge.SpongeNamespacedKey;
import com.wolfyscript.utilities.sponge.eval.context.EvalContextPlayer;
import com.wolfyscript.utilities.sponge.gui.GUIHolder;
import com.wolfyscript.utilities.sponge.gui.RenderContextImpl;
import com.wolfyscript.utilities.sponge.world.items.SpongeItemStackConfig;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.spongepowered.api.item.inventory.ItemStack;

@KeyedStaticId(key = "button")
public class ButtonImpl extends AbstractComponentImpl implements Button {

    private final InteractionCallback interactionCallback;
    private final ButtonIcon icon;

    public ButtonImpl(WolfyUtils wolfyUtils, String id, Component parent, ButtonIcon icon, InteractionCallback interactionCallback, IntList slots) {
        super(id, wolfyUtils, parent, slots);
        this.icon = icon;
        this.interactionCallback = interactionCallback;
    }

    @Override
    public ButtonIcon icon() {
        return icon;
    }

    @Override
    public InteractionResult interact(GuiHolder guiHolder, InteractionDetails interactionDetails) {
        if (parent() instanceof Interactable interactableParent) {
            InteractionResult result = interactableParent.interact(guiHolder, interactionDetails);
            if (result.isCancelled()) return result;
        }
        return interactionCallback.interact(guiHolder, interactionDetails);
    }

    @Override
    public InteractionCallback interactCallback() {
        return interactionCallback;
    }

    @Override
    public Renderer getRenderer() {
        return new ButtonRenderer(this);
    }

    @Override
    public Renderer construct(GuiViewManager guiViewManager) {
        return new ButtonRenderer(this);
    }

    @Override
    public void update(GuiViewManager viewManager, GuiHolder guiHolder, RenderContext context) {
        if (!(context instanceof RenderContextImpl renderContext)) return;
        renderContext.setNativeStack(renderContext.getCurrentOffset(),
                ((SpongeItemStackConfig) icon().getStack()).constructItemStack(
                        new EvalContextPlayer(((GUIHolder) guiHolder).getNativePlayer()),
                        viewManager.getWolfyUtils().getChat().getMiniMessage(),
                        icon().getResolvers()
                )
        );
    }

    public static class DynamicIcon implements ButtonIcon {

        private final SpongeItemStackConfig config;
        private final TagResolver resolvers;

        DynamicIcon(SpongeItemStackConfig config, TagResolver resolvers) {
            this.config = config;
            this.resolvers = resolvers;
        }

        @Override
        public ItemStackConfig<?> getStack() {
            return config;
        }

        public ItemStack create(MiniMessage miniMessage, EvalContext evalContext, TagResolver... tagResolvers) {
            return config.constructItemStack(evalContext, miniMessage, tagResolvers);
        }

        public TagResolver getResolvers() {
            return resolvers;
        }

    }

}