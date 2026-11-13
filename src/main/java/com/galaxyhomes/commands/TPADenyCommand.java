
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TPADenyCommand extends BaseCommand {

    public TPADenyCommand(GalaxyHomes p) {
        super(p);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpadeny")) return true;

        Player p = player(s);
        if (p == null) return true;

        plugin.getTPAManager().denyRequest(p);
        return true;
    }
}
