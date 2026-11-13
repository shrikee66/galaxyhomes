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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InventoryListener implements Listener {
    private final GalaxyHomes plugin;
    private final HomesCommand homes;
    private final NamespacedKey homeKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey targetKey;

    public InventoryListener(GalaxyHomes plugin) {
        this.plugin = plugin;
        homes = new HomesCommand(plugin);
        homeKey = new NamespacedKey(plugin, "home_name");
        actionKey = new NamespacedKey(plugin, "gui_action");
        targetKey = new NamespacedKey(plugin, "tpa_target");
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) return;
        if (!title.equals(HomesCommand.HOMES_TITLE) && !title.equals(HomesCommand.MANAGE_TITLE) && !title.equals(HomesCommand.CONFIRM_TITLE) && !title.startsWith("Select online players")) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String name = meta.getPersistentDataContainer().get(homeKey, PersistentDataType.STRING);

        if (title.startsWith("Select online players")) {
            String rawId = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
            if (rawId == null) return;
            Player target;
            try { target = Bukkit.getPlayer(UUID.fromString(rawId)); } catch (IllegalArgumentException ex) { return; }
            if (target == null) { player.sendMessage(ChatColor.RED + "That player is no longer online."); return; }
            player.closeInventory();
            if (title.endsWith("TPAHere")) plugin.getTPAManager().sendTPAHereRequest(player, target);
            else plugin.getTPAManager().sendTPARequest(player, target);
            return;
        }

        if (HomesCommand.HOMES_TITLE.equals(title)) {
            if (action != null) {
                if (action.equals("new_home")) {
                    var data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                    String nameForHome = plugin.getConfigManager().getDefaultHomeName();
                    if (data.getHome(nameForHome) != null) {
                        int index = 2;
                        while (data.getHome(nameForHome + index) != null) index++;
                        nameForHome += index;
                    }
                    data.addHome(new Home(nameForHome, player.getLocation()));
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "Home '" + nameForHome + "' saved.");
                } else if (action.startsWith("page:")) {
                    homes.open(player, Integer.parseInt(action.substring(5)));
                }
                return;
            }
            if (name != null) {
                Home home = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getHome(name);
                if (home != null) homes.openManage(player, home);
            }
            return;
        }

        if (HomesCommand.MANAGE_TITLE.equals(title)) {
            if (action == null) return;
            if (action.equals("back")) { homes.open(player, 0); return; }
            if (name == null) return;
            Home home = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getHome(name);
            if (home == null) { homes.open(player, 0); return; }
            switch (action) {
                case "teleport" -> { player.closeInventory(); homes.teleport(player, home); }
                case "rename" -> {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "To rename this home, use: /renamehome " + home.getName() + " <new name>");
                }
                case "delete" -> homes.openDeleteConfirm(player, home);
            }
            return;
        }

        if (HomesCommand.CONFIRM_TITLE.equals(title)) {
            if (action == null) return;
            if (action.equals("cancel")) { player.closeInventory(); return; }
            if (action.startsWith("back_manage:") && name != null) {
                Home home = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getHome(name);
                if (home != null) homes.openManage(player, home);
                return;
            }
            if (action.equals("confirm_delete") && name != null) {
                var data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                if (data.removeHome(name)) player.sendMessage(ChatColor.GREEN + "Home '" + name + "' deleted.");
                homes.open(player, 0);
            }
        }
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title != null && (title.equals(HomesCommand.HOMES_TITLE) || title.equals(HomesCommand.MANAGE_TITLE) || title.equals(HomesCommand.CONFIRM_TITLE) || title.startsWith("Select online players"))) event.setCancelled(true);
    }

    public void openPlayerPicker(Player player, boolean here) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(target -> target.getUniqueId().equals(player.getUniqueId()));
        online.sort(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        if (online.isEmpty()) { player.sendMessage(ChatColor.RED + "No other players are online."); return; }
        int size = Math.min(54, Math.max(9, ((Math.min(online.size(), 54) + 8) / 9) * 9));
        var inventory = Bukkit.createInventory(null, size, here ? "Select online players - TPAHere" : "Select online players - TPA");
        for (Player target : online.subList(0, Math.min(online.size(), size))) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            var skull = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            skull.setOwningPlayer(target);
            skull.setDisplayName(ChatColor.YELLOW + target.getName());
            skull.setLore(List.of(ChatColor.GRAY + "Click to send request"));
            skull.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target.getUniqueId().toString());
            head.setItemMeta(skull);
            inventory.addItem(head);
        }
        player.openInventory(inventory);
    }
}
