package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.data.PlayerData;
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
    private static final String HOMES_TITLE = "Homes";
    private static final String MANAGE_TITLE = "Manage homes";
    private static final String ACTION_TITLE = "Home actions";
    private static final String DELETE_TITLE = "Confirm home delete";
    private static final int HOMES_PER_PAGE = 45;
    private static final Map<UUID, PendingInput> PENDING_INPUTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PAGES = new ConcurrentHashMap<>();

    public enum InputMode { CREATE, RENAME }
    public record PendingInput(InputMode mode, String oldName, Location location) {}

    public HomesCommand(GalaxyHomes p) { super(p); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!permission(sender, "galaxyhomes.homes")) return true;
        Player player = player(sender);
        if (player != null) open(player, 0);
        return true;
    }

    public void open(Player player, int requestedPage) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        int pageCount = Math.max(1, (homes.size() + HOMES_PER_PAGE - 1) / HOMES_PER_PAGE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        PAGES.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, HOMES_TITLE);
        NamespacedKey actionKey = key("menu_action");
        NamespacedKey homeKey = key("home_name");
        int start = page * HOMES_PER_PAGE;
        for (int i = start; i < Math.min(start + HOMES_PER_PAGE, homes.size()); i++) {
            Home home = homes.get(i);
            inv.setItem(i - start, item(Material.RED_BED, ChatColor.YELLOW + home.getName(), List.of(
                ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(),
                ChatColor.GRAY + "X: " + (int) home.getX() + "  Y: " + (int) home.getY() + "  Z: " + (int) home.getZ(),
                ChatColor.GREEN + "Click to teleport"
            ), "teleport_home", home.getName(), actionKey, homeKey));
        }
        if (homes.isEmpty()) inv.setItem(22, item(Material.BOOK, ChatColor.RED + "No homes yet",
            List.of(ChatColor.GRAY + "Use the green wool to create one"), "none", null, actionKey, homeKey));

        // Java-only footer controls requested by the owner.
        inv.setItem(45, item(Material.GREEN_WOOL, ChatColor.GREEN + "New home here",
            List.of(ChatColor.GRAY + "Create a home at your current location"), "new_home", null, actionKey, homeKey));
        inv.setItem(49, item(Material.NETHER_STAR, ChatColor.GOLD + "Made by shrikee <3",
            List.of(ChatColor.YELLOW + "Made by shrikee <3"), "creator_info", null, actionKey, homeKey));
        inv.setItem(53, item(Material.ORANGE_WOOL, ChatColor.GOLD + "Manage homes",
            List.of(ChatColor.GRAY + "Rename or delete your homes"), "manage_homes", null, actionKey, homeKey));
        if (page > 0) inv.setItem(47, item(Material.ARROW, ChatColor.WHITE + "Previous page", List.of(), "prev_page", null, actionKey, homeKey));
        if (page + 1 < pageCount) inv.setItem(51, item(Material.ARROW, ChatColor.WHITE + "Next page", List.of(), "next_page", null, actionKey, homeKey));
        player.openInventory(inv);
    }

    public void openManageHomesMenu(Player player) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        Inventory inv = Bukkit.createInventory(null, 54, MANAGE_TITLE);
        NamespacedKey actionKey = key("menu_action");
        NamespacedKey homeKey = key("home_name");
        int count = Math.min(45, homes.size());
        for (int i = 0; i < count; i++) {
            Home home = homes.get(i);
            inv.setItem(i, item(Material.RED_BED, ChatColor.YELLOW + home.getName(),
                List.of(ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(), ChatColor.GREEN + "Click to manage"),
                "open_home", home.getName(), actionKey, homeKey));
        }
        if (homes.isEmpty()) inv.setItem(22, item(Material.BOOK, ChatColor.RED + "No homes yet", List.of(), "none", null, actionKey, homeKey));
        inv.setItem(49, item(Material.ARROW, ChatColor.WHITE + "Back to homes", List.of(), "back_homes", null, actionKey, homeKey));
        player.openInventory(inv);
    }

    public void openHomeDetailMenu(Player player, String homeName) {
        Home home = data(player).getHome(homeName);
        if (home == null) {
            MessageUtils.sendMessage(player, "home-not-found", Map.of("name", homeName));
            openManageHomesMenu(player);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, ACTION_TITLE);
        NamespacedKey actionKey = key("menu_action");
        NamespacedKey homeKey = key("home_name");
        inv.setItem(4, item(Material.WRITABLE_BOOK, ChatColor.AQUA + home.getName(), List.of(
            ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(),
            ChatColor.GRAY + "X: " + (int) home.getX() + "  Y: " + (int) home.getY() + "  Z: " + (int) home.getZ()
        ), "info", home.getName(), actionKey, homeKey));
        inv.setItem(10, item(Material.ENDER_PEARL, ChatColor.GREEN + "Teleport", List.of(), "teleport_home", home.getName(), actionKey, homeKey));
        inv.setItem(13, item(Material.NAME_TAG, ChatColor.YELLOW + "Rename", List.of("§7Type the new name in chat"), "rename_home", home.getName(), actionKey, homeKey));
        inv.setItem(16, item(Material.REDSTONE, ChatColor.RED + "Delete", List.of("§7Confirm before deletion"), "delete_home", home.getName(), actionKey, homeKey));
        inv.setItem(22, item(Material.ARROW, ChatColor.WHITE + "Back", List.of(), "back_manage", null, actionKey, homeKey));
        player.openInventory(inv);
    }

    private void confirmDelete(Player player, String name) {
        Inventory inv = Bukkit.createInventory(null, 27, DELETE_TITLE);
        NamespacedKey actionKey = key("menu_action");
        NamespacedKey homeKey = key("home_name");
        inv.setItem(11, item(Material.RED_WOOL, ChatColor.RED + "Confirm delete", List.of("§7Delete " + name), "confirm_delete", name, actionKey, homeKey));
        inv.setItem(15, item(Material.GREEN_WOOL, ChatColor.GREEN + "Cancel", List.of(), "cancel_delete", name, actionKey, homeKey));
        player.openInventory(inv);
    }

    /** Called by the inventory listener; true means this is one of our menus. */
    public boolean handleMenuClick(Player player, String title, ItemStack clicked) {
        if (!HOMES_TITLE.equalsIgnoreCase(title) && !MANAGE_TITLE.equalsIgnoreCase(title)
            && !ACTION_TITLE.equalsIgnoreCase(title) && !DELETE_TITLE.equalsIgnoreCase(title)) return false;
        if (clicked == null || clicked.getType() == Material.AIR) return true;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return true;
        String action = meta.getPersistentDataContainer().get(key("menu_action"), PersistentDataType.STRING);
        String homeName = meta.getPersistentDataContainer().get(key("home_name"), PersistentDataType.STRING);
        UUID uuid = player.getUniqueId();

        if (HOMES_TITLE.equalsIgnoreCase(title)) {
            int page = PAGES.getOrDefault(uuid, 0);
            if ("new_home".equals(action)) beginCreate(player);
            else if ("manage_homes".equals(action)) openManageHomesMenu(player);
            else if ("prev_page".equals(action)) open(player, page - 1);
            else if ("next_page".equals(action)) open(player, page + 1);
            else if ("teleport_home".equals(action) && homeName != null) teleportHome(player, homeName);
            return true;
        }
        if (MANAGE_TITLE.equalsIgnoreCase(title)) {
            if ("back_homes".equals(action)) open(player, 0);
            else if ("open_home".equals(action) && homeName != null) openHomeDetailMenu(player, homeName);
            return true;
        }
        if (ACTION_TITLE.equalsIgnoreCase(title)) {
            if (homeName == null && !"back_manage".equals(action)) return true;
            if ("teleport_home".equals(action)) teleportHome(player, homeName);
            else if ("rename_home".equals(action)) beginRename(player, homeName);
            else if ("delete_home".equals(action)) confirmDelete(player, homeName);
            else if ("back_manage".equals(action)) openManageHomesMenu(player);
            return true;
        }
        if (DELETE_TITLE.equalsIgnoreCase(title)) {
            if ("confirm_delete".equals(action) && homeName != null) deleteHome(player, homeName);
            else if ("cancel_delete".equals(action)) openHomeDetailMenu(player, homeName);
            return true;
        }
        return true;
    }

    public void beginCreate(Player player) {
        if (!player.hasPermission("galaxyhomes.sethome")) { MessageUtils.sendMessage(player, "no-permission"); return; }
        Location location = player.getLocation().clone();
        String defaultName = plugin.getConfigManager().getDefaultHomeName();
        if (data(player).getHome(defaultName) == null) {
            createHome(player, defaultName, location);
            return;
        }
        PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.CREATE, null, location));
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Type a name for this home in chat. Type 'cancel' to cancel.");
    }

    private void beginRename(Player player, String oldName) {
        if (!player.hasPermission("galaxyhomes.sethome")) { MessageUtils.sendMessage(player, "no-permission"); return; }
        PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.RENAME, oldName, null));
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Type a new name for '" + oldName + "' in chat. Type 'cancel' to cancel.");
    }

    public void completePendingInput(Player player, PendingInput input, String proposedName) {
        String name = proposedName.trim();
        if ("cancel".equalsIgnoreCase(name)) { player.sendMessage(ChatColor.GRAY + "Home action cancelled."); open(player, 0); return; }
        if (!validHomeName(player, name)) return;
        PlayerData playerData = data(player);
        if (input.mode() == InputMode.CREATE) {
            createHome(player, name, input.location());
            return;
        }
        if (playerData.getHome(name) != null && !name.equalsIgnoreCase(input.oldName())) {
            player.sendMessage(ChatColor.RED + "A home with that name already exists.");
            beginRename(player, input.oldName());
            return;
        }
        if (playerData.renameHome(input.oldName(), name)) {
            playerData.setDirty(true);
            MessageUtils.sendMessage(player, "home-updated", Map.of("name", name));
            openManageHomesMenu(player);
        } else MessageUtils.sendMessage(player, "home-not-found", Map.of("name", input.oldName()));
    }

    public static PendingInput takePendingInput(UUID playerId) { return PENDING_INPUTS.remove(playerId); }

    private boolean validHomeName(Player player, String name) {
        int max = plugin.getConfigManager().getMaxNameLength();
        if (name.isBlank() || name.length() > max) {
            MessageUtils.sendMessage(player, "home-name-too-long", Map.of("max", String.valueOf(max)));
            return false;
        }
        if (!plugin.getConfigManager().isAllowSpacesInName() && name.contains(" ")) {
            player.sendMessage(ChatColor.RED + "Spaces are not allowed in home names.");
            return false;
        }
        return true;
    }

    private void createHome(Player player, String name, Location location) {
        PlayerData playerData = data(player);
        if (playerData.getHome(name) != null) {
            player.sendMessage(ChatColor.RED + "A home with that name already exists.");
            beginCreate(player);
            return;
        }
        int max = plugin.getConfigManager().getMaxHomes();
        if (max >= 0 && playerData.getHomeCount() >= max) {
            MessageUtils.sendMessage(player, "home-limit-reached", Map.of("limit", String.valueOf(max)));
            open(player, PAGES.getOrDefault(player.getUniqueId(), 0));
            return;
        }
        playerData.addHome(new Home(name, location));
        MessageUtils.sendMessage(player, "home-set", Map.of("name", name));
        open(player, 0);
    }

    private void deleteHome(Player player, String name) {
        if (!player.hasPermission("galaxyhomes.delhome")) { MessageUtils.sendMessage(player, "no-permission"); return; }
        if (data(player).removeHome(name)) MessageUtils.sendMessage(player, "home-deleted", Map.of("name", name));
        else MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name));
        openManageHomesMenu(player);
    }

    private void teleportHome(Player player, String name) {
        Home home = data(player).getHome(name);
        if (home == null) { MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name)); return; }
        Location location = home.toLocation();
        if (location == null) { player.sendMessage(ChatColor.RED + "That home's world is unavailable."); return; }
        player.closeInventory();
        MessageUtils.sendMessage(player, "home-teleporting", Map.of("name", name));
        plugin.getTeleportManager().startTeleport(player, location, "home",
            success -> { if (success) MessageUtils.sendMessage(player, "home-teleported", Map.of("name", name)); });
    }

    private ItemStack item(Material material, String displayName, List<String> lore, String action,
                           String homeName, NamespacedKey actionKey, NamespacedKey homeKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(displayName);
        if (!lore.isEmpty()) meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (homeName != null) meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, homeName);
        item.setItemMeta(meta);
        return item;
    }

    private NamespacedKey key(String value) { return new NamespacedKey(plugin, value); }
}
