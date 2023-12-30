package com.wolfyscript.utilities.sponge.adapters;

import com.wolfyscript.utilities.NamespacedKey;
import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.sponge.SpongeNamespacedKey;
import com.wolfyscript.utilities.sponge.world.items.SpongeItemStackConfig;
import com.wolfyscript.utilities.world.items.ItemStackConfig;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

public class ItemStackImpl extends SpongeRefAdapter<ItemStack> implements com.wolfyscript.utilities.platform.adapters.ItemStack {

    private final WolfyUtils wolfyUtils;

    public ItemStackImpl(WolfyUtils wolfyUtils, ItemStack referenced) {
        super(referenced);
        this.wolfyUtils = wolfyUtils;
    }

    @Override
    public NamespacedKey getItem() {
        return ItemTypes.registry().findValueKey(spongeRef().type()).map(SpongeNamespacedKey::of).orElse(null);
    }

    @Override
    public int getAmount() {
        return spongeRef().quantity();
    }

    @Override
    public ItemStackConfig snapshot() {
        return new SpongeItemStackConfig(wolfyUtils, spongeRef());
    }
}
