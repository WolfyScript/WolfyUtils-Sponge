package com.wolfyscript.utilities.sponge.language;

import com.wolfyscript.utilities.common.language.LanguageAPI;
import com.wolfyscript.utilities.language.Language;
import com.wolfyscript.utilities.sponge.WolfyUtilsSponge;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class LangAPISponge extends LanguageAPI {

    public LangAPISponge(WolfyUtilsSponge api) {
        super(api);
    }

    @Override
    public Language loadLangFile(String s) {
        return null;
    }

    @Override
    public void saveLangFile(@NotNull Language language) {

    }

    @Override
    protected File getLangFile(String s) {
        return null;
    }

    @Override
    public String convertLegacyToMiniMessage(String s) {
        return null;
    }

    /**
     * @param list
     * @deprecated
     */
    @Override
    public List<String> replaceKeys(List<String> list) {
        return null;
    }

    /**
     * @param strings
     * @deprecated
     */
    @Override
    public List<String> replaceKeys(String... strings) {
        return null;
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public String replaceColoredKeys(String s) {
        return null;
    }

    /**
     * @param list
     * @deprecated
     */
    @Override
    public List<String> replaceColoredKeys(List<String> list) {
        return null;
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public List<String> replaceKey(String s) {
        return null;
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public List<String> replaceColoredKey(String s) {
        return null;
    }
}
