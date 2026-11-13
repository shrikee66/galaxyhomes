package com.galaxyhomes.listeners;

import com.galaxyhomes.GalaxyHomes;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryListener implements Listener {
    private final GalaxyHomes plugin;

    public InventoryListener(GalaxyHomes p) {
        plugin = p;
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (title == null) return;

        if ("Homes".equalsIgnoreCase(title) || "Your Homes".equalsIgnoreCase(title)) {
            e.setCancelled(true);

            int slot = e.getRawSlot();
            if (slot < 0 || slot >= e.getView().getTopInventory().getSize()) return;

            if (slot == 45) {
                p.performCommand("sethome");
                return;
            }
            if (slot == 49) {
                p.sendMessage(ChatColor.GOLD + "Made by shrikee <3");
                return;
            }
            if (slot == 53) {
                p.performCommand("homes");
                return;
            }

            if (slot >= 45) return;
            List<Home> homes = new ArrayList<>(
                plugin.getPlayerDataManager()
                    .getPlayerData(p.getUniqueId())
                    .getHomes()
                    .values()
            );

            if (slot >= homes.size()) return;

            Home h = homes.get(slot);
            if (e.isRightClick()) {
                p.sendMessage("§eHome: §f" + h.getName() + " §7— use /delhome " + h.getName() + " to delete it.");
                return;
            }

            var loc = h.toLocation();
            if (loc == null) {
                p.sendMessage("§cThat home's world is unavailable.");
                return;
            }

            p.closeInventory();
            MessageUtils.sendMessage(p, "home-teleporting", Map.of("name", h.getName()));

            plugin.getTeleportManager().startTeleport(
                p, loc, "home",
                ok -> {
                    if (ok) {
                        MessageUtils.sendMessage(p, "home-teleported", Map.of("name", h.getName()));
                    }
                }
            );
            return;
        }

        if ("Select player for TPA".equalsIgnoreCase(title) || "Select player for TPAHere".equalsIgnoreCase(title)) {
            e.setCancelled(true);

            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            ItemStack item = e.getCurrentItem();
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            NamespacedKey targetKey = new NamespacedKey(plugin, "target_uuid");
            if (!meta.getPersistentDataContainer().has(targetKey, PersistentDataType.STRING)) return;

            String targetUuid = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
            if (targetUuid == null || targetUuid.isBlank()) return;

            Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
            if (target == null) {
                p.sendMessage("§cPlayer not found or offline.");
                p.closeInventory();
                return;
            }

            p.closeInventory();

            if ("Select player for TPAHere".equalsIgnoreCase(title)) {
                plugin.getTPAManager().sendTPAHereRequest(p, target);
            } else {
                plugin.getTPAManager().sendTPARequest(p, target);
            }
        }
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        String title = ChatColor.stripColor(e.getView().getTitle());
        if ("Homes".equalsIgnoreCase(title)
            || "Your Homes".equalsIgnoreCase(title)
            || "Select player for TPA".equalsIgnoreCase(title)
            || "Select player for TPAHere".equalsIgnoreCase(title)) {
            e.setCancelled(true);
        }
    }
}
