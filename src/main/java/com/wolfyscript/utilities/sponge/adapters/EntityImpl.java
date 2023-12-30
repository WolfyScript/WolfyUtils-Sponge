package com.wolfyscript.utilities.sponge.adapters;

import com.wolfyscript.utilities.platform.adapters.Entity;
import com.wolfyscript.utilities.platform.adapters.Location;
import com.wolfyscript.utilities.platform.adapters.Vector3D;
import com.wolfyscript.utilities.platform.adapters.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;

import java.util.Optional;
import java.util.UUID;

public class EntityImpl<T extends org.spongepowered.api.entity.Entity> extends SpongeRefAdapter<T> implements Entity {

    private final UUID uuid;

    public EntityImpl(T spongeEntity) {
        super(spongeEntity);
        this.uuid = spongeEntity.uniqueId();
    }

    protected Optional<org.spongepowered.api.entity.Entity> fetch() {
        return Sponge.game().server().worldManager().defaultWorld().entity(uuid);
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public @NotNull Location getLocation() {
        return null;
    }

    @Override
    public @Nullable Location getLocation(Location location) {
        return null;
    }

    @Override
    public void setVelocity(@NotNull Vector3D vector3D) {

    }

    @Override
    public @NotNull Vector3D getVelocity() {
        return null;
    }

    @Override
    public double getHeight() {
        return 0;
    }

    @Override
    public double getWidth() {
        return 0;
    }

    @Override
    public boolean isOnGround() {
        return false;
    }

    @Override
    public boolean isInWater() {
        return false;
    }

    @Override
    public @NotNull World getWorld() {
        return null;
    }

    @Override
    public void setRotation(float v, float v1) {

    }
}
