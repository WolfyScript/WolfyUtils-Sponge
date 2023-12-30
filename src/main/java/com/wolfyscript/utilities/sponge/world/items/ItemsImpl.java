package com.wolfyscript.utilities.sponge.world.items;

import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.platform.world.items.Items;
import com.wolfyscript.utilities.world.items.ItemStackConfig;

public class ItemsImpl implements Items {

    @Override
    public ItemStackConfig createStackConfig(WolfyUtils wolfyUtils, String s) {
        return new SpongeItemStackConfig(wolfyUtils, s);
    }
}
