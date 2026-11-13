package com.galaxyhomes.listeners;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.commands.HomesCommand;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.MessageUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryListener implements Listener {
    private final GalaxyHomes plugin;

    public InventoryListener(GalaxyHomes plugin) { this.plugin = plugin; }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (title == null) return;

        if (new HomesCommand(plugin).handleJavaGuiClick(player, title, e.getCurrentItem())) {
            e.setCancelled(true);
            return;
        }

        if ("Your Homes".equalsIgnoreCase(title)) {
            e.setCancelled(true);
            if (e.getRawSlot() < 0 || e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= 45) return;
            List<Home> homes = new ArrayList<>(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getHomes().values());
            if (slot >= homes.size()) return;
            Home home = homes.get(slot);
            var location = home.toLocation();
            if (location == null) { player.sendMessage("§cThat home's world is unavailable."); return; }
            player.closeInventory();
            MessageUtils.sendMessage(player, "home-teleporting", Map.of("name", home.getName()));
            plugin.getTeleportManager().startTeleport(player, location, "home",
                ok -> { if (ok) MessageUtils.sendMessage(player, "home-teleported", Map.of("name", home.getName())); });
            return;
        }

        if ("Select player for TPA".equalsIgnoreCase(title) || "Select player for TPAHere".equalsIgnoreCase(title)) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
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
    public void drag(InventoryDragEvent e) {
        String title = ChatColor.stripColor(e.getView().getTitle());
        if ("Your Homes".equalsIgnoreCase(title) || "Manage homes".equalsIgnoreCase(title)
            || "Home actions".equalsIgnoreCase(title) || "Confirm home delete".equalsIgnoreCase(title)
            || "Select player for TPA".equalsIgnoreCase(title) || "Select player for TPAHere".equalsIgnoreCase(title)) e.setCancelled(true);
    }

    @EventHandler
    public void chatInput(AsyncPlayerChatEvent e) {
        HomesCommand.PendingInput input = HomesCommand.takePendingInput(e.getPlayer().getUniqueId());
        if (input == null) return;
        e.setCancelled(true);
        String text = e.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> new HomesCommand(plugin).completePendingInput(e.getPlayer(), input, text));
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) { HomesCommand.takePendingInput(e.getPlayer().getUniqueId()); }
}
