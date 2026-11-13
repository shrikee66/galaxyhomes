
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TPASettingsCommand extends BaseCommand {
    public TPASettingsCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpasettings")) return true;

        Player p = player(s);
        if (p != null) {
            p.sendMessage("§bTPA Settings: §7Use /tpatoggle to enable or disable requests.");
        }
        return true;
    }
}
