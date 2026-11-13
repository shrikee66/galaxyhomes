
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TPAcceptCommand extends BaseCommand {

    public TPAcceptCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpaccept")) return true;

        Player p = player(s);

        if (p != null) {
            plugin.getTPAManager().acceptTPARequest(p);
        }

        return true;
    }
}
