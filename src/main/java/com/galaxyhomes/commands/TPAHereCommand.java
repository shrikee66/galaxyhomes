
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TPAHereCommand extends BaseCommand {

    public TPAHereCommand(GalaxyHomes p) {
        super(p);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpahere")) return true;

        Player p = player(s);
        if (p == null) return true;

        if (a.length == 0) {
            p.sendMessage("§cUsage: /tpahere <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(a[0]);

        if (target == null) {
            p.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        plugin.getTPAManager().sendTPAHereRequest(p, target);
        return true;
    }
}
