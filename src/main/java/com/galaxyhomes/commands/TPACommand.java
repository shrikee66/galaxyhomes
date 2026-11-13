package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TPACommand extends BaseCommand {

    public TPACommand(GalaxyHomes p) { super(p); }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpa")) return true;

        Player p = player(s);
        if (p == null) return true;

        if (a.length == 0) {
            s.sendMessage("§cUsage: /tpa <player>");
            return true;
        }

        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) {
            s.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        plugin.getTPAManager().sendTPARequest(p, t);
        return true;
    }
}
