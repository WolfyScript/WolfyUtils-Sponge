package com.wolfyscript.utilities.sponge;

import com.wolfyscript.utilities.NamespacedKey;
import com.wolfyscript.utilities.Identifiers;
import com.wolfyscript.utilities.WolfyUtils;

public class SpongeIdentifiers implements Identifiers {

    private WolfyUtils wolfyUtils;

    SpongeIdentifiers(WolfyUtils wolfyUtils) {
        this.wolfyUtils = wolfyUtils;
    }

    @Override
    public NamespacedKey getNamespaced(String s) {
        return SpongeNamespacedKey.of(s);
    }

    @Override
    public NamespacedKey getNamespaced(String s, String s1) {
        return new SpongeNamespacedKey(s, s1);
    }

    @Override
    public NamespacedKey getSelfNamespaced(String s) {
        return new SpongeNamespacedKey(wolfyUtils, s);
    }

    @Override
    public NamespacedKey getWolfyUtilsNamespaced(String s) {
        return SpongeNamespacedKey.wolfyutilties(s);
    }
}
