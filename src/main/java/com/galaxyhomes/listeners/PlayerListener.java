package com.galaxyhomes.listeners;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {
    private final GalaxyHomes plugin;

    public PlayerListener(GalaxyHomes p) {
        plugin = p;
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        if (e.getTo() != null) {
            plugin.getTeleportManager().onPlayerMove(
                e.getPlayer(), e.getFrom(), e.getTo()
            );
        }
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.Player p) {
            plugin.getTeleportManager().onPlayerDamage(p);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        plugin.getTPAManager().handlePlayerQuit(e.getPlayer());
        plugin.getPlayerDataManager().unloadPlayerData(
            e.getPlayer().getUniqueId()
        );
    }
}
