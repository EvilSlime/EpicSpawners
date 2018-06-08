package com.songoda.epicspawners.spawners.object;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.arconix.api.methods.formatting.TimeComponent;
import com.songoda.arconix.plugin.Arconix;
import com.songoda.epicspawners.EpicSpawnersPlugin;
import com.songoda.epicspawners.api.CostType;
import com.songoda.epicspawners.api.EpicSpawnersAPI;
import com.songoda.epicspawners.api.events.SpawnerChangeEvent;
import com.songoda.epicspawners.api.spawner.Spawner;
import com.songoda.epicspawners.api.spawner.SpawnerData;
import com.songoda.epicspawners.api.spawner.SpawnerStack;
import com.songoda.epicspawners.api.spawner.condition.SpawnCondition;
import com.songoda.epicspawners.boost.BoostData;
import com.songoda.epicspawners.boost.BoostType;
import com.songoda.epicspawners.player.MenuType;
import com.songoda.epicspawners.utils.Debugger;
import com.songoda.epicspawners.utils.HeadType;
import com.songoda.epicspawners.utils.Methods;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

public class ESpawner implements Spawner {

    private Location location;

    private int spawnCount;

    private String omniState = null;

    private UUID placedBy = null;

    private CreatureSpawner creatureSpawner;

    //Holds the different types of spawners contained by this creatureSpawner.
    private final Deque<SpawnerStack> spawnerStacks = new ArrayDeque<>();

    private final ScriptEngine engine;

    public ESpawner(Location location) {
        this.location = location;
        this.creatureSpawner = (CreatureSpawner) location.getBlock().getState();
        ScriptEngineManager mgr = new ScriptEngineManager();
        this.engine = mgr.getEngineByName("JavaScript");
    }

    //ToDo: Use this for all spawner things (Like items, commands and what not) instead of the old shit
    //ToDO: There is a weird error that is triggered when a spawner is not found in the config.
    private Map<Location, Date> lastSpawns = new HashMap<>();

    @Override
    public void spawn() {
        EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();
        long lastSpawn = 1001;
        if (lastSpawns.containsKey(location)) {
            lastSpawn = (new Date()).getTime() - lastSpawns.get(location).getTime();
        }
        if (lastSpawn >= 1000) {
            lastSpawns.put(location, new Date());
        } else return;

        if (location.getBlock().isBlockPowered() && instance.getConfig().getBoolean("Main.Redstone Power Deactivates Spawners"))
            return;


        if (getFirstStack().getSpawnerData() == null) return;

        double x = (Math.random() * .8);
        double y = (Math.random() * .8);
        double z = (Math.random() * .8);

        Location particleLocation = location.clone();
        particleLocation.add(.5, .5, .5);
        //ToDo: Only currently works for the first spawner Type in the stack. this is not how it should work.
        SpawnerData spawnerData = getFirstStack().getSpawnerData();
        particleLocation.getWorld().spawnParticle(spawnerData.getSpawnerSpawnParticle().getEffect(), particleLocation, spawnerData.getParticleDensity().getEffect(), x, y, z, 0);

        for (SpawnerStack stack : getSpawnerStacks()) {
            ((ESpawnerData)stack.getSpawnerData()).spawn(this, stack);
        }
        Bukkit.getScheduler().runTaskLater(instance, this::updateDelay, 10);
    }

    @Override
    public void addSpawnerStack(SpawnerStack spawnerStack) {
        this.spawnerStacks.addFirst(spawnerStack);
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public int getX() {
        return location.getBlockX();
    }

    @Override
    public int getY() {
        return location.getBlockY();
    }

    @Override
    public int getZ() {
        return location.getBlockZ();
    }

    @Override
    public World getWorld() {
        return location.getWorld();
    }

    @Override
    public CreatureSpawner getCreatureSpawner() {
        return creatureSpawner;
    }

    @Override
    public SpawnerStack getFirstStack() {
        return spawnerStacks.getFirst();
    }

    @Override
    public int getSpawnerDataCount() {
        int multi = 0;
        for (SpawnerStack stack : spawnerStacks) {
            multi += stack.getStackSize();
        }
        return multi;
    }

    public void overview(Player p, int infoPage) {
        try {
            EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();
            if (!p.hasPermission("epicspawners.overview")) return;
            Inventory i = Bukkit.createInventory(null, 27, TextComponent.formatTitle(Methods.compileName(getIdentifyingName(), getSpawnerDataCount(), false)));


            SpawnerData spawnerData = getFirstStack().getSpawnerData();

            int showAmt = getSpawnerDataCount();
            if (showAmt > 64)
                showAmt = 1;
            else if (showAmt == 0)
                showAmt = 1;

            ItemStack item = new ItemStack(Material.PLAYER_HEAD, showAmt);
            if (spawnerStacks.size() != 1) {
                item = HeadType.addTexture(item, instance.getSpawnerManager().getSpawnerData("omni"));
            } else {
                try {
                    item = HeadType.addTexture(item, spawnerData);
                } catch (Exception e) {
                    item = new ItemStack(Material.MOB_SPAWNER, showAmt);
                }
            }

            if (spawnerStacks.size() == 1 && spawnerStacks.getFirst().getSpawnerData().getDisplayItem() != null) {
                item.setType(spawnerStacks.getFirst().getSpawnerData().getDisplayItem());
            }

            ItemMeta itemmeta = item.getItemMeta();
            itemmeta.setDisplayName(instance.getLocale().getMessage("interface.spawner.statstitle"));
            ArrayList<String> lore = new ArrayList<>();

            if (spawnerStacks.size() != 1) {
                StringBuilder only = new StringBuilder("&6" + Methods.compileName(spawnerStacks.getFirst().getSpawnerData().getIdentifyingName(), spawnerStacks.getFirst().getStackSize(), false));

                int num = 1;
                for (SpawnerStack stack : spawnerStacks) {
                    if (num != 1)
                        only.append("&8, &6").append(Methods.compileName(stack.getSpawnerData().getIdentifyingName(), stack.getStackSize(), false));
                    num++;
                }

                lore.add(TextComponent.formatText(only.toString()));
            }

            List<Material> blocks = getFirstStack().getSpawnerData().getSpawnBlocksList();

            StringBuilder only = new StringBuilder(blocks.get(0).name());

            int num = 1;
            for (Material block : blocks) {
                if (num != 1)
                    only.append("&8, &6").append(Methods.getTypeFromString(block.name()));
                num++;
            }

            lore.add(instance.getLocale().getMessage("interface.spawner.onlyspawnson", only.toString()));

            lore.add(instance.getLocale().getMessage("interface.spawner.stats", spawnCount));
            if (p.hasPermission("epicspawners.convert") && spawnerStacks.size() == 1) {
                lore.add("");
                lore.add(instance.getLocale().getMessage("interface.spawner.convert"));
            }
            if (p.hasPermission("epicspawners.canboost")) {
                if (getBoost() == 0) {
                    if (!p.hasPermission("epicspawners.convert") || spawnerStacks.size() != 1) {
                        lore.add("");
                    }
                    lore.add(instance.getLocale().getMessage("interface.spawner.boost"));
                }
            }
            if (getBoost() != 0) {

                // ToDo: Make it display all boosts.
                String[] parts = instance.getLocale().getMessage("interface.spawner.boostedstats", Integer.toString(getBoost()), spawnerData.getIdentifyingName(), TimeComponent.makeReadable(getBoostEnd().toEpochMilli() - System.currentTimeMillis())).split("\\|");
                lore.add("");
                for (String line : parts)
                    lore.add(TextComponent.formatText(line));
            }
            itemmeta.setLore(lore);
            item.setItemMeta(itemmeta);

            int xpCost = getUpgradeCost(CostType.EXPERIENCE);

            int ecoCost = getUpgradeCost(CostType.ECONOMY);

            boolean maxed = false;
            if (getSpawnerDataCount() == EpicSpawnersPlugin.getInstance().getConfig().getInt("Main.Spawner Max Upgrade"))
                maxed = true;

            ItemStack itemXP = new ItemStack(Material.valueOf(instance.getConfig().getString("Interfaces.XP Icon")), 1);
            ItemMeta itemmetaXP = itemXP.getItemMeta();
            itemmetaXP.setDisplayName(instance.getLocale().getMessage("interface.spawner.upgradewithxp"));
            ArrayList<String> loreXP = new ArrayList<>();
            if (!maxed)
                loreXP.add(instance.getLocale().getMessage("interface.spawner.upgradewithxplore", Integer.toString(xpCost)));
            else
                loreXP.add(instance.getLocale().getMessage("event.upgrade.maxed"));
            itemmetaXP.setLore(loreXP);
            itemXP.setItemMeta(itemmetaXP);

            ItemStack itemECO = new ItemStack(Material.valueOf(instance.getConfig().getString("Interfaces.Economy Icon")), 1);
            ItemMeta itemmetaECO = itemECO.getItemMeta();
            itemmetaECO.setDisplayName(instance.getLocale().getMessage("interface.spawner.upgradewitheconomy"));
            ArrayList<String> loreECO = new ArrayList<>();
            if (!maxed)
                loreECO.add(instance.getLocale().getMessage("interface.spawner.upgradewitheconomylore", TextComponent.formatEconomy(ecoCost)));
            else
                loreECO.add(instance.getLocale().getMessage("event.upgrade.maxed"));
            itemmetaECO.setLore(loreECO);
            itemECO.setItemMeta(itemmetaECO);

            int nu = 0;
            while (nu != 27) {
                i.setItem(nu, Methods.getGlass());
                nu++;
            }
            i.setItem(13, item);

            i.setItem(0, Methods.getBackgroundGlass(true));
            i.setItem(1, Methods.getBackgroundGlass(true));
            i.setItem(2, Methods.getBackgroundGlass(false));
            i.setItem(6, Methods.getBackgroundGlass(false));
            i.setItem(7, Methods.getBackgroundGlass(true));
            i.setItem(8, Methods.getBackgroundGlass(true));
            i.setItem(9, Methods.getBackgroundGlass(true));
            i.setItem(10, Methods.getBackgroundGlass(false));
            i.setItem(16, Methods.getBackgroundGlass(false));
            i.setItem(17, Methods.getBackgroundGlass(true));
            i.setItem(18, Methods.getBackgroundGlass(true));
            i.setItem(19, Methods.getBackgroundGlass(true));
            i.setItem(20, Methods.getBackgroundGlass(false));
            i.setItem(24, Methods.getBackgroundGlass(false));
            i.setItem(25, Methods.getBackgroundGlass(true));
            i.setItem(26, Methods.getBackgroundGlass(true));

            if (EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Display Help Button In Spawner Overview")) {
                ItemStack itemO = new ItemStack(Material.PAPER, 1);
                ItemMeta itemmetaO = itemO.getItemMeta();
                itemmetaO.setDisplayName(instance.getLocale().getMessage("interface.spawner.howtotitle"));
                ArrayList<String> loreO = new ArrayList<>();
                String text = EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.spawner.howtoinfo");

                int start = (14 * infoPage) - 14;
                int li = 1; // 12
                int added = 0;
                boolean max = false;

                String[] parts = text.split("\\|");
                for (String line : parts) {
                    line = compileHow(p, line);
                    if (line.equals(".") || line.equals("")) {

                    } else {
                        Pattern regex = Pattern.compile("(.{1,28}(?:\\s|$))|(.{0,28})", Pattern.DOTALL);
                        Matcher m = regex.matcher(line);
                        while (m.find()) {
                            if (li > start) {
                                if (li < start + 15) {
                                    loreO.add(TextComponent.formatText("&7" + m.group()));
                                    added++;
                                } else {
                                    max = true;
                                }
                            }
                            li++;
                        }
                    }
                }
                if (added == 0) {
                    overview(p, 1);
                    EpicSpawnersPlugin.getInstance().getPlayerActionManager().getPlayerAction(p).setInfoPage(1);
                    return;
                }
                if (max) {
                    loreO.add(instance.getLocale().getMessage("interface.spawner.howtonext"));
                } else {
                    loreO.add(instance.getLocale().getMessage("interface.spawner.howtoback"));
                }
                itemmetaO.setLore(loreO);
                itemO.setItemMeta(itemmetaO);
                i.setItem(8, itemO);
            }
            if (spawnerStacks.size() == 1) {
                if (getFirstStack().getSpawnerData().isUpgradeable()) {
                    if (EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Upgrade With XP"))
                        i.setItem(11, itemXP);
                    if (EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Upgrade With Economy"))
                        i.setItem(15, itemECO);
                }
            }
            p.openInventory(i);
            EpicSpawnersPlugin.getInstance().getPlayerActionManager().getPlayerAction(p).setInMenu(MenuType.OVERVIEW);
            EpicSpawnersPlugin.getInstance().getPlayerActionManager().getPlayerAction(p).setLastSpawner(this);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void playerBoost(Player p) {
        try {
            if (!p.hasPermission("epicspawners.canboost")) return;

            if (EpicSpawnersPlugin.getInstance().boostAmt.containsKey(p)) {
                if (EpicSpawnersPlugin.getInstance().boostAmt.get(p) > EpicSpawnersPlugin.getInstance().getConfig().getInt("Spawner Boosting.Max Multiplier For A Spawner Boost")) {
                    EpicSpawnersPlugin.getInstance().boostAmt.put(p, EpicSpawnersPlugin.getInstance().getConfig().getInt("Spawner Boosting.Max Multiplier For A Spawner Boost"));
                    return;
                } else if (EpicSpawnersPlugin.getInstance().boostAmt.get(p) < 1) {
                    EpicSpawnersPlugin.getInstance().boostAmt.put(p, 1);
                }
            }

            int amt = 1;

            if (EpicSpawnersPlugin.getInstance().boostAmt.containsKey(p))
                amt = EpicSpawnersPlugin.getInstance().boostAmt.get(p);
            else
                EpicSpawnersPlugin.getInstance().boostAmt.put(p, amt);

            Inventory i = Bukkit.createInventory(null, 27, EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.boost.title", Integer.toString(amt), Methods.compileName(getIdentifyingName(), getSpawnerDataCount(), false)));

            int num = 0;
            while (num != 27) {
                i.setItem(num, Methods.getGlass());
                num++;
            }

            ItemStack coal = new ItemStack(Material.COAL);
            ItemMeta coalMeta = coal.getItemMeta();
            coalMeta.setDisplayName(EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.boost.boostfor", "5"));
            ArrayList<String> coalLore = new ArrayList<>();
            coalLore.add(TextComponent.formatText("&7Costs &6&l" + Methods.getBoostCost(5, amt) + "."));
            coalMeta.setLore(coalLore);
            coal.setItemMeta(coalMeta);

            ItemStack iron = new ItemStack(Material.IRON_INGOT);
            ItemMeta ironMeta = iron.getItemMeta();
            ironMeta.setDisplayName(EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.boost.boostfor", "15"));
            ArrayList<String> ironLore = new ArrayList<>();
            ironLore.add(TextComponent.formatText("&7Costs &6&l" + Methods.getBoostCost(15, amt) + "."));
            ironMeta.setLore(ironLore);
            iron.setItemMeta(ironMeta);

            ItemStack diamond = new ItemStack(Material.DIAMOND);
            ItemMeta diamondMeta = diamond.getItemMeta();
            diamondMeta.setDisplayName(EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.boost.boostfor", "30"));
            ArrayList<String> diamondLore = new ArrayList<>();
            diamondLore.add(TextComponent.formatText("&7Costs &6&l" + Methods.getBoostCost(30, amt) + "."));
            diamondMeta.setLore(diamondLore);
            diamond.setItemMeta(diamondMeta);

            ItemStack emerald = new ItemStack(Material.EMERALD);
            ItemMeta emeraldMeta = emerald.getItemMeta();
            emeraldMeta.setDisplayName(EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.boost.boostfor", "60"));
            ArrayList<String> emeraldLore = new ArrayList<>();
            emeraldLore.add(TextComponent.formatText("&7Costs &6&l" + Methods.getBoostCost(60, amt) + "."));
            emeraldMeta.setLore(emeraldLore);
            emerald.setItemMeta(emeraldMeta);

            i.setItem(10, coal);
            i.setItem(12, iron);
            i.setItem(14, diamond);
            i.setItem(16, emerald);

            i.setItem(0, Methods.getBackgroundGlass(true));
            i.setItem(1, Methods.getBackgroundGlass(true));
            i.setItem(2, Methods.getBackgroundGlass(false));
            i.setItem(6, Methods.getBackgroundGlass(false));
            i.setItem(7, Methods.getBackgroundGlass(true));
            i.setItem(8, Methods.getBackgroundGlass(true));
            i.setItem(9, Methods.getBackgroundGlass(true));
            i.setItem(17, Methods.getBackgroundGlass(true));
            i.setItem(18, Methods.getBackgroundGlass(true));
            i.setItem(19, Methods.getBackgroundGlass(true));
            i.setItem(20, Methods.getBackgroundGlass(false));
            i.setItem(24, Methods.getBackgroundGlass(false));
            i.setItem(25, Methods.getBackgroundGlass(true));
            i.setItem(26, Methods.getBackgroundGlass(true));

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head = HeadType.ARROW_RIGHT.addTexture(head);
            SkullMeta headMeta = (SkullMeta) head.getItemMeta();
            headMeta.setDisplayName(TextComponent.formatText("&6&l+1"));
            head.setItemMeta(headMeta);

            ItemStack head2 = new ItemStack(Material.PLAYER_HEAD);
            head2 = HeadType.ARROW_LEFT.addTexture(head2);
            SkullMeta head2Meta = (SkullMeta) head2.getItemMeta();
            head2Meta.setDisplayName(TextComponent.formatText("&6&l-1"));
            head2.setItemMeta(head2Meta);

            if (amt != 1) {
                i.setItem(0, head2);
            }
            if (amt < EpicSpawnersPlugin.getInstance().getConfig().getInt("Spawner Boosting.Max Multiplier For A Spawner Boost")) {
                i.setItem(8, head);
            }

            p.openInventory(i);
            EpicSpawnersPlugin.getInstance().getPlayerActionManager().getPlayerAction(p).setInMenu(MenuType.PLAYERBOOST);
            EpicSpawnersPlugin.getInstance().getPlayerActionManager().getPlayerAction(p).setLastSpawner(this);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    @Override
    public boolean checkConditions() {
        for (SpawnerStack stack : spawnerStacks) {
            for (SpawnCondition spawnCondition : stack.getSpawnerData().getConditions()) {
                if (!spawnCondition.isMet(this)) return false;
            }
        }
        return true;
    }

    public void purchaseBoost(Player p, int time) {
        try {
            EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();

            int amt = instance.boostAmt.get(p);
            boolean yes = false;

            String un = EpicSpawnersPlugin.getInstance().getConfig().getString("Spawner Boosting.Item Charged For A Boost");

            String[] parts = un.split(":");

            String type = parts[0];
            String multi = parts[1];
            int cost = Methods.boostCost(multi, time, amt);
            if (!type.equals("ECO") && !type.equals("XP")) {
                ItemStack stack = new ItemStack(Material.valueOf(type));
                int invAmt = Arconix.pl().getApi().getGUI().getAmount(p.getInventory(), stack);
                if (invAmt >= cost) {
                    stack.setAmount(cost);
                    Arconix.pl().getApi().getGUI().removeFromInventory(p.getInventory(), stack);
                    yes = true;
                } else {
                    p.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford"));
                }
            } else if (type.equals("ECO")) {
                if (EpicSpawnersPlugin.getInstance().getServer().getPluginManager().getPlugin("Vault") != null) {
                    RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = EpicSpawnersPlugin.getInstance().getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    net.milkbowl.vault.economy.Economy econ = rsp.getProvider();
                    if (econ.has(p, cost)) {
                        econ.withdrawPlayer(p, cost);
                        yes = true;
                    } else {
                        p.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford"));
                    }
                } else {
                    p.sendMessage("Vault is not installed.");
                }
            } else if (type.equals("XP")) {
                if (p.getLevel() >= cost || p.getGameMode() == GameMode.CREATIVE) {
                    if (p.getGameMode() != GameMode.CREATIVE) {
                        p.setLevel(p.getLevel() - cost);
                    }
                    yes = true;
                } else {
                    p.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford"));
                }
            }
            if (yes) {
                Calendar c = Calendar.getInstance();
                Date currentDate = new Date();
                c.setTime(currentDate);
                c.add(Calendar.MINUTE, time);


                BoostData boostData = new BoostData(BoostType.LOCATION, amt, c.getTime().getTime(), location);
                instance.getBoostManager().addBoostToSpawner(boostData);
                p.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.boost.applied"));
            }
            p.closeInventory();
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void convertOverview(Player p, int page) {
        try {
            EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();
            instance.page.put(p, page);

            List<SpawnerData> entities = new ArrayList<>();

            int num = 0;
            int show = 0;
            int start = (page - 1) * 32;
            for (SpawnerData spawnerData : instance.getSpawnerManager().getAllSpawnerData()) {
                if (spawnerData.getIdentifyingName().equalsIgnoreCase("omni")
                        || !spawnerData.isConvertible()
                        || !p.hasPermission("epicspawners.convert." + spawnerData.getIdentifyingName())) continue;
                if (num >= start) {
                    if (show <= 32) {
                        entities.add(spawnerData);
                        show++;
                    }
                }
                num++;
            }

            int amt = entities.size();
            String title = EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.convert.title");
            Inventory i = Bukkit.createInventory(null, 54, TextComponent.formatTitle(title));
            int max2 = 54;
            if (amt <= 7) {
                i = Bukkit.createInventory(null, 27, TextComponent.formatTitle(title));
                max2 = 27;
            } else if (amt <= 15) {
                i = Bukkit.createInventory(null, 36, TextComponent.formatTitle(title));
                max2 = 36;
            } else if (amt <= 25) {
                i = Bukkit.createInventory(null, 45, TextComponent.formatTitle(title));
                max2 = 45;
            }

            final int max22 = max2;
            int place = 10;
            for (SpawnerData spawnerData : entities) {
                if (place == 17)
                    place++;
                if (place == (max22 - 18))
                    place++;
                ItemStack it = new ItemStack(Material.PLAYER_HEAD);
                it = HeadType.addTexture(it, spawnerData);

                if (spawnerData.getDisplayItem() != null) {
                    Material mat = spawnerData.getDisplayItem();
                    if (!mat.equals(Material.AIR))
                        it = new ItemStack(mat, 1);
                }

                ItemMeta itemmeta = it.getItemMeta();
                String name = Methods.compileName(spawnerData.getIdentifyingName(), 1, true);
                ArrayList<String> lore = new ArrayList<>();
                double price = spawnerData.getConvertPrice() * getSpawnerDataCount();

                lore.add(EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.shop.buyprice", TextComponent.formatEconomy(price)));
                String loreString = EpicSpawnersPlugin.getInstance().getLocale().getMessage("interface.convert.lore", Methods.getTypeFromString(spawnerData.getIdentifyingName()));
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    loreString = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, loreString.replace(" ", "_")).replace("_", " ");
                }
                lore.add(loreString);
                itemmeta.setLore(lore);
                itemmeta.setDisplayName(name);
                it.setItemMeta(itemmeta);
                i.setItem(place, it);
                place++;
            }

            int max = (int) Math.ceil((double) num / (double) 36);
            num = 0;
            while (num != 9) {
                i.setItem(num, Methods.getGlass());
                num++;
            }
            int num2 = max2 - 9;
            while (num2 != max2) {
                i.setItem(num2, Methods.getGlass());
                num2++;
            }

            ItemStack exit = new ItemStack(Material.valueOf(EpicSpawnersPlugin.getInstance().getConfig().getString("Interfaces.Exit Icon")), 1);
            ItemMeta exitmeta = exit.getItemMeta();
            exitmeta.setDisplayName(EpicSpawnersPlugin.getInstance().getLocale().getMessage("general.nametag.exit"));
            exit.setItemMeta(exitmeta);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head = HeadType.ARROW_RIGHT.addTexture(head);
            SkullMeta headMeta = (SkullMeta) head.getItemMeta();
            headMeta.setDisplayName(EpicSpawnersPlugin.getInstance().getLocale().getMessage("general.nametag.next"));
            head.setItemMeta(headMeta);

            ItemStack head2 = new ItemStack(Material.PLAYER_HEAD);
            head2 = HeadType.ARROW_LEFT.addTexture(head2);
            SkullMeta head2Meta = (SkullMeta) head2.getItemMeta();
            head2Meta.setDisplayName(EpicSpawnersPlugin.getInstance().getLocale().getMessage("general.nametag.back"));
            head2.setItemMeta(head2Meta);

            i.setItem(8, exit);

            i.setItem(0, Methods.getBackgroundGlass(true));
            i.setItem(1, Methods.getBackgroundGlass(true));
            i.setItem(9, Methods.getBackgroundGlass(true));

            i.setItem(7, Methods.getBackgroundGlass(true));
            i.setItem(17, Methods.getBackgroundGlass(true));

            i.setItem(max22 - 18, Methods.getBackgroundGlass(true));
            i.setItem(max22 - 9, Methods.getBackgroundGlass(true));
            i.setItem(max22 - 8, Methods.getBackgroundGlass(true));

            i.setItem(max22 - 10, Methods.getBackgroundGlass(true));
            i.setItem(max22 - 2, Methods.getBackgroundGlass(true));
            i.setItem(max22 - 1, Methods.getBackgroundGlass(true));

            i.setItem(2, Methods.getBackgroundGlass(false));
            i.setItem(6, Methods.getBackgroundGlass(false));
            i.setItem(max22 - 7, Methods.getBackgroundGlass(false));
            i.setItem(max22 - 3, Methods.getBackgroundGlass(false));

            if (page != 1) {
                i.setItem(max22 - 8, head2);
            }
            if (page != max) {
                i.setItem(max22 - 2, head);
            }

            p.openInventory(i);
            EpicSpawnersPlugin.getInstance().change.add(p);
            EpicSpawnersPlugin.getInstance().getPlayerActionManager().getPlayerAction(p).setInMenu(MenuType.CONVERT);
            EpicSpawnersPlugin.getInstance().getPlayerActionManager().getPlayerAction(p).setLastSpawner(this);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }


    public void convert(SpawnerData type, Player p) {
        try {
            EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();
            if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                p.sendMessage("Vault is not installed.");
                return;
            }

            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = EpicSpawnersPlugin.getInstance().getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            net.milkbowl.vault.economy.Economy econ = rsp.getProvider();

            double price = type.getConvertPrice() * getSpawnerDataCount();

            if (!(econ.has(p, price) || p.isOp())) {
                p.sendMessage(EpicSpawnersPlugin.getInstance().getPrefix() + EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford"));
                return;
            }
            SpawnerChangeEvent event = new SpawnerChangeEvent(p, this, getFirstStack().getSpawnerData(), type);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }

            this.spawnerStacks.getFirst().setSpawnerData(type);
            try {
                this.creatureSpawner.setSpawnedType(EntityType.valueOf(type.getIdentifyingName().toUpperCase()));
            } catch (Exception e) {
                this.creatureSpawner.setSpawnedType(EntityType.DROPPED_ITEM);
            }
            this.creatureSpawner.update();

            p.sendMessage(EpicSpawnersPlugin.getInstance().getPrefix() + EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.convert.success"));

            instance.getHologramHandler().updateHologram(this);
            p.closeInventory();
            if (!p.isOp()) {
                econ.withdrawPlayer(p, price);
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public int getUpgradeCost(CostType type) {
        try {
            int cost = 0;
            if (type == CostType.ECONOMY) {
                if (getFirstStack().getSpawnerData().getUpgradeCostEconomy() != 0)
                    cost = (int) getFirstStack().getSpawnerData().getUpgradeCostEconomy();
                else
                    cost = EpicSpawnersPlugin.getInstance().getConfig().getInt("Main.Cost To Upgrade With Economy");
                if (EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Use Custom Equations for Upgrade Costs")) {
                    String math = EpicSpawnersPlugin.getInstance().getConfig().getString("Main.Equations.Calculate Economy Upgrade Cost").replace("{ECOCost}", Integer.toString(cost)).replace("{Level}", Integer.toString(getSpawnerDataCount()));
                    cost = (int) Math.round(Double.parseDouble(engine.eval(math).toString()));
                }
            } else if (type == CostType.EXPERIENCE) {
                if (getFirstStack().getSpawnerData().getUpgradeCostExperience() != 0) {
                    cost = getFirstStack().getSpawnerData().getUpgradeCostExperience();
                } else
                    cost = EpicSpawnersPlugin.getInstance().getConfig().getInt("Main.Cost To Upgrade With XP");
                if (EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Use Custom Equations for Upgrade Costs")) {
                    String math = EpicSpawnersPlugin.getInstance().getConfig().getString("Main.Equations.Calculate XP Upgrade Cost").replace("{XPCost}", Integer.toString(cost)).replace("{Level}", Integer.toString(getSpawnerDataCount()));
                    cost = (int) Math.round(Double.parseDouble(engine.eval(math).toString()));
                }
            }
            return cost;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return Integer.MAX_VALUE;
    }

    public String compileHow(Player p, String text) {
        try {
            Matcher m = Pattern.compile("\\{(.*?)}").matcher(text);
            while (m.find()) {
                Matcher mi = Pattern.compile("\\[(.*?)]").matcher(text);
                int nu = 0;
                int a = 0;
                String type = "";
                while (mi.find()) {
                    if (nu == 0) {
                        type = mi.group().replace("[", "").replace("]", "");
                        text = text.replace(mi.group(), "");
                    } else {
                        switch (type) {
                            case "LEVELUP":
                                if (nu == 1) {
                                    if (!p.hasPermission("epicspawners.combine." + getIdentifyingName()) && !p.hasPermission("epicspawners.combine." + getIdentifyingName())) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                        a++;
                                    }
                                } else if (nu == 2) {
                                    if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Upgrade With XP")) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                        a++;
                                    }
                                } else if (nu == 3) {
                                    if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Upgrade With Economy")) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                        a++;
                                    }
                                }
                                break;
                            case "WATER":
                                if (nu == 1) {
                                    if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("settings.Spawners-repel-liquid")) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                    }
                                }
                                break;
                            case "INVSTACK":
                                if (nu == 1) {
                                    if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Allow Stacking Spawners In Survival Inventories")) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                    }
                                }
                                break;
                            case "REDSTONE":
                                if (nu == 1) {
                                    if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Redstone Power Deactivates Spawners")) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                    }
                                }
                                break;
                            case "OMNI":
                                if (nu == 1) {
                                    if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.OmniSpawners Enabled")) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                    }
                                }
                                break;
                            case "DROP":
                                if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Spawner Drops.Allow Killing Mobs To Drop Spawners") || !p.hasPermission("epicspawners.Killcounter")) {
                                    text = "";
                                } else {
                                    text = text.replace("<TYPE>", getIdentifyingName().toLowerCase());
                                    if (EpicSpawnersPlugin.getInstance().spawnerFile.getConfig().getInt("Entities." + Methods.getTypeFromString(getIdentifyingName()) + ".CustomGoal") != 0)
                                        text = text.replace("<AMT>", Integer.toString(EpicSpawnersPlugin.getInstance().spawnerFile.getConfig().getInt("Entities." + Methods.getTypeFromString(getIdentifyingName()) + ".CustomGoal")));
                                    else
                                        text = text.replace("<AMT>", Integer.toString(EpicSpawnersPlugin.getInstance().getConfig().getInt("Spawner Drops.Kills Needed for Drop")));
                                }
                                if (nu == 1) {
                                    if (EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Spawner Drops.Count Unnatural Kills Towards Spawner Drop")) {
                                        text = text.replace(mi.group(), "");
                                    } else {
                                        text = text.replace(mi.group(), a(a, mi.group()));
                                    }
                                }
                                break;
                        }
                    }
                    nu++;
                }

            }
            text = text.replace("[", "").replace("]", "").replace("{", "").replace("}", "");
            return text;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    private String a(int a, String text) {
        try {
            if (a != 0) {
                text = ", " + text;
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return text;
    }

    @Override
    public boolean unstack(Player player) {
        EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();
        SpawnerStack stack = spawnerStacks.getFirst();

        int stackSize = 1;

        if (player.isSneaking() && EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Sneak To Receive A Stacked Spawner")
                || instance.getConfig().getBoolean("Spawner Drops.Only Drop Stacked Spawners")) {
            stackSize = stack.getStackSize();
        }

        if (instance.getConfig().getBoolean("Main.Sounds Enabled")) {
        	player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.6F, 15.0F);
        }
        ItemStack item = stack.getSpawnerData().toItemStack(1, stackSize);


        if (EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Add Spawners To Inventory On Drop") && player.getInventory().firstEmpty() == -1)
            player.getInventory().addItem(item);
        else if (!instance.getConfig().getBoolean("Main.Only Drop Placed Spawner") || placedBy != null) { //ToDo: Clean this up.

        	ItemStack handItem = player.getInventory().getItemInMainHand();
            if (instance.getConfig().getBoolean("Spawner Drops.Drop On SilkTouch")
                    && handItem != null && handItem.hasItemMeta()
                    && handItem.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)
                    && player.hasPermission("epicspawners.silkdrop." + stack.getSpawnerData().getIdentifyingName())
                    || player.hasPermission("epicspawners.no-silk-drop")) {

                int ch = Integer.parseInt(instance.getConfig().getString((placedBy != null
                        ? "Spawner Drops.Chance On Placed Silktouch" : "Spawner Drops.Chance On Natural Silktouch")).replace("%", ""));

                double rand = Math.random() * 100;

                if (rand - ch < 0 || ch == 100) {
                    if (instance.getConfig().getBoolean("Main.Add Spawners To Inventory On Drop") && player.getInventory().firstEmpty() != -1)
                        player.getInventory().addItem(item);
                    else
                        location.getWorld().dropItemNaturally(location.clone().add(.5, 0, .5), item);
                }
            }
        }

        if (stack.getStackSize() != stackSize) {
            stack.setStackSize(stack.getStackSize() - 1);
            return true;
        }

        spawnerStacks.removeFirst();

        if (spawnerStacks.size() != 0) return true;

        location.getBlock().setType(Material.AIR);
        EpicSpawnersPlugin.getInstance().getSpawnerManager().removeSpawnerFromWorld(location);
        instance.getHologramHandler().despawn(location.getBlock());
        return true;
    }

    @Override
    public boolean preStack(Player player, ItemStack itemStack) {
        return stack(player, EpicSpawnersAPI.getSpawnerDataFromItem(itemStack), EpicSpawnersAPI.getStackSizeFromItem(itemStack));
    }

    @Override
    public boolean stack(Player player, SpawnerData data, int amount) {
        EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();

        int max = instance.getConfig().getInt("Main.Spawner Max Upgrade");
        int currentStackSize = getSpawnerDataCount();

        if (getSpawnerDataCount() == max) {
            player.sendMessage(instance.getLocale().getMessage("event.upgrade.maxed", max));
            return false;
        }

        if ((getSpawnerDataCount() + amount) > max) {
            ItemStack item = data.toItemStack( 1, (getSpawnerDataCount() + amount) - max);
            if (player.getInventory().firstEmpty() == -1)
                location.getWorld().dropItemNaturally(location.clone().add(.5, 0, .5), item);
            else
                player.getInventory().addItem(item);

            amount = max - currentStackSize;
        }


        for (SpawnerStack stack : spawnerStacks) {
            if (!stack.getSpawnerData().equals(data)) continue;
            stack.setStackSize(stack.getStackSize() + amount);
            upgradeFinal(player, currentStackSize);

            if (player.getGameMode() != GameMode.CREATIVE)
                Methods.takeItem(player, amount);

            return true;
        }

        if (!instance.getConfig().getBoolean("Main.OmniSpawners Enabled") || !player.hasPermission("epicspawners.omni")) return false;

        ESpawnerStack stack = new ESpawnerStack(data, amount);
        spawnerStacks.push(stack);

        if (player.getGameMode() != GameMode.CREATIVE)
            Methods.takeItem(player, amount);

        return true;
    }

    private void upgradeFinal(Player player, int oldStackSize) {
        try {
            int currentStackSize = getSpawnerDataCount();

            if (getSpawnerDataCount() != EpicSpawnersPlugin.getInstance().getConfig().getInt("Main.Spawner Max Upgrade"))
                player.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.success", currentStackSize));
            else
                player.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.successmaxed", currentStackSize));

            SpawnerChangeEvent event = new SpawnerChangeEvent(player, this, currentStackSize, oldStackSize);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            Location loc = location.clone();
            loc.setX(loc.getX() + .5);
            loc.setY(loc.getY() + .5);
            loc.setZ(loc.getZ() + .5);
            player.getWorld().spawnParticle(Particle.valueOf(EpicSpawnersPlugin.getInstance().getConfig().getString("Main.Upgrade Particle Type")), loc, 100, 0.5, 0.5, 0.5);

            if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Sounds Enabled")) {
                return;
            }
            if (currentStackSize != EpicSpawnersPlugin.getInstance().getConfig().getInt("Main.Spawner Max Upgrade")) {
            	player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 15.0F);
            } else {
            	player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2F, 25.0F);
            	new BukkitRunnable() {

            		private int count = 0;

            		@Override
            		public void run() {
            			if (count == 0) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_CHIME, 2F, 25.0F);
            			else if (count == 1) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_CHIME, 1.2F, 25.0F);
            			else if (count == 2) {
            				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_CHIME, 1.8F, 25.0F);
            				this.cancel();
            			}

            			this.count++;
            		};
            	}.runTaskTimer(EpicSpawnersPlugin.getInstance(), 0, 5);
            }
            EpicSpawnersPlugin.getInstance().getHologramHandler().updateHologram(this);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }


    public void upgrade(Player player, CostType type) {
        try {
            int cost = getUpgradeCost(type);

            boolean maxed = false;

            if (getSpawnerDataCount() == EpicSpawnersPlugin.getInstance().getConfig().getInt("Main.Spawner Max Upgrade")) {
                maxed = true;
            }
            if (maxed) {
                player.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.maxed"));
            } else {
                if (type == CostType.ECONOMY) {
                    if (EpicSpawnersPlugin.getInstance().getServer().getPluginManager().getPlugin("Vault") != null) {
                        RegisteredServiceProvider<Economy> rsp = EpicSpawnersPlugin.getInstance().getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                        net.milkbowl.vault.economy.Economy econ = rsp.getProvider();
                        if (econ.has(player, cost)) {
                            econ.withdrawPlayer(player, cost);
                            int oldMultiplier = getSpawnerDataCount();
                            spawnerStacks.getFirst().setStackSize(spawnerStacks.getFirst().getStackSize() + 1);
                            upgradeFinal(player, oldMultiplier);
                        } else {
                            player.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford"));
                        }
                    } else {
                        player.sendMessage("Vault is not installed.");
                    }
                } else if (type == CostType.EXPERIENCE) {
                    if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            player.setLevel(player.getLevel() - cost);
                        }
                        int oldMultiplier = getSpawnerDataCount();
                        spawnerStacks.getFirst().setStackSize(spawnerStacks.getFirst().getStackSize() + 1);
                        upgradeFinal(player, oldMultiplier);
                    } else {
                        player.sendMessage(EpicSpawnersPlugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford"));
                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    @Override
    public int getBoost() {
        EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();
        if (placedBy == null) return 0;

        Set<BoostData> boosts = instance.getBoostManager().getBoosts();

        if (boosts.size() == 0) return 0;

        int amountToBoost = 0;

        for (BoostData boostData : boosts) {
            if (System.currentTimeMillis() >= boostData.getEndTime()) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> instance.getBoostManager().removeBoostFromSpawner(boostData), 1);
                continue;
            }

            switch (boostData.getBoostType()) {
                case LOCATION:
                    if (!location.equals(boostData.getData())) continue;
                    break;
                case PLAYER:
                    if (!placedBy.toString().equals(boostData.getData())) continue;
                    break;
                case FACTION:
                    if (!instance.isInFaction((String) boostData.getData(), location)) continue;
                    break;
                case ISLAND:
                    if (!instance.isInIsland((String) boostData.getData(), location)) continue;
                    break;
                case TOWN:
                    if (!instance.isInTown((String) boostData.getData(), location)) continue;
                    break;
            }
            amountToBoost += boostData.getAmtBoosted();
        }
        return amountToBoost;
    }

    @Override
    public Instant getBoostEnd() { //ToDo: Wrong.
        EpicSpawnersPlugin instance = EpicSpawnersPlugin.getInstance();

        Set<BoostData> boosts = instance.getBoostManager().getBoosts();

        if (boosts.size() == 0) return null;

        for (BoostData boostData : boosts) {
            if (System.currentTimeMillis() >= boostData.getEndTime()) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> instance.getBoostManager().removeBoostFromSpawner(boostData), 1);
                continue;
            }

            switch (boostData.getBoostType()) {
                case LOCATION:
                    if (!location.equals(boostData.getData())) continue;
                    break;
                case PLAYER:
                    if (!placedBy.toString().equals(boostData.getData())) continue;
                    break;
                case FACTION:
                    if (!instance.isInFaction((String) boostData.getData(), location)) continue;
                    break;
                case ISLAND:
                    if (!instance.isInIsland((String) boostData.getData(), location)) continue;
                    break;
                case TOWN:
                    if (!instance.isInTown((String) boostData.getData(), location)) continue;
                    break;
            }

            return Instant.ofEpochMilli(boostData.getEndTime());
        }
        return null;
    }

    private int lastDelay = 0;
    private int lastMulti = 0;
    private static final Random rand = new Random();

    @Override
    public int updateDelay() { //ToDO: Should be redesigned to work with spawner.setmaxdelay
        try {
            if (!EpicSpawnersPlugin.getInstance().getConfig().getBoolean("Main.Default Minecraft Spawner Cooldowns"))
                return 0;

            String equation = EpicSpawnersPlugin.getInstance().getConfig().getString("Main.Equations.Cooldown Between Spawns");

            int max = 0;
            int min = 0;
            for (SpawnerStack stack : spawnerStacks) { //ToDo: You can probably do this only on spawner stack or upgrade.
                String tickRate = stack.getSpawnerData().getTickRate();

                String[] tick = tickRate.contains(":") ? tickRate.split(":") : new String[]{tickRate, tickRate};

                int tickMin = Integer.parseInt(tick[1]);
                int tickMax = Integer.parseInt(tick[0]);
                if (max == 0 && min == 0) {
                    max = tickMax;
                    min = tickMin;
                    continue;
                }
                if ((max + min) < (tickMax + min)) {
                    max = tickMax;
                    min = tickMin;
                }
            }

            int delay;
            if (!EpicSpawnersPlugin.getInstance().cache.containsKey(equation) || (max + min) != lastDelay || getSpawnerDataCount() != lastMulti) {
                equation = equation.replace("{DEFAULT}", Integer.toString(rand.nextInt(Math.max(max, 0) + min)));
                equation = equation.replace("{MULTI}", Integer.toString(getSpawnerDataCount()));
                try {
                    delay = (int) Math.round(Double.parseDouble(engine.eval(equation).toString()));
                } catch (IllegalArgumentException ex) {
                    delay = 30;
                }
                EpicSpawnersPlugin.getInstance().cache.put(equation, delay);
                lastDelay = max + min;
                lastMulti = getSpawnerDataCount();
            } else {
                delay = EpicSpawnersPlugin.getInstance().cache.get(equation);
            }

            if (getCreatureSpawner().getSpawnedType() != EntityType.DROPPED_ITEM)
                getCreatureSpawner().setDelay(delay);
            getCreatureSpawner().update();

            return delay;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return 999999;
    }

    @Override
    public String getIdentifyingName() {
        String name = spawnerStacks.getFirst().getSpawnerData().getIdentifyingName();

        if (spawnerStacks.size() > 1)
            name = EpicSpawnersPlugin.getInstance().getSpawnerManager().getSpawnerData("omni").getIdentifyingName();

        return name;
    }

    @Override
    public String getDisplayName() {
        if (spawnerStacks.size() == 0) {
            return Methods.getTypeFromString(creatureSpawner.getSpawnedType().name());
        } else if (spawnerStacks.size() > 1) {
            return EpicSpawnersPlugin.getInstance().getSpawnerManager().getSpawnerData("omni").getDisplayName();
        }

        return spawnerStacks.getFirst().getSpawnerData().getDisplayName();
    }

    @Override
    public Collection<SpawnerStack> getSpawnerStacks() {
        return Collections.unmodifiableCollection(spawnerStacks);
    }

    @Override
    public void clearSpawnerStacks() {
        spawnerStacks.clear();
    }

    @Override
    public OfflinePlayer getPlacedBy() {
        if (placedBy == null) return null;
        return Bukkit.getOfflinePlayer(placedBy);
    }

    public void setPlacedBy(Player placedBy) {
        this.placedBy = placedBy.getUniqueId();
    }

    public void setPlacedBy(UUID placedBy) {
        this.placedBy = placedBy;
    }

    @Override
    public int getSpawnCount() {
        return spawnCount;
    }

    @Override
    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
    }

    public String getOmniState() {
        return omniState;
    }

    public void setOmniState(String omniState) {
        this.omniState = omniState;
    }

    @Override
    public int hashCode() {
        int result = 31 * (location == null ? 0 : location.hashCode());
        result = 31 * result + (placedBy == null ? 0 : placedBy.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ESpawner)) return false;

        ESpawner other = (ESpawner) obj;
        return Objects.equals(location, other.location) && Objects.equals(placedBy, other.placedBy);
    }

    @Override
    public String toString() {
        return "ESpawner:{"
                + "Owner:\"" + placedBy + "\","
                + "Location:{"
                    + "World:\"" + location.getWorld().getName() + "\","
                    + "X:" + location.getBlockX() + ","
                    + "Y:" + location.getBlockY() + ","
                    + "Z:" + location.getBlockZ()
                + "},"
                + "StackCount:" + spawnerStacks.size()
             + "}";
    }

}
