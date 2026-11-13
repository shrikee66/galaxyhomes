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
    private static final String HOMES_TITLE = "Homes";
    private static final String MANAGE_TITLE = "Manage homes";
    private static final String DETAIL_TITLE = "Manage home";
    private static final String DELETE_TITLE = "Delete home?";
    private static final int PAGE_SIZE = 45;
    private static final Map<UUID, PendingInput> PENDING_INPUTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> HOME_PAGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MANAGE_PAGES = new ConcurrentHashMap<>();

    public enum InputMode { CREATE, RENAME }
    public record PendingInput(InputMode mode, String oldName, Location location) {}

    public HomesCommand(GalaxyHomes plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!permission(sender, "galaxyhomes.homes")) return true;
        Player player = player(sender);
        if (player != null) open(player, 0);
        return true;
    }

    public void open(Player player, int page) {
        if (GeyserUtils.isBedrockPlayer(player)) {
            openBedrockHomes(player, page);
            return;
        }
        openJavaHomes(player, page);
    }

    private void openJavaHomes(Player player, int requestedPage) {
        List<Home> homes = homes(player);
        int pages = Math.max(1, (homes.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        HOME_PAGES.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(null, 54, HOMES_TITLE);
        NamespacedKey actionKey = key("home_menu_action");
        NamespacedKey nameKey = key("home_menu_name");
        int start = page * PAGE_SIZE;
        for (int i = start; i < Math.min(start + PAGE_SIZE, homes.size()); i++) {
            Home home = homes.get(i);
            inventory.setItem(i - start, menuItem(Material.RED_BED, ChatColor.YELLOW + home.getName(), List.of(
                ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(),
                ChatColor.GRAY + "X: " + (int) home.getX() + "  Y: " + (int) home.getY() + "  Z: " + (int) home.getZ(),
                ChatColor.GREEN + "Left-click to teleport",
                ChatColor.YELLOW + "Right-click to manage"
            ), "teleport", home.getName(), actionKey, nameKey));
        }
        if (homes.isEmpty()) inventory.setItem(22, menuItem(Material.BOOK, ChatColor.RED + "No homes yet",
            List.of(ChatColor.GRAY + "Use the green wool to create one"), "none", null, actionKey, nameKey));

        inventory.setItem(45, menuItem(Material.GREEN_WOOL, ChatColor.GREEN + "New home here",
            List.of(ChatColor.GRAY + "Create a home at your current location"), "new_home", null, actionKey, nameKey));
        inventory.setItem(49, menuItem(Material.NETHER_STAR, ChatColor.GOLD + "Made by shrikee <3",
            List.of(ChatColor.YELLOW + "Made by shrikee <3"), "creator_info", null, actionKey, nameKey));
        inventory.setItem(53, menuItem(Material.ORANGE_WOOL, ChatColor.GOLD + "Manage homes",
            List.of(ChatColor.GRAY + "Rename or delete your homes"), "manage", null, actionKey, nameKey));
        if (page > 0) inventory.setItem(47, menuItem(Material.ARROW, ChatColor.WHITE + "Previous page",
            List.of(), "prev", null, actionKey, nameKey));
        if (page + 1 < pages) inventory.setItem(51, menuItem(Material.ARROW, ChatColor.WHITE + "Next page",
            List.of(), "next", null, actionKey, nameKey));
        player.openInventory(inventory);
    }

    private void openManageHomes(Player player, int requestedPage) {
        List<Home> homes = homes(player);
        int pages = Math.max(1, (homes.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        MANAGE_PAGES.put(player.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(null, 54, MANAGE_TITLE);
        NamespacedKey actionKey = key("home_menu_action");
        NamespacedKey nameKey = key("home_menu_name");
        int start = page * PAGE_SIZE;
        for (int i = start; i < Math.min(start + PAGE_SIZE, homes.size()); i++) {
            Home home = homes.get(i);
            inventory.setItem(i - start, menuItem(Material.RED_BED, ChatColor.YELLOW + home.getName(),
                List.of(ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(), ChatColor.GREEN + "Click to manage"),
                "details", home.getName(), actionKey, nameKey));
        }
        if (homes.isEmpty()) inventory.setItem(22, menuItem(Material.BOOK, ChatColor.RED + "No homes yet", List.of(), "none", null, actionKey, nameKey));
        if (page > 0) inventory.setItem(45, menuItem(Material.ARROW, ChatColor.WHITE + "Previous page", List.of(), "manage_prev", null, actionKey, nameKey));
        inventory.setItem(49, menuItem(Material.ARROW, ChatColor.WHITE + "Back to homes", List.of(), "back", null, actionKey, nameKey));
        if (page + 1 < pages) inventory.setItem(53, menuItem(Material.ARROW, ChatColor.WHITE + "Next page", List.of(), "manage_next", null, actionKey, nameKey));
        player.openInventory(inventory);
    }

    /** Opens the actions menu for one selected home (used by right-click and manage list). */
    public void openHomeDetails(Player player, String name) {
        Home home = data(player).getHome(name);
        if (home == null) {
            MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name));
            openManageHomes(player, MANAGE_PAGES.getOrDefault(player.getUniqueId(), 0));
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, DETAIL_TITLE);
        NamespacedKey actionKey = key("home_menu_action");
        NamespacedKey nameKey = key("home_menu_name");
        inventory.setItem(4, menuItem(Material.WRITABLE_BOOK, ChatColor.AQUA + home.getName(), List.of(
            ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(),
            ChatColor.GRAY + "X: " + (int) home.getX() + "  Y: " + (int) home.getY() + "  Z: " + (int) home.getZ()
        ), "info", name, actionKey, nameKey));
        inventory.setItem(10, menuItem(Material.ENDER_PEARL, ChatColor.GREEN + "Teleport", List.of(), "teleport", name, actionKey, nameKey));
        inventory.setItem(13, menuItem(Material.NAME_TAG, ChatColor.YELLOW + "Rename", List.of("§7Enter the new name in chat"), "rename", name, actionKey, nameKey));
        inventory.setItem(16, menuItem(Material.REDSTONE, ChatColor.RED + "Delete", List.of("§7Confirm before deletion"), "delete", name, actionKey, nameKey));
        inventory.setItem(22, menuItem(Material.ARROW, ChatColor.WHITE + "Back", List.of(), "back_manage", null, actionKey, nameKey));
        player.openInventory(inventory);
    }

    private void confirmDelete(Player player, String name) {
        Inventory inventory = Bukkit.createInventory(null, 27, DELETE_TITLE);
        NamespacedKey actionKey = key("home_menu_action");
        NamespacedKey nameKey = key("home_menu_name");
        inventory.setItem(11, menuItem(Material.RED_WOOL, ChatColor.RED + "Confirm delete", List.of("§7Delete " + name), "confirm_delete", name, actionKey, nameKey));
        inventory.setItem(15, menuItem(Material.GREEN_WOOL, ChatColor.GREEN + "Cancel", List.of(), "cancel_delete", name, actionKey, nameKey));
        player.openInventory(inventory);
    }

    public boolean handleMenuClick(Player player, String title, ItemStack clicked, boolean rightClick) {
        if (!HOMES_TITLE.equalsIgnoreCase(title) && !MANAGE_TITLE.equalsIgnoreCase(title)
            && !DETAIL_TITLE.equalsIgnoreCase(title) && !DELETE_TITLE.equalsIgnoreCase(title)) return false;
        if (clicked == null || clicked.getType() == Material.AIR) return true;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return true;
        String action = meta.getPersistentDataContainer().get(key("home_menu_action"), PersistentDataType.STRING);
        String name = meta.getPersistentDataContainer().get(key("home_menu_name"), PersistentDataType.STRING);
        UUID uuid = player.getUniqueId();

        if (HOMES_TITLE.equalsIgnoreCase(title)) {
            if (rightClick && "teleport".equals(action) && name != null) {
                openHomeDetails(player, name);
                return true;
            }
            int page = HOME_PAGES.getOrDefault(uuid, 0);
            if ("new_home".equals(action)) beginCreate(player);
            else if ("manage".equals(action)) openManageHomes(player, 0);
            else if ("prev".equals(action)) openJavaHomes(player, page - 1);
            else if ("next".equals(action)) openJavaHomes(player, page + 1);
            else if ("teleport".equals(action) && name != null) teleportHome(player, name);
            return true;
        }
        if (MANAGE_TITLE.equalsIgnoreCase(title)) {
            int page = MANAGE_PAGES.getOrDefault(uuid, 0);
            if ("back".equals(action)) openJavaHomes(player, HOME_PAGES.getOrDefault(uuid, 0));
            else if ("manage_prev".equals(action)) openManageHomes(player, page - 1);
            else if ("manage_next".equals(action)) openManageHomes(player, page + 1);
            else if ("details".equals(action) && name != null) openHomeDetails(player, name);
            return true;
        }
        if (DETAIL_TITLE.equalsIgnoreCase(title)) {
            if ("back_manage".equals(action)) openManageHomes(player, MANAGE_PAGES.getOrDefault(uuid, 0));
            else if (name != null && "teleport".equals(action)) teleportHome(player, name);
            else if (name != null && "rename".equals(action)) beginRename(player, name);
            else if (name != null && "delete".equals(action)) confirmDelete(player, name);
            return true;
        }
        if (DELETE_TITLE.equalsIgnoreCase(title)) {
            if ("confirm_delete".equals(action) && name != null) deleteHome(player, name);
            else if ("cancel_delete".equals(action) && name != null) openHomeDetails(player, name);
            return true;
        }
        return true;
    }

    private void openBedrockHomes(Player player, int page) {
        List<Home> homes = homes(player);
        int max = plugin.getConfigManager().getMaxHomes();
        int pageCount = Math.max(1, (homes.size() + 7) / 8);
        int currentPage = Math.max(0, Math.min(page, pageCount - 1));
        int start = currentPage * 8;
        List<GeyserUtils.Button> buttons = new ArrayList<>();
        if (max < 0 || homes.size() < max) buttons.add(new GeyserUtils.Button("+ New home here", "new_home"));
        for (int i = start; i < Math.min(start + 8, homes.size()); i++) buttons.add(new GeyserUtils.Button(homes.get(i).getName(), "home:" + homes.get(i).getName()));
        if (currentPage > 0) buttons.add(new GeyserUtils.Button("Previous page", "page:" + (currentPage - 1)));
        if (currentPage + 1 < pageCount) buttons.add(new GeyserUtils.Button("Next page", "page:" + (currentPage + 1)));
        buttons.add(new GeyserUtils.Button("Manage homes", "manage"));
        GeyserUtils.sendForm(player, "homes-main", "Homes", homes.size() + " homes", buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("new_home".equals(response)) beginCreate(p);
                else if ("manage".equals(response)) openBedrockManage(p, 0);
                else if (response != null && response.startsWith("page:")) openBedrockHomes(p, parsePage(response, currentPage));
                else if (response != null && response.startsWith("home:")) openBedrockDetails(p, response.substring(5));
            }));
    }

    private void openBedrockManage(Player player, int page) {
        List<Home> homes = homes(player);
        int pages = Math.max(1, (homes.size() + 7) / 8);
        int currentPage = Math.max(0, Math.min(page, pages - 1));
        int start = currentPage * 8;
        List<GeyserUtils.Button> buttons = new ArrayList<>();
        for (int i = start; i < Math.min(start + 8, homes.size()); i++) buttons.add(new GeyserUtils.Button(homes.get(i).getName(), "home:" + homes.get(i).getName()));
        if (currentPage > 0) buttons.add(new GeyserUtils.Button("Previous page", "page:" + (currentPage - 1)));
        if (currentPage + 1 < pages) buttons.add(new GeyserUtils.Button("Next page", "page:" + (currentPage + 1)));
        buttons.add(new GeyserUtils.Button("Back to homes", "back"));
        GeyserUtils.sendForm(player, "homes-manage", "Manage homes", "Choose a home to edit.", buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("back".equals(response)) openBedrockHomes(p, 0);
                else if (response != null && response.startsWith("page:")) openBedrockManage(p, parsePage(response, currentPage));
                else if (response != null && response.startsWith("home:")) openBedrockDetails(p, response.substring(5));
            }));
    }

    private void openBedrockDetails(Player player, String name) {
        Home home = data(player).getHome(name);
        if (home == null) { MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name)); openBedrockHomes(player, 0); return; }
        List<GeyserUtils.Button> buttons = List.of(new GeyserUtils.Button("Teleport", "teleport"),
            new GeyserUtils.Button("Rename", "rename"), new GeyserUtils.Button("Delete", "delete"), new GeyserUtils.Button("Back", "back"));
        String details = home.getWorldName() + " " + (int) home.getX() + " " + (int) home.getY() + " " + (int) home.getZ();
        GeyserUtils.sendForm(player, "home-actions-" + name, home.getName(), details, buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("teleport".equals(response)) teleportHome(p, name);
                else if ("rename".equals(response)) beginRename(p, name);
                else if ("delete".equals(response)) confirmBedrockDelete(p, name);
                else if ("back".equals(response)) openBedrockHomes(p, 0);
            }));
    }

    private void confirmBedrockDelete(Player player, String name) {
        GeyserUtils.sendForm(player, "delete-" + name, "Delete home?", "Delete '" + name + "'? This cannot be undone.",
            List.of(new GeyserUtils.Button("Confirm delete", "confirm"), new GeyserUtils.Button("Cancel", "cancel")),
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("confirm".equals(response)) deleteHome(p, name);
                else openBedrockDetails(p, name);
            }));
    }

    public void beginCreate(Player player) {
        if (!player.hasPermission("galaxyhomes.sethome")) { MessageUtils.sendMessage(player, "no-permission"); return; }
        Location location = player.getLocation().clone();
        String defaultName = plugin.getConfigManager().getDefaultHomeName();
        if (data(player).getHome(defaultName) == null) { createHome(player, defaultName, location); return; }
        PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.CREATE, null, location));
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Type the new home name in chat. Type 'cancel' to cancel.");
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
        if (!validHomeName(player, name)) { PENDING_INPUTS.put(player.getUniqueId(), input); return; }
        PlayerData playerData = data(player);
        if (input.mode() == InputMode.CREATE) { createHome(player, name, input.location()); return; }
        if (playerData.getHome(name) != null && !name.equalsIgnoreCase(input.oldName())) {
            player.sendMessage(ChatColor.RED + "A home with that name already exists.");
            PENDING_INPUTS.put(player.getUniqueId(), input);
            return;
        }
        if (playerData.renameHome(input.oldName(), name)) {
            MessageUtils.sendMessage(player, "home-updated", Map.of("name", name));
            if (GeyserUtils.isBedrockPlayer(player)) openBedrockManage(player, 0); else openManageHomes(player, 0);
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
            PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.CREATE, null, location));
            player.sendMessage(ChatColor.YELLOW + "Type another home name in chat, or 'cancel'.");
            return;
        }
        int max = plugin.getConfigManager().getMaxHomes();
        if (max >= 0 && playerData.getHomeCount() >= max) {
            MessageUtils.sendMessage(player, "home-limit-reached", Map.of("limit", String.valueOf(max)));
            open(player, 0);
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
        if (GeyserUtils.isBedrockPlayer(player)) openBedrockHomes(player, 0); else openManageHomes(player, 0);
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

    private List<Home> homes(Player player) { return new ArrayList<>(data(player).getHomes().values()); }
    private NamespacedKey key(String value) { return new NamespacedKey(plugin, value); }

    private ItemStack menuItem(Material material, String displayName, List<String> lore, String action,
                               String homeName, NamespacedKey actionKey, NamespacedKey nameKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(displayName);
        if (!lore.isEmpty()) meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (homeName != null) meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, homeName);
        item.setItemMeta(meta);
        return item;
    }

    private int parsePage(String response, int fallback) {
        try { return Integer.parseInt(response.substring("page:".length())); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
