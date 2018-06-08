package com.songoda.epicspawners.spawners.object.option;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.songoda.epicspawners.EpicSpawnersPlugin;
import com.songoda.epicspawners.api.spawner.Spawner;
import com.songoda.epicspawners.api.spawner.SpawnerData;
import com.songoda.epicspawners.api.spawner.SpawnerStack;
import com.songoda.epicspawners.api.spawner.condition.SpawnCondition;
import com.songoda.epicspawners.spawners.condition.SpawnConditionNearbyEntities;
import com.songoda.epicspawners.spawners.object.SpawnOptionType;
import com.songoda.epicspawners.utils.Debugger;
import com.songoda.epicspawners.utils.Methods;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Consumer;

public class SpawnOptionEntity implements SpawnOption {

    private static Method nmsSpawnMethod;

    private final EntityType[] types;

    private final ScriptEngineManager mgr;

    private final ScriptEngine engine;

    private EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();

    public SpawnOptionEntity(EntityType... types) {
        this.types = types;
        this.mgr = new ScriptEngineManager();
        this.engine = mgr.getEngineByName("JavaScript");
    }

    public SpawnOptionEntity(Collection<EntityType> entities) {
        this(entities.toArray(new EntityType[entities.size()]));
    }

    @Override
    public void spawn(SpawnerData data, SpawnerStack stack, Spawner spawner) {
        Location location = spawner.getLocation();
        if (location == null || location.getWorld() == null) return;

        World world = location.getWorld();

        // Instantiate nmsSpawnMethod if not done so already

        if (nmsSpawnMethod == null) {
            try {
                nmsSpawnMethod = world.getClass().getMethod("spawn", Location.class, Class.class, Consumer.class, CreatureSpawnEvent.SpawnReason.class);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
                return;
            }
        }

        String[] randomLowHigh = instance.getConfig().getString("Main.Random Amount Added To Each Spawn").split(":");

        int randomAmt = ThreadLocalRandom.current().nextInt(Integer.valueOf(randomLowHigh[0]), Integer.valueOf(randomLowHigh[1]));

        String equation = instance.getConfig().getString("Main.Equations.Mobs Spawned Per Spawn");
        equation = equation.replace("{RAND}", Integer.toString(randomAmt));
        equation = equation.replace("{MULTI}", Integer.toString(stack.getStackSize()));


        int spawnCount = 0;
        try {
            if (!instance.cache.containsKey(equation)) {
                spawnCount = (int) Math.round(Double.parseDouble(engine.eval(equation).toString()));
                instance.cache.put(equation, spawnCount);
            } else {
                spawnCount = instance.cache.get(equation);
            }
        } catch (ScriptException e) {
            System.out.println("Your spawner equation is broken, fix it.");
        }

        int limit = 0;
        for (SpawnCondition spawnCondition : data.getConditions()) {
            if (spawnCondition instanceof SpawnConditionNearbyEntities)
                limit = ((SpawnConditionNearbyEntities)spawnCondition).getMax();
        }

        int spawnerBoost = spawner.getBoost();
        int amtOfCurrentEntities = Methods.countEntitiesAroundLocation(location);
        if (amtOfCurrentEntities == limit && spawnerBoost == 0) return;
        spawnCount = Math.min(limit - amtOfCurrentEntities, spawnCount) + spawner.getBoost();

        while (spawnCount-- > 0) {
            EntityType type = types[ThreadLocalRandom.current().nextInt(types.length)];
            spawnEntity(location, type, data);
            spawner.setSpawnCount(spawner.getSpawnCount() + 1);
            // TODO: Talk to the author of StackMob to get his ass in gear. lolk (I dropped support, try and add it in later)
        }
    }

    public void spawnEntity(Location location, EntityType type, SpawnerData data) {
        try {

            Location spot = null;
            boolean in = false;

            int amt = 0;
            while (!in && amt <= 25) {
                double testX = ThreadLocalRandom.current().nextDouble(-1, 1);
                double testY = ThreadLocalRandom.current().nextDouble(-1, 2);
                double testZ = ThreadLocalRandom.current().nextDouble(-1, 1);

                double x = location.getX() + testX * 3;
                double y = location.getY() + testY;
                double z = location.getZ() + testZ * 3;

                spot = new Location(location.getWorld(), x, y, z);

                if (type.equals(EntityType.IRON_GOLEM)) {
                    if (spot.getBlock().getRelative(BlockFace.UP).getType() !=  Material.AIR) continue;
                    if (spot.getBlock().getRelative(BlockFace.DOWN).getType() !=  Material.AIR) continue;
                    if (spot.getBlock().getRelative(BlockFace.NORTH).getType() !=  Material.AIR) continue;
                    if (spot.getBlock().getRelative(BlockFace.SOUTH).getType() !=  Material.AIR) continue;
                    if (spot.getBlock().getRelative(BlockFace.EAST).getType() !=  Material.AIR) continue;
                    if (spot.getBlock().getRelative(BlockFace.WEST).getType() !=  Material.AIR) continue;
                }

                if (canSpawn(data, spot))
                    in = true;

                amt++;
            }

            if (in) {
                float x = (float) (0 + (Math.random() * 1));
                float y = (float) (0 + (Math.random() * 2));
                float z = (float) (0 + (Math.random() * 1));

                //ToDo: Make this work for all spawn types
                spot.getWorld().spawnParticle(data.getEntitySpawnParticle().getEffect(), spot, data.getParticleDensity().getEffect(), x, y, z, 0);

                Location loc = spot.clone();
                loc.subtract(0, 1, 0);
                    spot = spot.clone().getBlock().getLocation();

                    double spawnX = ThreadLocalRandom.current().nextDouble(0.4, 0.6);
                    double spawnZ = ThreadLocalRandom.current().nextDouble(0.4, 0.6);

                    spot.add(spawnX, .5, spawnZ);


                spawnFinal(spot, data, type);
            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }

    public boolean canSpawn(SpawnerData data, Location location) {
        boolean canSpawn = true;
        try {
            List<Material> spawnBlocks = Arrays.asList(data.getSpawnBlocks());

            if (!Methods.isAir(location.getBlock().getType()) && (location.getBlock().getType() != Material.WATER && !spawnBlocks.contains(Material.WATER))) {
                canSpawn = false;
            }

            boolean canSpawnUnder = false;
            if (canSpawn) {
                for (Material material : spawnBlocks) {
                    Location loc = location.clone().subtract(0, 1, 0);
                    if (loc.getBlock().getType().toString().equalsIgnoreCase(material.name()) || loc.getBlock().getType() == Material.WATER && spawnBlocks.contains(Material.WATER)) {
                        canSpawnUnder = true;
                    }
                }
                canSpawn = canSpawnUnder;
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return canSpawn;
    }

    private boolean spawnFinal(Location location, SpawnerData data, EntityType type) {
        World world = location.getWorld();

        try {
            Entity entity = (Entity) nmsSpawnMethod.invoke(world, location, type.getEntityClass(), null, CreatureSpawnEvent.SpawnReason.SPAWNER); //ToDo: account for all mobs in the spawner.

            if (data.isSpawnOnFire()) {
                entity.setFireTicks(160);
            }

            entity.setMetadata("ES", new FixedMetadataValue(instance, data.getIdentifyingName()));

            EpicSpawnersPlugin.getInstance().getSpawnManager().addUnnaturalSpawn(entity.getUniqueId());
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @Override
    public SpawnOptionType getType() {
        return SpawnOptionType.ENTITY;
    }

    @Override
    public int hashCode() {
        return 31 * (types != null ? types.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (!(object instanceof SpawnOptionEntity)) return false;

        SpawnOptionEntity other = (SpawnOptionEntity) object;
        return Arrays.equals(types, other.types);
    }

}
