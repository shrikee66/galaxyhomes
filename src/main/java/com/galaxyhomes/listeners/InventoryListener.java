package com.galaxyhomes.listeners;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.commands.HomesCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class InventoryListener implements Listener {
    private final GalaxyHomes plugin;

    public InventoryListener(GalaxyHomes plugin) { this.plugin = plugin; }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) return;

        if ("Homes".equalsIgnoreCase(title) && event.isRightClick()
            && event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
                String action = item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "home_menu_action"), PersistentDataType.STRING);
                String homeName = item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "home_menu_name"), PersistentDataType.STRING);
                if ("teleport".equals(action) && homeName != null) {
                    event.setCancelled(true);
                    new HomesCommand(plugin).openHomeDetails(player, homeName);
                    return;
                }
            }
        }

        if (new HomesCommand(plugin).handleMenuClick(player, title, event.getCurrentItem(), event.isRightClick())) {
            event.setCancelled(true);
            return;
        }

        if ("Select player for TPA".equalsIgnoreCase(title) || "Select player for TPAHere".equalsIgnoreCase(title)) {
            event.setCancelled(true);
            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            String targetId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "target_uuid"), PersistentDataType.STRING);
            if (targetId == null || targetId.isBlank()) return;
            Player target;
            try { target = Bukkit.getPlayer(UUID.fromString(targetId)); }
            catch (IllegalArgumentException ex) { return; }
            if (target == null) { player.sendMessage("§cPlayer not found or offline."); player.closeInventory(); return; }
            player.closeInventory();
            if ("Select player for TPAHere".equalsIgnoreCase(title)) plugin.getTPAManager().sendTPAHereRequest(player, target);
            else plugin.getTPAManager().sendTPARequest(player, target);
        }
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title != null && ("Homes".equalsIgnoreCase(title) || "Manage homes".equalsIgnoreCase(title)
            || "Manage home".equalsIgnoreCase(title) || "Delete home?".equalsIgnoreCase(title)
            || "Select player for TPA".equalsIgnoreCase(title) || "Select player for TPAHere".equalsIgnoreCase(title))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void chatInput(AsyncPlayerChatEvent event) {
        HomesCommand.PendingInput input = HomesCommand.takePendingInput(event.getPlayer().getUniqueId());
        if (input == null) return;
        event.setCancelled(true);
        String text = event.getMessage();
        Bukkit.getScheduler().runTask(plugin,
            () -> new HomesCommand(plugin).completePendingInput(event.getPlayer(), input, text));
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        HomesCommand.takePendingInput(event.getPlayer().getUniqueId());
    }
}
