package com.wolfyscript.utilities.sponge.adapters;

import com.wolfyscript.utilities.platform.adapters.Player;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.UUID;

public class PlayerImpl extends EntityImpl<ServerPlayer> implements Player {

    public PlayerImpl(ServerPlayer spongeEntity) {
        super(spongeEntity);
    }

    @Override
    public void setDisplayName(Component component) {

    }

    @Override
    public Component getDisplayName() {
        return null;
    }
}
