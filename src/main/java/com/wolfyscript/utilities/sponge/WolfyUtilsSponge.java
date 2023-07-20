package com.wolfyscript.utilities.sponge;

import com.wolfyscript.utilities.common.Identifiers;
import com.wolfyscript.utilities.common.WolfyCore;
import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.chat.Chat;
import com.wolfyscript.utilities.common.gui.GuiAPIManager;
import com.wolfyscript.utilities.common.language.LanguageAPI;
import com.wolfyscript.utilities.sponge.chat.ChatImpl;
import com.wolfyscript.utilities.sponge.gui.GuiAPIManagerImpl;
import com.wolfyscript.utilities.sponge.language.LangAPISponge;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.spongepowered.plugin.PluginContainer;

public class WolfyUtilsSponge extends WolfyUtils {

    private final WolfyCoreSponge core;

    private final PluginContainer pluginContainer;
    private final Chat chat;
    private final LanguageAPI languageAPI;
    private final GuiAPIManager guiAPIManager;
    private final SpongeIdentifiers identifiers;
    private final Path configDir;

    public WolfyUtilsSponge(WolfyCoreSponge core, PluginContainer pluginContainer, Path configDir) {
        this.core = core;
        this.configDir = configDir;
        this.pluginContainer = pluginContainer;
        this.chat = new ChatImpl(this);
        this.languageAPI = new LangAPISponge(this);
        this.guiAPIManager = new GuiAPIManagerImpl(this);
        this.identifiers = new SpongeIdentifiers(this);
    }

    @Override
    public WolfyCoreSponge getCore() {
        return core;
    }

    @Override
    public String getName() {
        return pluginContainer.metadata().name().orElseGet(() -> pluginContainer.metadata().id());
    }

    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    @Override
    public File getDataFolder() {
        return configDir.toFile();
    }

    @Override
    public java.util.logging.Logger getLogger() {
        return null;
    }

    @Override
    public LanguageAPI getLanguageAPI() {
        return languageAPI;
    }

    @Override
    public Chat getChat() {
        return chat;
    }

    @Override
    public Identifiers getIdentifiers() {
        return identifiers;
    }

    @Override
    public GuiAPIManager getGUIManager() {
        return guiAPIManager;
    }

    @Override
    public void exportResource(String s, File file, boolean b) {

    }

    @Override
    public void exportResources(String resourceName, File dir, boolean replace, Pattern filePattern) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        resourceName = resourceName.replace('\\', '/');

        Set<String> paths = getCore().getReflections().getResources(filePattern);
        for (String path : paths) {
            if (!path.startsWith(resourceName)) continue;
            URL url = pluginContainer.instance().getClass().getClassLoader().getResource(path);
            if (url == null) return;

            try {
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                InputStream in = connection.getInputStream();
                if (in == null) throw new IllegalArgumentException("The embedded resource '" + url + "' cannot be found in " + getName());

                File destination = new File(dir, path.replace(resourceName, ""));
                try {
                    if (destination.exists() && !replace) return;
                    destination.getParentFile().mkdirs();
                    destination.createNewFile();
                    OutputStream out = new FileOutputStream(destination);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    in.close();
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Could not save " + destination.getName() + " to " + destination, ex);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
