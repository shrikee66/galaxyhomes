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

    public InventoryListener(GalaxyHomes p) { plugin = p; }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (title == null) return;

        HomesCommand homesCommand = new HomesCommand(plugin);
        if (homesCommand.handleJavaGuiClick(p, title, e.getCurrentItem(), e.getRawSlot())) {
            e.setCancelled(true);
            return;
        }

        if ("Your Homes".equalsIgnoreCase(title)) {
            e.setCancelled(true);
            if (e.getRawSlot() < 0 || e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
            int slot = e.getRawSlot();
            List<Home> homes = new ArrayList<>(plugin.getPlayerDataManager().getPlayerData(p.getUniqueId()).getHomes().values());
            int start = Math.max(0, slotPage(e.getView().getTopInventory()) * 45);
            int index = start + slot;
            if (index >= homes.size() || slot >= 45) return;
            Home home = homes.get(index);
            var location = home.toLocation();
            if (location == null) { p.sendMessage("§cThat home's world is unavailable."); return; }
            p.closeInventory();
            MessageUtils.sendMessage(p, "home-teleporting", Map.of("name", home.getName()));
            plugin.getTeleportManager().startTeleport(p, location, "home",
                ok -> { if (ok) MessageUtils.sendMessage(p, "home-teleported", Map.of("name", home.getName())); });
            return;
        }

        if ("Select player for TPA".equalsIgnoreCase(title) || "Select player for TPAHere".equalsIgnoreCase(title)) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            NamespacedKey targetKey = new NamespacedKey(plugin, "target_uuid");
            String targetId = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
            if (targetId == null || targetId.isBlank()) return;
            Player target;
            try { target = Bukkit.getPlayer(UUID.fromString(targetId)); }
            catch (IllegalArgumentException ex) { return; }
            if (target == null) { p.sendMessage("§cPlayer not found or offline."); p.closeInventory(); return; }
            p.closeInventory();
            if ("Select player for TPAHere".equalsIgnoreCase(title)) plugin.getTPAManager().sendTPAHereRequest(p, target);
            else plugin.getTPAManager().sendTPARequest(p, target);
        }
    }

    private int slotPage(org.bukkit.inventory.Inventory inv) {
        ItemStack first = inv.getItem(0);
        if (first == null || !first.hasItemMeta()) return 0;
        String page = first.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "homes_page"), PersistentDataType.INTEGER) == null
            ? null : String.valueOf(first.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "homes_page"), PersistentDataType.INTEGER));
        try { return page == null ? 0 : Integer.parseInt(page); } catch (NumberFormatException ignored) { return 0; }
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
