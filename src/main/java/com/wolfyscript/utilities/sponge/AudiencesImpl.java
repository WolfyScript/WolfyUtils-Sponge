package com.wolfyscript.utilities.sponge;

import com.wolfyscript.utilities.platform.Audiences;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.Sponge;

import java.util.UUID;

public class AudiencesImpl implements Audiences {

    @Override
    public Audience player(UUID uuid) {
        return Sponge.server().player(uuid).map(serverPlayer -> (Audience) serverPlayer).orElseGet(Audience::empty);
    }

    @Override
    public Audience all() {
        return org.spongepowered.api.adventure.Audiences.server();
    }

    @Override
    public Audience system() {
        return org.spongepowered.api.adventure.Audiences.system();
    }
}
