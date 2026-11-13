package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomesCommand extends BaseCommand {
    public static final String HOMES_TITLE = "Homes";
    public static final String MANAGE_TITLE = "Manage homes";
    public static final String CONFIRM_TITLE = "Delete home?";
    private final NamespacedKey homeKey;
    private final NamespacedKey actionKey;

    public HomesCommand(GalaxyHomes plugin) {
        super(plugin);
        homeKey = new NamespacedKey(plugin, "home_name");
        actionKey = new NamespacedKey(plugin, "gui_action");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!permission(sender, "galaxyhomes.homes")) return true;
        Player player = player(sender);
        if (player != null) open(player, 0);
        return true;
    }

    public void open(Player player, int page) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        Inventory inventory = Bukkit.createInventory(null, 54, HOMES_TITLE);
        if (homes.isEmpty()) {
            inventory.setItem(22, action(Material.LIME_CONCRETE, ChatColor.GREEN + "+ New home here", "new_home"));
            player.openInventory(inventory);
            return;
        }
        int start = Math.max(0, page) * 45;
        for (int i = start; i < Math.min(start + 45, homes.size()); i++) {
            Home home = homes.get(i);
            ItemStack item = new ItemStack(Material.RED_BED);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + home.getName());
            meta.setLore(List.of(ChatColor.GRAY + home.getWorldName(), ChatColor.GRAY + (int) home.getX() + ", " + (int) home.getY() + ", " + (int) home.getZ(), ChatColor.YELLOW + "Click to manage"));
            meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, home.getName());
            item.setItemMeta(meta);
            inventory.setItem(i - start, item);
        }
        inventory.setItem(49, action(Material.OAK_SIGN, ChatColor.YELLOW + "Manage homes", "manage_list"));
        inventory.setItem(48, action(Material.LIME_CONCRETE, ChatColor.GREEN + "+ New home here", "new_home"));
        if (start > 0) inventory.setItem(45, action(Material.ARROW, ChatColor.GRAY + "Previous page", "page:" + (page - 1)));
        if (start + 45 < homes.size()) inventory.setItem(53, action(Material.ARROW, ChatColor.GRAY + "Next page", "page:" + (page + 1)));
        player.openInventory(inventory);
    }

    public void openManage(Player player, Home home) {
        Inventory inventory = Bukkit.createInventory(null, 27, MANAGE_TITLE);
        ItemStack title = new ItemStack(Material.PAPER);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.setDisplayName(ChatColor.WHITE + home.getName());
        titleMeta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, home.getName());
        title.setItemMeta(titleMeta);
        inventory.setItem(4, title);
        inventory.setItem(10, homeAction(Material.ENDER_PEARL, ChatColor.GREEN + "Teleport", "teleport", home.getName()));
        inventory.setItem(13, homeAction(Material.NAME_TAG, ChatColor.YELLOW + "Rename", "rename", home.getName()));
        inventory.setItem(16, homeAction(Material.BARRIER, ChatColor.RED + "Delete", "delete", home.getName()));
        inventory.setItem(22, action(Material.ARROW, ChatColor.GRAY + "Back", "back"));
        player.openInventory(inventory);
    }

    public void openDeleteConfirm(Player player, Home home) {
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        inventory.setItem(11, homeAction(Material.LIME_CONCRETE, ChatColor.GREEN + "Confirm", "confirm_delete", home.getName()));
        inventory.setItem(13, action(Material.ARROW, ChatColor.YELLOW + "Back", "back_manage:" + home.getName()));
        inventory.setItem(15, action(Material.RED_CONCRETE, ChatColor.RED + "Cancel", "cancel"));
        player.openInventory(inventory);
    }

    private ItemStack homeAction(Material material, String name, String action, String home) {
        ItemStack item = action(material, name, action);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, home);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack action(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    public String getHomeName(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(homeKey, PersistentDataType.STRING);
    }

    public String getAction(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public void teleport(Player player, Home home) {
        var location = home.toLocation();
        if (location == null) {
            player.sendMessage(ChatColor.RED + "That home's world is unavailable.");
            return;
        }
        MessageUtils.sendMessage(player, "home-teleporting", Map.of("name", home.getName()));
        plugin.getTeleportManager().startTeleport(player, location, "home", success -> {
            if (success) MessageUtils.sendMessage(player, "home-teleported", Map.of("name", home.getName()));
        });
    }
}
