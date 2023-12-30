package com.wolfyscript.utilities.sponge.chat;

import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.platform.adapters.Player;
import com.wolfyscript.utilities.chat.Chat;
import com.wolfyscript.utilities.chat.ClickActionCallback;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

public class ChatImpl extends Chat {

    public ChatImpl(WolfyUtils wolfyUtils) {
        super(wolfyUtils);
    }

    @Override
    public void sendMessage(Player player, Component component) {

    }

    @Override
    public void sendMessage(Player player, boolean b, Component component) {

    }

    @Override
    public void sendMessages(Player player, Component... components) {

    }

    @Override
    public void sendMessages(Player player, boolean b, Component... components) {

    }

    @Override
    public Component translated(String s) {
        return null;
    }

    @Override
    public Component translated(String s, TagResolver... tagResolvers) {
        return null;
    }

    @Override
    public Component translated(String s, TagResolver tagResolver) {
        return null;
    }

    @Override
    public Component translated(String s, boolean b) {
        return null;
    }

    @Override
    public Component translated(String s, List<? extends TagResolver> list) {
        return null;
    }

    @Override
    public Component translated(String s, boolean b, List<? extends TagResolver> list) {
        return null;
    }

    @Override
    public ClickEvent executable(Player player, boolean b, ClickActionCallback clickActionCallback) {
        return null;
    }

    @Override
    public String convertOldPlaceholder(String s) {
        return null;
    }
}
