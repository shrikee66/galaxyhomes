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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomesCommand extends BaseCommand {
    private static final int BEDROCK_PAGE_SIZE = 8;
    private static final Map<UUID, PendingInput> PENDING_INPUTS = new ConcurrentHashMap<>();

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
        if (GeyserUtils.isBedrockPlayer(player)) {
            openBedrockHomes(player, page);
        } else {
            openJavaHomes(player, page);
        }
    }

    private void openJavaHomes(Player player, int page) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        if (homes.isEmpty()) {
            MessageUtils.sendMessage(player, "home-list-empty");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.color("&8&lYour Homes"));
        int start = Math.max(0, page) * 45;
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
            item.setItemMeta(meta);
            inv.setItem(i - start, item);
        }
        if (page > 0) inv.setItem(45, new ItemStack(Material.ARROW));
        if (start + 45 < homes.size()) inv.setItem(53, new ItemStack(Material.ARROW));
        player.openInventory(inv);
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
        if (plugin.getConfigManager().getMaxHomes() < 0 || homes.size() < maxHomes) {
            buttons.add(new GeyserUtils.Button("+ New home here", "new_home", "PATH", "textures/items/bed_green"));
        }
        for (int i = from; i < to; i++) {
            Home home = homes.get(i);
            buttons.add(new GeyserUtils.Button(home.getName(), "home:" + home.getName(), "PATH", "textures/items/bed_red"));
        }
        if (currentPage > 0) buttons.add(new GeyserUtils.Button("Previous page", "page:" + (currentPage - 1), "PATH", "textures/items/arrow"));
        if (currentPage + 1 < pageCount) buttons.add(new GeyserUtils.Button("Next page", "page:" + (currentPage + 1), "PATH", "textures/items/arrow"));
        buttons.add(new GeyserUtils.Button("Manage homes", "manage", "PATH", "textures/items/book_written"));

        GeyserUtils.sendForm(player, "homes-main", "Homes", count, buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("new_home".equals(response)) {
                    beginCreate(p);
                } else if ("manage".equals(response)) {
                    openBedrockManage(p, 0);
                } else if (response != null && response.startsWith("page:")) {
                    openBedrockHomes(p, parsePage(response, currentPage));
                } else if (response != null && response.startsWith("home:")) {
                    openBedrockHomeActions(p, response.substring("home:".length()));
                }
            }));
    }

    private void openBedrockManage(Player player, int page) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        int pageCount = Math.max(1, (homes.size() + BEDROCK_PAGE_SIZE - 1) / BEDROCK_PAGE_SIZE);
        int currentPage = Math.max(0, Math.min(page, pageCount - 1));
        int from = currentPage * BEDROCK_PAGE_SIZE;
        int to = Math.min(from + BEDROCK_PAGE_SIZE, homes.size());
        List<GeyserUtils.Button> buttons = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Home home = homes.get(i);
            buttons.add(new GeyserUtils.Button(home.getName(), "home:" + home.getName(), "PATH", "textures/items/bed_red"));
        }
        if (currentPage > 0) buttons.add(new GeyserUtils.Button("Previous page", "page:" + (currentPage - 1), "PATH", "textures/items/arrow"));
        if (currentPage + 1 < pageCount) buttons.add(new GeyserUtils.Button("Next page", "page:" + (currentPage + 1), "PATH", "textures/items/arrow"));
        buttons.add(new GeyserUtils.Button("Back to homes", "back", "PATH", "textures/items/arrow"));

        GeyserUtils.sendForm(player, "homes-manage", "Manage homes", "Choose a home to edit.", buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("back".equals(response)) openBedrockHomes(p, 0);
                else if (response != null && response.startsWith("page:")) openBedrockManage(p, parsePage(response, currentPage));
                else if (response != null && response.startsWith("home:")) openBedrockHomeActions(p, response.substring(5));
            }));
    }

    private void openBedrockHomeActions(Player player, String homeName) {
        Home home = data(player).getHome(homeName);
        if (home == null) {
            MessageUtils.sendMessage(player, "home-not-found", Map.of("name", homeName));
            openBedrockHomes(player, 0);
            return;
        }
        String details = home.getWorldName() + " " + (int) home.getX() + " " + (int) home.getY() + " " + (int) home.getZ();
        List<GeyserUtils.Button> buttons = List.of(
            new GeyserUtils.Button("Teleport", "teleport", "PATH", "textures/items/ender_pearl"),
            new GeyserUtils.Button("Rename", "rename", "PATH", "textures/items/name_tag"),
            new GeyserUtils.Button("Delete", "delete", "PATH", "textures/items/lava_bucket"),
            new GeyserUtils.Button("Back", "back", "PATH", "textures/items/arrow")
        );
        GeyserUtils.sendForm(player, "home-actions-" + homeName, home.getName(), details, buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("teleport".equals(response)) teleportHome(p, homeName);
                else if ("rename".equals(response)) beginRename(p, homeName);
                else if ("delete".equals(response)) confirmDelete(p, homeName);
                else if ("back".equals(response)) openBedrockHomes(p, 0);
            }));
    }

    private void confirmDelete(Player player, String homeName) {
        List<GeyserUtils.Button> buttons = List.of(
            new GeyserUtils.Button("Confirm delete", "confirm", "PATH", "textures/items/lava_bucket"),
            new GeyserUtils.Button("Cancel", "cancel", "PATH", "textures/items/arrow")
        );
        GeyserUtils.sendForm(player, "home-delete-" + homeName, "Delete home?", "Delete '" + homeName + "'? This cannot be undone.", buttons,
            (p, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if ("confirm".equals(response)) deleteHome(p, homeName);
                else openBedrockHomeActions(p, homeName);
            }));
    }

    private void beginCreate(Player player) {
        if (!player.hasPermission("galaxyhomes.sethome")) {
            MessageUtils.sendMessage(player, "no-permission");
            return;
        }
        Location location = player.getLocation().clone();
        String defaultName = plugin.getConfigManager().getDefaultHomeName();
        PlayerData playerData = data(player);
        if (playerData.getHome(defaultName) == null) {
            createHome(player, defaultName, location);
            return;
        }
        PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.CREATE, null, location));
        player.sendMessage(ChatColor.YELLOW + "Type the new home name in chat. Type 'cancel' to cancel.");
    }

    private void beginRename(Player player, String oldName) {
        if (!player.hasPermission("galaxyhomes.sethome")) {
            MessageUtils.sendMessage(player, "no-permission");
            return;
        }
        PENDING_INPUTS.put(player.getUniqueId(), new PendingInput(InputMode.RENAME, oldName, null));
        player.sendMessage(ChatColor.YELLOW + "Type a new name for '" + oldName + "' in chat. Type 'cancel' to cancel.");
    }

    public void completePendingInput(Player player, PendingInput input, String newName) {
        if ("cancel".equalsIgnoreCase(newName.trim())) {
            player.sendMessage(ChatColor.GRAY + "Home action cancelled.");
            open(player, 0);
            return;
        }
        String name = newName.trim();
        if (!validHomeName(player, name)) return;
        if (input.mode() == InputMode.CREATE) {
            createHome(player, name, input.createLocation());
        } else {
            PlayerData playerData = data(player);
            if (playerData.getHome(name) != null && !name.equalsIgnoreCase(input.oldName())) {
                player.sendMessage(ChatColor.RED + "A home with that name already exists.");
                return;
            }
            if (!playerData.renameHome(input.oldName(), name)) {
                MessageUtils.sendMessage(player, "home-not-found", Map.of("name", input.oldName()));
                return;
            }
            playerData.setDirty(true);
            MessageUtils.sendMessage(player, "home-updated", Map.of("name", name));
            open(player, 0);
        }
    }

    public static PendingInput takePendingInput(UUID playerId) {
        return PENDING_INPUTS.remove(playerId);
    }

    private boolean validHomeName(Player player, String name) {
        if (name.isBlank() || name.length() > plugin.getConfigManager().getMaxNameLength()) {
            MessageUtils.sendMessage(player, "home-name-too-long", Map.of("max", String.valueOf(plugin.getConfigManager().getMaxNameLength())));
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
            open(player, 0);
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
        if (!player.hasPermission("galaxyhomes.delhome")) {
            MessageUtils.sendMessage(player, "no-permission");
            return;
        }
        if (data(player).removeHome(name)) MessageUtils.sendMessage(player, "home-deleted", Map.of("name", name));
        else MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name));
        openBedrockHomes(player, 0);
    }

    private void teleportHome(Player player, String name) {
        Home home = data(player).getHome(name);
        if (home == null) {
            MessageUtils.sendMessage(player, "home-not-found", Map.of("name", name));
            return;
        }
        Location location = home.toLocation();
        if (location == null) {
            player.sendMessage(ChatColor.RED + "That home's world is unavailable.");
            return;
        }
        MessageUtils.sendMessage(player, "home-teleporting", Map.of("name", name));
        plugin.getTeleportManager().startTeleport(player, location, "home",
            success -> { if (success) MessageUtils.sendMessage(player, "home-teleported", Map.of("name", name)); });
    }

    private int parsePage(String response, int fallback) {
        try { return Integer.parseInt(response.substring("page:".length())); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
