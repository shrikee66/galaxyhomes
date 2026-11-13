package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.GeyserUtils;
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
        if (GeyserUtils.isBedrockPlayer(player)) {
            openBedrockHomes(player);
            return;
        }
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        Inventory inventory = Bukkit.createInventory(null, 54, HOMES_TITLE);
        int start = Math.max(0, page) * 45;
        for (int i = start; i < Math.min(start + 45, homes.size()); i++) {
            Home home = homes.get(i);
            ItemStack item = new ItemStack(Material.RED_BED);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + home.getName());
            meta.setLore(List.of(ChatColor.GRAY + home.getWorldName(),
                ChatColor.GRAY + (int) home.getX() + ", " + (int) home.getY() + ", " + (int) home.getZ(),
                ChatColor.GREEN + "Click to teleport"));
            meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, home.getName());
            item.setItemMeta(meta);
            inventory.setItem(i - start, item);
        }
        inventory.setItem(48, action(Material.LIME_CONCRETE, ChatColor.GREEN + "+ New home here", "new_home"));
        inventory.setItem(49, action(Material.OAK_SIGN, ChatColor.YELLOW + "Manage homes", "manage_list:0"));
        if (start > 0) inventory.setItem(45, action(Material.ARROW, ChatColor.GRAY + "Previous page", "page:" + (page - 1)));
        if (start + 45 < homes.size()) inventory.setItem(53, action(Material.ARROW, ChatColor.GRAY + "Next page", "page:" + (page + 1)));
        player.openInventory(inventory);
    }

    private void openBedrockHomes(Player player) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        List<GeyserUtils.Button> buttons = new ArrayList<>();
        buttons.add(new GeyserUtils.Button("+ New home here", "new"));
        for (Home home : homes) buttons.add(new GeyserUtils.Button(home.getName(), "home:" + home.getName()));
        if (!homes.isEmpty()) buttons.add(new GeyserUtils.Button("Manage homes", "manage"));
        GeyserUtils.sendForm(player, "homes", "Homes", homes.size() + " / " + plugin.getConfigManager().getMaxHomes() + " homes", buttons,
            (viewer, response) -> {
                if (response.equals("new")) createHome(viewer);
                else if (response.equals("manage")) openBedrockManageList(viewer);
                else if (response.startsWith("home:")) {
                    Home home = data(viewer).getHome(response.substring(5));
                    if (home != null) teleport(viewer, home);
                }
            });
    }

    private void openBedrockManageList(Player player) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        List<GeyserUtils.Button> buttons = homes.stream()
            .map(home -> new GeyserUtils.Button(home.getName(), "select:" + home.getName()))
            .toList();
        GeyserUtils.sendForm(player, "manage-homes", "Manage homes",
            homes.isEmpty() ? "You have no homes." : "Choose a home to manage.", buttons,
            (viewer, response) -> {
                if (response.startsWith("select:")) openBedrockHomeActions(viewer, response.substring(7));
            });
    }

    private void openBedrockHomeActions(Player player, String name) {
        Home home = data(player).getHome(name);
        if (home == null) return;
        GeyserUtils.sendForm(player, "manage-home-" + name, name,
            home.getWorldName() + " - " + (int) home.getX() + " " + (int) home.getY() + " " + (int) home.getZ(),
            List.of(new GeyserUtils.Button("Teleport", "teleport"),
                new GeyserUtils.Button("Rename", "rename"),
                new GeyserUtils.Button("Delete", "delete"),
                new GeyserUtils.Button("Back", "back")),
            (viewer, response) -> {
                switch (response) {
                    case "teleport" -> teleport(viewer, home);
                    case "rename" -> GeyserUtils.sendInput(viewer, "Rename home", "New home name", home.getName(),
                        (renamer, newName) -> rename(renamer, home.getName(), newName));
                    case "delete" -> openBedrockDeleteConfirm(viewer, home.getName());
                    case "back" -> openBedrockManageList(viewer);
                }
            });
    }

    private void openBedrockDeleteConfirm(Player player, String name) {
        GeyserUtils.sendForm(player, "delete-home-" + name, "Delete home",
            "Delete '" + name + "'? This cannot be undone.",
            List.of(new GeyserUtils.Button("Confirm", "confirm"),
                new GeyserUtils.Button("Cancel", "cancel"), new GeyserUtils.Button("Back", "back")),
            (viewer, response) -> {
                if (response.equals("confirm")) {
                    if (data(viewer).removeHome(name)) MessageUtils.sendMessage(viewer, "home-deleted", Map.of("name", name));
                    openBedrockManageList(viewer);
                } else if (response.equals("back")) openBedrockHomeActions(viewer, name);
            });
    }

    public void openManage(Player player, Home home) {
        Inventory inventory = Bukkit.createInventory(null, 27, MANAGE_TITLE);
        inventory.setItem(4, actionWithHome(Material.PAPER, ChatColor.WHITE + home.getName(), "info", home.getName()));
        inventory.setItem(10, actionWithHome(Material.ENDER_PEARL, ChatColor.GREEN + "Teleport", "teleport", home.getName()));
        inventory.setItem(13, actionWithHome(Material.NAME_TAG, ChatColor.YELLOW + "Rename", "rename", home.getName()));
        inventory.setItem(16, actionWithHome(Material.BARRIER, ChatColor.RED + "Delete", "delete", home.getName()));
        inventory.setItem(22, action(Material.ARROW, ChatColor.GRAY + "Back", "back"));
        player.openInventory(inventory);
    }

    public void openDeleteConfirm(Player player, Home home) {
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        inventory.setItem(11, actionWithHome(Material.LIME_CONCRETE, ChatColor.GREEN + "Confirm", "confirm_delete", home.getName()));
        inventory.setItem(13, actionWithHome(Material.ARROW, ChatColor.YELLOW + "Back", "back_manage", home.getName()));
        inventory.setItem(15, action(Material.RED_CONCRETE, ChatColor.RED + "Cancel", "cancel"));
        player.openInventory(inventory);
    }

    private ItemStack actionWithHome(Material material, String display, String action, String name) {
        ItemStack item = action(material, display, action);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack action(Material material, String display, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(display);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    public String getHomeName(ItemMeta meta) { return meta.getPersistentDataContainer().get(homeKey, PersistentDataType.STRING); }
    public String getAction(ItemMeta meta) { return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING); }

    public void createHome(Player player) {
        var playerData = data(player);
        int max = plugin.getConfigManager().getMaxHomes();
        if (max >= 0 && playerData.getHomeCount() >= max) {
            MessageUtils.sendMessage(player, "home-limit-reached", Map.of("limit", String.valueOf(max)));
            return;
        }
        String base = plugin.getConfigManager().getDefaultHomeName();
        String name = base;
        int suffix = 2;
        while (playerData.getHome(name) != null) name = base + suffix++;
        playerData.addHome(new Home(name, player.getLocation()));
        MessageUtils.sendMessage(player, "home-set", Map.of("name", name));
    }

    public void rename(Player player, String oldName, String newName) {
        if (newName == null || newName.isBlank() || newName.length() > plugin.getConfigManager().getMaxNameLength()) {
            MessageUtils.sendMessage(player, "home-name-too-long", Map.of("max", String.valueOf(plugin.getConfigManager().getMaxNameLength())));
            return;
        }
        var playerData = data(player);
        if (playerData.getHome(newName) != null) {
            player.sendMessage(ChatColor.RED + "A home with that name already exists.");
            return;
        }
        if (playerData.renameHome(oldName, newName)) {
            MessageUtils.sendMessage(player, "home-rename-success", Map.of("old", oldName, "new", newName));
        }
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
