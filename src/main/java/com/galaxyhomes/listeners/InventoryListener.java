package com.galaxyhomes.listeners;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import java.util.*;

public class InventoryListener implements Listener {
    private final GalaxyHomes plugin;

    public InventoryListener(GalaxyHomes p) {
        plugin = p;
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!"Your Homes".equalsIgnoreCase(title)) return;

        e.setCancelled(true);

        if (e.getRawSlot() < 0 ||
            e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;

        int slot = e.getRawSlot();
        if (slot >= 45) return;

        List<Home> homes = new ArrayList<>(
            plugin.getPlayerDataManager()
                .getPlayerData(p.getUniqueId())
                .getHomes().values()
        );

        if (slot >= homes.size()) return;

        Home h = homes.get(slot);

        if (e.isRightClick()) {
            p.sendMessage(
                "§eHome: §f" + h.getName()
                + " §7— use /delhome " + h.getName() + " to delete it."
            );
            return;
        }

        var loc = h.toLocation();

        if (loc == null) {
            p.sendMessage("§cThat home's world is unavailable.");
            return;
        }

        p.closeInventory();

        MessageUtils.sendMessage(
            p, "home-teleporting", Map.of("name", h.getName())
        );

        plugin.getTeleportManager().startTeleport(
            p, loc, "home",
            ok -> {
                if (ok) {
                    MessageUtils.sendMessage(
                        p, "home-teleported", Map.of("name", h.getName())
                    );
                }
            }
        );
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        String title = ChatColor.stripColor(e.getView().getTitle());

        if ("Your Homes".equalsIgnoreCase(title)) {
            e.setCancelled(true);
        }
    }
}
