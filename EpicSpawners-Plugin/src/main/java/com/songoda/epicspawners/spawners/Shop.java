package com.songoda.epicspawners.spawners;

import java.util.ArrayList;
import java.util.List;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.epicspawners.EpicSpawnersPlugin;
import com.songoda.epicspawners.api.spawner.SpawnerData;
import com.songoda.epicspawners.utils.Debugger;
import com.songoda.epicspawners.utils.HeadType;
import com.songoda.epicspawners.utils.Methods;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Created by songoda on 3/10/2017.
 */
public class Shop {

    private final EpicSpawnersPlugin instance;

    public Shop(EpicSpawnersPlugin instance) {
        this.instance = instance;
    }

    public void open(Player p, int page) {
        try {
            this.instance.page.put(p, page);

            List<SpawnerData> entities = new ArrayList<>();

            int num = 0, show = 0;
            int start = (page - 1) * 33;

            for (SpawnerData spawnerData : instance.getSpawnerManager().getAllSpawnerData()) {
                if (!spawnerData.getIdentifyingName().toLowerCase().equals("omni")
                        && spawnerData.isInShop() && spawnerData.isActive()
                        && p.hasPermission("epicspawners.shop." + Methods.getTypeFromString(spawnerData.getIdentifyingName()).replaceAll(" ", "_"))) {
                    if (num >= start && show <= 33) {
                        entities.add(spawnerData);
                        show++;
                    }
                }
                num++;
            }

            int amount = entities.size();
            String title = TextComponent.formatTitle(instance.getLocale().getMessage("interface.shop.title"));
            Inventory inventory = Bukkit.createInventory(null, 54, title);
            int max2 = 54;

            if (amount <= 7) {
                inventory = Bukkit.createInventory(null, 27, title);
                max2 = 27;
            } else if (amount <= 15) {
                inventory = Bukkit.createInventory(null, 36, title);
                max2 = 36;
            } else if (amount <= 25) {
                inventory = Bukkit.createInventory(null, 45, title);
                max2 = 45;
            }

            int max22 = max2;
            int place = 10;
            for (SpawnerData spawnerData : entities) {
                if (place == 17 || place == (max22 - 18)) place++;

                ItemStack it = new ItemStack(Material.PLAYER_HEAD);
                ItemStack item = HeadType.addTexture(it, spawnerData);

                if (spawnerData.getDisplayItem() != null) {
                    Material mat = spawnerData.getDisplayItem();
                    if (mat != Material.AIR)
                        item = new ItemStack(mat, 1);
                }

                ItemMeta itemmeta = item.getItemMeta();
                String name = Methods.compileName(spawnerData.getIdentifyingName(), 1, true);
                ArrayList<String> lore = new ArrayList<>();
                double price = spawnerData.getShopPrice();
                lore.add(TextComponent.formatText(instance.getLocale().getMessage("interface.shop.buyprice", TextComponent.formatEconomy(price))));
                String loreString = instance.getLocale().getMessage("interface.shop.lore", Methods.getTypeFromString(Methods.getTypeFromString(spawnerData.getDisplayName())));
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    loreString = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, loreString.replace(" ", "_")).replace("_", " ");
                }
                lore.add(loreString);
                itemmeta.setLore(lore);
                itemmeta.setDisplayName(name);
                item.setItemMeta(itemmeta);
                inventory.setItem(place, item);
                place++;
            }

            int max = (int) Math.ceil((double) num / (double) 36);
            num = 0;
            while (num != 9) {
                inventory.setItem(num, Methods.getGlass());
                num++;
            }
            int num2 = max2 - 9;
            while (num2 != max2) {
                inventory.setItem(num2, Methods.getGlass());
                num2++;
            }

            ItemStack exit = new ItemStack(Material.valueOf(instance.getConfig().getString("Interfaces.Exit Icon")), 1);
            ItemMeta exitmeta = exit.getItemMeta();
            exitmeta.setDisplayName(instance.getLocale().getMessage("general.nametag.exit"));
            exit.setItemMeta(exitmeta);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head = HeadType.ARROW_RIGHT.addTexture(head);
            SkullMeta headMeta = (SkullMeta) head.getItemMeta();
            headMeta.setDisplayName(instance.getLocale().getMessage("general.nametag.next"));
            head.setItemMeta(headMeta);

            ItemStack head2 = new ItemStack(Material.PLAYER_HEAD);
            head2 = HeadType.ARROW_LEFT.addTexture(head2);
            SkullMeta head2Meta = (SkullMeta) head2.getItemMeta();
            head2Meta.setDisplayName(instance.getLocale().getMessage("general.nametag.back"));
            head2.setItemMeta(head2Meta);

            inventory.setItem(8, exit);

            inventory.setItem(0, Methods.getBackgroundGlass(true));
            inventory.setItem(1, Methods.getBackgroundGlass(true));
            inventory.setItem(9, Methods.getBackgroundGlass(true));

            inventory.setItem(7, Methods.getBackgroundGlass(true));
            inventory.setItem(17, Methods.getBackgroundGlass(true));

            inventory.setItem(max22 - 18, Methods.getBackgroundGlass(true));
            inventory.setItem(max22 - 9, Methods.getBackgroundGlass(true));
            inventory.setItem(max22 - 8, Methods.getBackgroundGlass(true));

            inventory.setItem(max22 - 10, Methods.getBackgroundGlass(true));
            inventory.setItem(max22 - 2, Methods.getBackgroundGlass(true));
            inventory.setItem(max22 - 1, Methods.getBackgroundGlass(true));

            inventory.setItem(2, Methods.getBackgroundGlass(false));
            inventory.setItem(6, Methods.getBackgroundGlass(false));
            inventory.setItem(max22 - 7, Methods.getBackgroundGlass(false));
            inventory.setItem(max22 - 3, Methods.getBackgroundGlass(false));

            if (page != 1) {
                inventory.setItem(max22 - 8, head2);
            }
            if (page != max) {
                inventory.setItem(max22 - 2, head);
            }

            p.openInventory(inventory);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void show(SpawnerData spawnerData, int amt, Player p) {
        try {
            Inventory inventory = Bukkit.createInventory(null, 45, TextComponent.formatTitle(instance.getLocale().getMessage("interface.shop.spawnershowtitle", Methods.compileName(spawnerData.getIdentifyingName(), 1, false))));

            int num = 0;
            while (num != 9) {
                inventory.setItem(num, Methods.getGlass());
                num++;
            }

            num = 36;
            while (num != 45) {
                inventory.setItem(num, Methods.getGlass());
                num++;
            }

            inventory.setItem(1, Methods.getBackgroundGlass(true));
            inventory.setItem(9, Methods.getBackgroundGlass(true));

            inventory.setItem(7, Methods.getBackgroundGlass(true));
            inventory.setItem(17, Methods.getBackgroundGlass(true));

            inventory.setItem(27, Methods.getBackgroundGlass(true));
            inventory.setItem(36, Methods.getBackgroundGlass(true));
            inventory.setItem(37, Methods.getBackgroundGlass(true));

            inventory.setItem(35, Methods.getBackgroundGlass(true));
            inventory.setItem(43, Methods.getBackgroundGlass(true));
            inventory.setItem(44, Methods.getBackgroundGlass(true));

            inventory.setItem(2, Methods.getBackgroundGlass(false));
            inventory.setItem(6, Methods.getBackgroundGlass(false));
            inventory.setItem(38, Methods.getBackgroundGlass(false));
            inventory.setItem(42, Methods.getBackgroundGlass(false));

            double price = spawnerData.getShopPrice() * amt;

            ItemStack it = new ItemStack(Material.PLAYER_HEAD, amt);

            ItemStack item = HeadType.addTexture(it, spawnerData);


            if (spawnerData.getDisplayItem() != null) {
                Material mat = spawnerData.getDisplayItem();
                if (!mat.equals(Material.AIR))
                    item = new ItemStack(mat, 1);
            }

            item.setAmount(amt);
            ItemMeta itemmeta = item.getItemMeta();
            String name = Methods.compileName(spawnerData.getIdentifyingName(), 1, false);
            itemmeta.setDisplayName(name);
            ArrayList<String> lore = new ArrayList<>();
            lore.add(instance.getLocale().getMessage("interface.shop.buyprice", TextComponent.formatEconomy(price)));
            itemmeta.setLore(lore);
            item.setItemMeta(itemmeta);
            inventory.setItem(22, item);


            ItemStack plus = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta plusmeta = plus.getItemMeta();
            plusmeta.setDisplayName(instance.getLocale().getMessage("interface.shop.add1"));
            plus.setItemMeta(plusmeta);
            if (item.getAmount() + 1 <= 64) {
                inventory.setItem(15, plus);
            }

            plus = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 10);
            plusmeta.setDisplayName(instance.getLocale().getMessage("interface.shop.add10"));
            plus.setItemMeta(plusmeta);
            if (item.getAmount() + 10 <= 64) {
                inventory.setItem(33, plus);
            }

            plus = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 64);
            plusmeta.setDisplayName(instance.getLocale().getMessage("interface.shop.set64"));
            plus.setItemMeta(plusmeta);
            if (item.getAmount() != 64) {
                inventory.setItem(25, plus);
            }

            ItemStack minus = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta minusmeta = minus.getItemMeta();
            minusmeta.setDisplayName(instance.getLocale().getMessage("interface.shop.remove1"));
            minus.setItemMeta(minusmeta);
            if (item.getAmount() != 1) {
                inventory.setItem(11, minus);
            }

            minus = new ItemStack(Material.RED_STAINED_GLASS_PANE, 10);
            minusmeta.setDisplayName(instance.getLocale().getMessage("interface.shop.remove10"));
            minus.setItemMeta(minusmeta);
            if (item.getAmount() - 10 >= 0) {
                inventory.setItem(29, minus);
            }

            minus = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            minusmeta.setDisplayName(instance.getLocale().getMessage("interface.shop.set1"));
            minus.setItemMeta(minusmeta);
            if (item.getAmount() != 1) {
                inventory.setItem(19, minus);
            }

            ItemStack exit = new ItemStack(Material.valueOf(EpicSpawnersPlugin.getInstance().getConfig().getString("Interfaces.Exit Icon")), 1);
            ItemMeta exitmeta = exit.getItemMeta();
            exitmeta.setDisplayName(instance.getLocale().getMessage("general.nametag.exit"));
            exit.setItemMeta(exitmeta);
            inventory.setItem(8, exit);

            ItemStack head2 = new ItemStack(Material.PLAYER_HEAD);
            head2 = HeadType.ARROW_LEFT.addTexture(head2);
            SkullMeta head2Meta = (SkullMeta) head2.getItemMeta();
            head2Meta.setDisplayName(instance.getLocale().getMessage("general.nametag.back"));
            head2.setItemMeta(head2Meta);

            inventory.setItem(0, head2);

            ItemStack buy = new ItemStack(Material.valueOf(EpicSpawnersPlugin.getInstance().getConfig().getString("Interfaces.Buy Icon")), 1);
            ItemMeta buymeta = buy.getItemMeta();
            buymeta.setDisplayName(instance.getLocale().getMessage("general.nametag.confirm"));
            buy.setItemMeta(buymeta);
            inventory.setItem(40, buy);

            p.openInventory(inventory);

            p.openInventory(inventory);
            EpicSpawnersPlugin.getInstance().inShow.put(p, spawnerData); //ToDo: This system needs to be removed.
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void confirm(Player p, int amount) {
        try {
            SpawnerData spawnerData = EpicSpawnersPlugin.getInstance().inShow.get(p);
            if (EpicSpawnersPlugin.getInstance().getServer().getPluginManager().getPlugin("Vault") == null) {
                p.sendMessage("Vault is not installed.");
                return;
            }
            RegisteredServiceProvider<Economy> rsp = EpicSpawnersPlugin.getInstance().getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            net.milkbowl.vault.economy.Economy econ = rsp.getProvider();
            double price = spawnerData.getShopPrice() * amount;
            if (!p.isOp() && !econ.has(p, price)) {
                p.sendMessage(EpicSpawnersPlugin.getInstance().getPrefix() + instance.getLocale().getMessage("event.shop.cannotafford"));
                return;
            }
            ItemStack item = spawnerData.toItemStack(amount);


            p.getInventory().addItem(item);

            p.sendMessage(EpicSpawnersPlugin.getInstance().getPrefix() + instance.getLocale().getMessage("event.shop.purchasesuccess"));


            if (!p.isOp()) {
                econ.withdrawPlayer(p, price);
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }
}
