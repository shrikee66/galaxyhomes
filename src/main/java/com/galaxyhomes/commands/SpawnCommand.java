
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SpawnCommand extends BaseCommand {

    public SpawnCommand(GalaxyHomes p) {
        super(p);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.spawn")) return true;

        Player p = player(s);
        if (p == null) return true;

        Location spawn = plugin.getConfigManager().getSpawnLocation();

        if (spawn == null) {
            MessageUtils.sendMessage(p, "spawn-not-set");
            return true;
        }

        plugin.getTeleportManager().startTeleport(p, spawn, "spawn", success -> {
            if (success) MessageUtils.sendMessage(p, "spawn-teleported");
        });

        return true;
    }
}
