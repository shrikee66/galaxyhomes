package com.galaxyhomes.listeners;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.commands.HomesCommand;
import com.galaxyhomes.data.Home;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class InventoryListener implements Listener {
    private final GalaxyHomes plugin;
    private final HomesCommand homes;
    private final NamespacedKey targetKey;
    private final java.util.Map<UUID, Integer> managePage = new java.util.concurrent.ConcurrentHashMap<>();

    public InventoryListener(GalaxyHomes plugin) {
        this.plugin = plugin;
        this.homes = new HomesCommand(plugin);
        this.targetKey = new NamespacedKey(plugin, "tpa_target");
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) return;
        boolean playerPicker = title.startsWith("Select online players");
        if (!title.equals(HomesCommand.HOMES_TITLE) && !title.equals(HomesCommand.MANAGE_TITLE) && !title.equals(HomesCommand.CONFIRM_TITLE) && !playerPicker) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();

        if (playerPicker) {
            String rawId = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
            if (rawId == null) return;
            Player target;
            try { target = Bukkit.getPlayer(UUID.fromString(rawId)); } catch (IllegalArgumentException exception) { return; }
            if (target == null) { player.sendMessage(ChatColor.RED + "That player is no longer online."); return; }
            player.closeInventory();
            if (title.endsWith("TPAHere")) plugin.getTPAManager().sendTPAHereRequest(player, target);
            else plugin.getTPAManager().sendTPARequest(player, target);
            return;
        }

        String action = homes.getAction(meta);
        String name = homes.getHomeName(meta);
        var data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (HomesCommand.HOMES_TITLE.equals(title)) {
            if (action != null) {
                if (action.equals("new_home")) { homes.createHome(player); homes.open(player, 0); }
                else if (action.startsWith("page:")) homes.open(player, Integer.parseInt(action.substring(5)));
                else if (action.startsWith("manage_list:")) openManageList(player, Integer.parseInt(action.substring(12)));
                return;
            }
            if (name != null) {
                Home home = data.getHome(name);
                if (home != null) { player.closeInventory(); homes.teleport(player, home); }
            }
            return;
        }

        if (HomesCommand.MANAGE_TITLE.equals(title)) {
            if (action == null) return;
            if (action.equals("back")) { homes.open(player, 0); return; }
            if (action.equals("manage_prev")) { openManageList(player, Math.max(0, managePage.getOrDefault(player.getUniqueId(), 0) - 1)); return; }
            if (action.equals("manage_next")) { openManageList(player, managePage.getOrDefault(player.getUniqueId(), 0) + 1); return; }
            if (name == null) return;
            Home home = data.getHome(name);
            if (home == null) { homes.open(player, 0); return; }
            switch (action) {
                case "teleport" -> { player.closeInventory(); homes.teleport(player, home); }
                case "rename" -> { player.closeInventory(); homes.rename(player, home.getName(), ""); player.sendMessage(ChatColor.YELLOW + "Rename cancelled: use /renamehome <old name> <new name>."); }
                case "delete" -> homes.openDeleteConfirm(player, home);
            }
            return;
        }

        if (HomesCommand.CONFIRM_TITLE.equals(title)) {
            if (action == null) return;
            if (action.equals("cancel")) { player.closeInventory(); return; }
            if (name == null) return;
            Home home = data.getHome(name);
            if (home == null) { homes.open(player, 0); return; }
            if (action.equals("back_manage")) { homes.openManage(player, home); return; }
            if (action.equals("confirm_delete")) {
                if (data.removeHome(name)) player.sendMessage(ChatColor.GREEN + "Home '" + name + "' deleted.");
                homes.open(player, 0);
            }
        }
    }

    private void openManageList(Player player, int page) {
        List<Home> list = new ArrayList<>(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getHomes().values());
        int start = Math.max(0, page) * 45;
        managePage.put(player.getUniqueId(), Math.max(0, page));
        Inventory inventory = Bukkit.createInventory(null, 54, HomesCommand.MANAGE_TITLE);
        for (int i = start; i < Math.min(start + 45, list.size()); i++) {
            Home home = list.get(i);
            ItemStack item = new ItemStack(Material.RED_BED);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + home.getName());
            meta.setLore(List.of(ChatColor.GRAY + "Click to manage"));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "home_name"), PersistentDataType.STRING, home.getName());
            item.setItemMeta(meta);
            inventory.setItem(i - start, item);
        }
        inventory.setItem(49, namedAction(Material.ARROW, ChatColor.GRAY + "Back", "back"));
        if (start > 0) inventory.setItem(45, namedAction(Material.ARROW, ChatColor.GRAY + "Previous", "manage_prev"));
        if (start + 45 < list.size()) inventory.setItem(53, namedAction(Material.ARROW, ChatColor.GRAY + "Next", "manage_next"));
        player.openInventory(inventory);
    }

    private ItemStack namedAction(Material material, String display, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(display);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gui_action"), PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title != null && (title.equals(HomesCommand.HOMES_TITLE) || title.equals(HomesCommand.MANAGE_TITLE) || title.equals(HomesCommand.CONFIRM_TITLE) || title.startsWith("Select online players"))) event.setCancelled(true);
    }

    public void openPlayerPicker(Player player, boolean here) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(target -> target.getUniqueId().equals(player.getUniqueId()));
        online.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        if (online.isEmpty()) { player.sendMessage(ChatColor.RED + "No other players are online."); return; }
        int size = Math.min(54, Math.max(9, ((Math.min(online.size(), 54) + 8) / 9) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, here ? "Select online players - TPAHere" : "Select online players - TPA");
        for (Player target : online.subList(0, Math.min(online.size(), size))) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.YELLOW + target.getName());
            meta.setLore(List.of(ChatColor.GRAY + "Click to send request"));
            meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target.getUniqueId().toString());
            head.setItemMeta(meta);
            inventory.addItem(head);
        }
        player.openInventory(inventory);
    }
}
