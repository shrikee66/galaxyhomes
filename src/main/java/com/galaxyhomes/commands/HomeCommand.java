
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class HomeCommand extends BaseCommand {

    public HomeCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.home")) return true;

        Player p = player(s);
        if (p == null) return true;

        if (a.length == 0) {
            new HomesCommand(plugin).open(p, 0);
            return true;
        }

        String n = arg(a, 0);
        var h = data(p).getHome(n);

        if (h == null) {
            MessageUtils.sendMessage(p, "home-not-found", Map.of("name", n));
            return true;
        }

        var loc = h.toLocation();

        if (loc == null) {
            p.sendMessage("§cThe world for this home is unavailable.");
            return true;
        }

        MessageUtils.sendMessage(p, "home-teleporting", Map.of("name", h.getName()));

        plugin.getTeleportManager().startTeleport(p, loc, "home", ok -> {
            if (ok) MessageUtils.sendMessage(p, "home-teleported", Map.of("name", h.getName()));
        });

        return true;
    }
}
