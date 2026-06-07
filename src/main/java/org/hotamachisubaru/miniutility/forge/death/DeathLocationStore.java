package org.hotamachisubaru.miniutility.forge.death;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeathLocationStore {
    private final Map<UUID, DeathLocation> deathLocations = new ConcurrentHashMap<>();

    public record DeathLocation(Vec3 pos, ResourceKey<Level> dim, float yaw) {}

    public void record(UUID uniqueId, Vec3 position, ResourceKey<Level> dimension, float yaw) {
        deathLocations.put(uniqueId, new DeathLocation(position, dimension, yaw));
    }

    public DeathLocation getDeathLocation(UUID uniqueId) {
        return deathLocations.get(uniqueId);
    }

    public boolean hasDeathLocation(UUID uniqueId) {
        return deathLocations.containsKey(uniqueId);
    }

    public void removeDeathLocation(UUID uniqueId) {
        deathLocations.remove(uniqueId);
    }
}
