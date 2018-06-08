package com.songoda.epicspawners.spawners.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.songoda.epicspawners.api.spawner.Spawner;
import com.songoda.epicspawners.api.spawner.SpawnerData;
import com.songoda.epicspawners.api.spawner.SpawnerManager;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class ESpawnerManager implements SpawnerManager {

    // These are the spawner types loaded into memory.
    private final Map<String, SpawnerData> spawners = new LinkedHashMap<>();

    // These are spawners that exist in the game world.
    private final Map<Location, Spawner> spawnersInWorld = new HashMap<>();

    // This is the map that holds the cooldowns for picking up stuffs
    private final List<Spawner> pickingUp = new ArrayList<>();

    @Override
    public SpawnerData getSpawnerData(String name) {
        return spawners.get(name.toLowerCase());
    }

    @Override
    public SpawnerData getSpawnerData(EntityType type) {
        return getSpawnerData(type.name().replaceAll("_", " "));
    }

    @Override
    public void addSpawnerData(String name, SpawnerData spawnerData) {
        spawners.put(name.toLowerCase(), spawnerData);
        spawnerData.reloadSpawnMethods();
    }

    @Override
    public void addSpawnerData(SpawnerData spawnerData) {
        spawners.put(spawnerData.getIdentifyingName().toLowerCase(), spawnerData);
    }

    @Override
    public void removeSpawnerData(String name) {
        spawners.remove(name.toLowerCase());
    }

    @Override
    public Collection<SpawnerData> getAllSpawnerData() {
        return Collections.unmodifiableCollection(spawners.values());
    }

    @Override
    public boolean isSpawner(Location location) {
        return spawnersInWorld.containsKey(location);
    }

    @Override
    public boolean isSpawnerData(String type) {
        return spawners.containsKey(type);
    }

    @Override
    public Spawner getSpawnerFromWorld(Location location) {
        return spawnersInWorld.get(roundLocation(location));
    }

    @Override
    public void addSpawnerToWorld(Location location, Spawner spawner) {
        spawnersInWorld.put(roundLocation(location), spawner);
    }

    @Override
    public Spawner removeSpawnerFromWorld(Location location) {
        return spawnersInWorld.remove(roundLocation(location));
    }

    @Override
    public Collection<Spawner> getSpawners() {
        return Collections.unmodifiableCollection(spawnersInWorld.values());
    }

    public void addCooldown(Spawner spawner) {
        pickingUp.add(spawner);
    }

    public void removeCooldown(Spawner spawner) {
        pickingUp.remove(spawner);
    }

    public boolean hasCooldown(Spawner spawner) {
        return pickingUp.contains(spawner);
    }

    private Location roundLocation(Location location) {
        location = location.clone();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        return location;
    }
}
