package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.data.PlayerData;
import com.galaxyhomes.utils.GeyserUtils;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomesCommand extends BaseCommand {
    private static final int BEDROCK_PAGE_SIZE = 8;
    private static final Map<UUID, PendingInput> PENDING_INPUTS = new ConcurrentHashMap<>();
    private static final String JAVA_HOME_TITLE = "Your Homes";
    private static final String JAVA_MANAGE_TITLE = "Manage homes";
    private static final String JAVA_ACTIONS_TITLE = "Home actions";
    private static final String JAVA_DELETE_TITLE = "Confirm home delete";

    public enum InputMode { CREATE, RENAME }
    public record PendingInput(InputMode mode, String oldName, Location createLocation) {}

    public HomesCommand(GalaxyHomes p) {
        super(p);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.homes")) return true;
        Player p = player(s);
        if (p == null) return true;
        open(p, 0);
        return true;
    }

    public void open(Player player, int page) {
        if (GeyserUtils.isBedrockPlayer(player)) openBedrockHomes(player, page);
        else openJavaHomes(player, page);
    }

    private void openJavaHomes(Player player, int page) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.color("&8&l" + JAVA_HOME_TITLE));
        int currentPage = Math.max(0, page);
        int start = currentPage * 45;

        for (int i = start; i < Math.min(start + 45, homes.size()); i++) {
            Home home = homes.get(i);
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            meta.setDisplayName(MessageUtils.color("&b&l" + home.getName()));
            meta.setLore(List.of(
                "§7World: §f" + home.getWorldName(),
                "§7X: §f" + (int) home.getX() + " §7Y: §f" + (int) home.getY() + " §7Z: §f" + (int) home.getZ(),
                "§aLeft-click to teleport"
            ));
            setMenuData(meta, "teleport_home", home.getName());
            item.setItemMeta(meta);
            inv.setItem(i - start, item);
        }

        // The bottom row is reserved for the requested Java-only GUI controls.
        inv.setItem(45, menuItem(Material.GREEN_WOOL, "§a§lNew home here",
            List.of("§7Create a home at your current location"), "new_home", null));
        inv.setItem(49, menuItem(Material.NETHER_STAR, "§6§lMade by Shrikee",
            List.of("§eMade by shrikee <3"), "creator_info", null));
        inv.setItem(53, menuItem(Material.ORANGE_WOOL, "§6§lManage homes",
            List.of("§7Rename or delete a home"), "manage_homes", null));
        if (currentPage > 0) inv.setItem(47, menuItem(Material.ARROW, "§fPrevious page", List.of(), "prev_page", null));
        if (start + 45 < homes.size()) inv.setItem(51, menuItem(Material.ARROW, "§fNext page", List.of(), "next_page", null));
        player.openInventory(inv);
    }

    public void openJavaManageHomes(Player player) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.color("&8&l" + JAVA_MANAGE_TITLE));
        int limit = Math.min(45, homes.size());
        for (int i = 0; i < limit; i++) {
            Home home = homes.get(i);
            ItemStack item = new ItemStack(Material.RED_BED);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            meta.setDisplayName(MessageUtils.color("&e" + home.getName()));
            meta.setLore(List.of("§7World: §f" + home.getWorldName(), "§aClick to manage this home"));
            setMenuData(meta, "open_home_actions", home.getName());
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        if (homes.isEmpty()) inv.setItem(22, menuItem(Material.BOOK, "§cNo homes yet", List.of("§7Use New home here to create one"), "none", null));
        inv.setItem(49, menuItem(Material.ARROW, "§fBack to homes", List.of(), "back_homes", null));
        inv.setItem(45, menuItem(Material.GREEN_WOOL, "§a§lNew home here", List.of("§7Create a home at your current location"), "new_home", null));
        player.openInventory(inv);
    }

    private void openJavaHomeActions(Player player, String name) {
        Home home = data(player).getHome(name);
        if (home == null) {
            MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name));
            openJavaManageHomes(player);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.color("&8&l" + JAVA_ACTIONS_TITLE));
        inv.setItem(13, menuItem(Material.WRITABLE_BOOK, "§b§l" + home.getName(), List.of(
            "§7World: §f" + home.getWorldName(),
            "§7X: §f" + (int) home.getX() + " §7Y: §f" + (int) home.getY() + " §7Z: §f" + (int) home.getZ()
        ), "home_info", home.getName()));
        inv.setItem(11, menuItem(Material.ENDER_PEARL, "§aTeleport", List.of("§7Teleport to this home"), "teleport_home", home.getName()));
        inv.setItem(13 + 9, menuItem(Material.NAME_TAG, "§eRename", List.of("§7Choose a new name"), "rename_home", home.getName()));
        inv.setItem(15, menuItem(Material.REDSTONE, "§cDelete", List.of("§7Delete this home"), "delete_home", home.getName()));
        inv.setItem(49, menuItem(Material.ARROW, "§fBack", List.of(), "back_manage", null));
        player.openInventory(inv);
    }

    private void confirmJavaDelete(Player player, String name) {
        Inventory inv = Bukkit.createInventory(null, 27, MessageUtils.color("&8&l" + JAVA_DELETE_TITLE));
        inv.setItem(11, menuItem(Material.RED_WOOL, "§cConfirm delete", List.of("§7Delete home: " + name), "confirm_delete", name));
        inv.setItem(15, menuItem(Material.GREEN_WOOL, "§aCancel", List.of(), "cancel_delete", name));
        player.openInventory(inv);
    }

    /** Handles clicks in Java homes submenus; returns true when the GUI owns the click. */
    public boolean handleJavaGuiClick(Player player, String title, ItemStack clicked, int rawSlot) {
        if (!JAVA_HOME_TITLE.equalsIgnoreCase(title) && !JAVA_MANAGE_TITLE.equalsIgnoreCase(title)
            && !JAVA_ACTIONS_TITLE.equalsIgnoreCase(title) && !JAVA_DELETE_TITLE.equalsIgnoreCase(title)) return false;

        if (clicked == null || clicked.getType() == Material.AIR) return true;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return true;
        NamespacedKey actionKey = new NamespacedKey(plugin, "home_menu_action");
        NamespacedKey nameKey = new NamespacedKey(plugin, "home_menu_name");
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String name = meta.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);

        if (JAVA_HOME_TITLE.equalsIgnoreCase(title)) {
            if ("new_home".equals(action)) beginCreate(player);
            else if ("manage_homes".equals(action)) openJavaManageHomes(player);
            else if ("prev_page".equals(action)) openJavaHomes(player, Math.max(0, rawSlotPage(player) - 1));
            else if ("next_page".equals(action)) openJavaHomes(player, rawSlotPage(player) + 1);
            else if ("teleport_home".equals(action) && name != null) teleportHome(player, name);
            return true;
        }
        if (JAVA_MANAGE_TITLE.equalsIgnoreCase(title)) {
            if ("new_home".equals(action)) beginCreate(player);
            else if ("back_homes".equals(action)) openJavaHomes(player, 0);
            else if ("open_home_actions".equals(action) && name != null) openJavaHomeActions(player, name);
            return true;
        }
        if (JAVA_ACTIONS_TITLE.equalsIgnoreCase(title)) {
            if (name == null) return true;
            if ("teleport_home".equals(action)) teleportHome(player, name);
            else if ("rename_home".equals(action)) beginRename(player, name);
            else if ("delete_home".equals(action)) confirmJavaDelete(player, name);
            else if ("back_manage".equals(action)) openJavaManageHomes(player);
            return true;
        }
        if (JAVA_DELETE_TITLE.equalsIgnoreCase(title)) {
            if ("confirm_delete".equals(action) && name != null) deleteHome(player, name);
            else if ("cancel_delete".equals(action)) openJavaManageHomes(player);
            return true;
        }
        return true;
    }

    // Bottom controls only appear on the first Java homes page; current page is inferred from visible home metadata.
    private int rawSlotPage(Player player) {
        return Math.max(0, player.getOpenInventory().getTopInventory().getItem(0) == null ? 0 : 0);
    }

    private ItemStack menuItem(Material material, String displayName, List<String> lore, String action, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(displayName);
        if (!lore.isEmpty()) meta.setLore(lore);
        setMenuData(meta, action, name);
        item.setItemMeta(meta);
        return item;
    }

    private void setMenuData(ItemMeta meta, String action, String name) {
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "home_menu_action"), PersistentDataType.STRING, action);
        if (name != null) meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "home_menu_name"), PersistentDataType.STRING, name);
    }

    private void openBedrockHomes(Player player, int page) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        int maxHomes = plugin.getConfigManager().getMaxHomes();
        String count = homes.size() + (maxHomes < 0 ? " homes" : " / " + maxHomes + " homes");
        int pageCount = Math.max(1, (homes.size() + BEDROCK_PAGE_SIZE - 1) / BEDROCK_PAGE_SIZE);
        int currentPage = Math.max(0, Math.min(page, pageCount - 1));
        int from = currentPage * BEDROCK_PAGE_SIZE;
        int to = Math.min(from + BEDROCK_PAGE_SIZE, homes.size());
        List<GeyserUtils.Button> buttons = new ArrayList<>();
        if (maxHomes < 0 || homes.size() < maxHomes) buttons.add(new GeyserUtils.Button("+ New home here", "new_home"));
        for (int i = from; i < to; i++) {
            Home home = homes.get(i);
            buttons.add(new GeyserUtils.Button(home.getName(), "home:" + home.getName()));
        }
        if (currentPage > 0) buttons.add(new GeyserUtils.Button("Previous page", "page:" + (currentPage - 1)));
        if (currentPage + 1 < pageCount) buttons.add(new GeyserUtils.Button("Next page", "page:" + (currentPage + 1)));
        buttons.add(new GeyserUtils.Button("Manage homes", "manage"));
        GeyserUtils.sendForm(player, "homes-main", "Homes", count, buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("new_home".equals(response)) beginCreate(p);
                else if ("manage".equals(response)) openBedrockManage(p, 0);
                else if (response != null && response.startsWith("page:")) openBedrockHomes(p, parsePage(response, currentPage));
                else if (response != null && response.startsWith("home:")) openBedrockHomeActions(p, response.substring(5));
            }));
    }

    private void openBedrockManage(Player player, int page) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        int pageCount = Math.max(1, (homes.size() + BEDROCK_PAGE_SIZE - 1) / BEDROCK_PAGE_SIZE);
        int currentPage = Math.max(0, Math.min(page, pageCount - 1));
        int from = currentPage * BEDROCK_PAGE_SIZE;
        int to = Math.min(from + BEDROCK_PAGE_SIZE, homes.size());
        List<GeyserUtils.Button> buttons = new ArrayList<>();
        for (int i = from; i < to; i++) buttons.add(new GeyserUtils.Button(homes.get(i).getName(), "home:" + homes.get(i).getName()));
        if (currentPage > 0) buttons.add(new GeyserUtils.Button("Previous page", "page:" + (currentPage - 1)));
        if (currentPage + 1 < pageCount) buttons.add(new GeyserUtils.Button("Next page", "page:" + (currentPage + 1)));
        buttons.add(new GeyserUtils.Button("Back to homes", "back"));
        GeyserUtils.sendForm(player, "homes-manage", "Manage homes", "Choose a home to edit.", buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("back".equals(response)) openBedrockHomes(p, 0);
                else if (response != null && response.startsWith("page:")) openBedrockManage(p, parsePage(response, currentPage));
                else if (response != null && response.startsWith("home:")) openBedrockHomeActions(p, response.substring(5));
            }));
    }

    private void openBedrockHomeActions(Player player, String homeName) {
        Home home = data(player).getHome(homeName);
        if (home == null) { MessageUtils.sendMessage(player, "home-not-found", Map.of("name", homeName)); openBedrockHomes(player, 0); return; }
        String details = home.getWorldName() + " " + (int) home.getX() + " " + (int) home.getY() + " " + (int) home.getZ();
        List<GeyserUtils.Button> buttons = List.of(
            new GeyserUtils.Button("Teleport", "teleport"), new GeyserUtils.Button("Rename", "rename"),
            new GeyserUtils.Button("Delete", "delete"), new GeyserUtils.Button("Back", "back"));
        GeyserUtils.sendForm(player, "home-actions-" + homeName, home.getName(), details, buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("teleport".equals(response)) teleportHome(p, homeName);
                else if ("rename".equals(response)) beginRename(p, homeName);
                else if ("delete".equals(response)) confirmDelete(p, homeName);
                else if ("back".equals(response)) openBedrockHomes(p, 0);
            }));
    }

    private void confirmDelete(Player player, String homeName) {
        List<GeyserUtils.Button> buttons = List.of(new GeyserUtils.Button("Confirm delete", "confirm"), new GeyserUtils.Button("Cancel", "cancel"));
        GeyserUtils.sendForm(player, "home-delete-" + homeName, "Delete home?", "Delete '" + homeName + "'? This cannot be undone.", buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("confirm".equals(response)) deleteHome(p, homeName);
                else openBedrockHomeActions(p, homeName);
            }));
    }

    public void beginCreate(Player player) {
        if (!player.hasPermission("galaxyhomes.sethome")) { MessageUtils.sendMessage(player, "no-permission"); return; }
        Location location = player.getLocation().clone();
        String defaultName = plugin.getConfigManager().getDefaultHomeName();
        if (data(player).getHome(defaultName) == null) { createHome(player, defaultName, location); return; }
        PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.CREATE, null, location));
        player.sendMessage(ChatColor.YELLOW + "Type the new home name in chat. Type 'cancel' to cancel.");
    }

    public void beginRename(Player player, String oldName) {
        if (!player.hasPermission("galaxyhomes.sethome")) { MessageUtils.sendMessage(player, "no-permission"); return; }
        PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.RENAME, oldName, null));
        player.sendMessage(ChatColor.YELLOW + "Type a new name for '" + oldName + "' in chat. Type 'cancel' to cancel.");
    }

    public void completePendingInput(Player player, PendingInput input, String newName) {
        if ("cancel".equalsIgnoreCase(newName.trim())) { player.sendMessage(ChatColor.GRAY + "Home action cancelled."); open(player, 0); return; }
        String name = newName.trim();
        if (!validHomeName(player, name)) return;
        if (input.mode() == InputMode.CREATE) createHome(player, name, input.createLocation());
        else {
            PlayerData playerData = data(player);
            if (playerData.getHome(name) != null && !name.equalsIgnoreCase(input.oldName())) { player.sendMessage(ChatColor.RED + "A home with that name already exists."); return; }
            if (!playerData.renameHome(input.oldName(), name)) { MessageUtils.sendMessage(player, "home-not-found", Map.of("name", input.oldName())); return; }
            playerData.setDirty(true);
            MessageUtils.sendMessage(player, "home-updated", Map.of("name", name));
            open(player, 0);
        }
    }

    public static PendingInput takePendingInput(UUID playerId) { return PENDING_INPUTS.remove(playerId); }

    private boolean validHomeName(Player player, String name) {
        if (name.isBlank() || name.length() > plugin.getConfigManager().getMaxNameLength()) {
            MessageUtils.sendMessage(player, "home-name-too-long", Map.of("max", String.valueOf(plugin.getConfigManager().getMaxNameLength()))); return false;
        }
        if (!plugin.getConfigManager().isAllowSpacesInName() && name.contains(" ")) { player.sendMessage(ChatColor.RED + "Spaces are not allowed in home names."); return false; }
        return true;
    }

    private void createHome(Player player, String name, Location location) {
        PlayerData playerData = data(player);
        if (playerData.getHome(name) != null) { player.sendMessage(ChatColor.RED + "A home with that name already exists."); open(player, 0); return; }
        int max = plugin.getConfigManager().getMaxHomes();
        if (max >= 0 && playerData.getHomeCount() >= max) { MessageUtils.sendMessage(player, "home-limit-reached", Map.of("limit", String.valueOf(max))); open(player, 0); return; }
        playerData.addHome(new Home(name, location));
        MessageUtils.sendMessage(player, "home-set", Map.of("name", name));
        open(player, 0);
    }

    public void deleteHome(Player player, String name) {
        if (!player.hasPermission("galaxyhomes.delhome")) { MessageUtils.sendMessage(player, "no-permission"); return; }
        if (data(player).removeHome(name)) MessageUtils.sendMessage(player, "home-deleted", Map.of("name", name));
        else MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name));
        if (GeyserUtils.isBedrockPlayer(player)) openBedrockHomes(player, 0); else openJavaManageHomes(player);
    }

    public void teleportHome(Player player, String name) {
        Home home = data(player).getHome(name);
        if (home == null) { MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name)); return; }
        Location location = home.toLocation();
        if (location == null) { player.sendMessage(ChatColor.RED + "That home's world is unavailable."); return; }
        MessageUtils.sendMessage(player, "home-teleporting", Map.of("name", name));
        plugin.getTeleportManager().startTeleport(player, location, "home", success -> {
            if (success) MessageUtils.sendMessage(player, "home-teleported", Map.of("name", name));
        });
    }

    private int parsePage(String response, int fallback) {
        try { return Integer.parseInt(response.substring("page:".length())); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
