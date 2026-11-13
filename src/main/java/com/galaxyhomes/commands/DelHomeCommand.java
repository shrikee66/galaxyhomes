
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class DelHomeCommand extends BaseCommand {

    public DelHomeCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.delhome")) return true;

        Player p = player(s);
        if (p == null) return true;

        if (a.length == 0) {
            s.sendMessage("§cUsage: /delhome <name>");
            return true;
        }

        String n = arg(a, 0);

        if (data(p).removeHome(n)) {
            MessageUtils.sendMessage(p, "home-deleted", Map.of("name", n));
        } else {
            MessageUtils.sendMessage(p, "home-not-found", Map.of("name", n));
        }

        return true;
    }
}
