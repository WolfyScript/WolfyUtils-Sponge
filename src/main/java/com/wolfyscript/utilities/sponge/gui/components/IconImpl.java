package com.wolfyscript.utilities.sponge.gui.components;

import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.components.Icon;
import com.wolfyscript.utilities.common.gui.impl.AbstractComponentImpl;
import com.wolfyscript.utilities.sponge.gui.GuiViewManagerImpl;
import com.wolfyscript.utilities.sponge.world.items.SpongeItemStackConfig;
import it.unimi.dsi.fastutil.ints.IntList;
import org.spongepowered.api.item.inventory.ItemStack;

public class IconImpl extends AbstractComponentImpl implements Icon<ItemStack> {

    private final SpongeItemStackConfig itemStackConfig;

    public IconImpl(WolfyUtils wolfyUtils, String id, Component parent, SpongeItemStackConfig itemStackConfig, int[] slots) {
        super(id, wolfyUtils, parent, IntList.of(slots));
        this.itemStackConfig = itemStackConfig;
    }

    @Override
    public Component construct(GuiHolder guiHolder, GuiViewManager guiViewManager) {
        return this;
    }

    @Override
    public void remove(GuiHolder guiHolder, GuiViewManager guiViewManager, RenderContext renderContext) {
        for (int slot : getSlots()) {
            renderContext.setNativeStack(slot, null);
            ((GuiViewManagerImpl) guiHolder.getViewManager()).updateLeaveNodes(null, slot);
        }
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public SpongeItemStackConfig getItemStackConfig() {
        return itemStackConfig;
    }

    @Override
    public void update(GuiViewManager viewManager, GuiHolder guiHolder, RenderContext renderContext) {

    }
}
