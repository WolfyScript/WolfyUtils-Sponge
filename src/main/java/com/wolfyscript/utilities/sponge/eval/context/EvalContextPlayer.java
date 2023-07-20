package com.wolfyscript.utilities.sponge.eval.context;

import com.wolfyscript.utilities.eval.context.EvalContext;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

public class EvalContextPlayer extends EvalContext {

    private final ServerPlayer player;

    public EvalContextPlayer(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
