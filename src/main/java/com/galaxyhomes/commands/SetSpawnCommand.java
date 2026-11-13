
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends BaseCommand {

    public SetSpawnCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.setspawn")) return true;

        Player p = player(s);
        if (p == null) return true;

        plugin.getConfigManager().setSpawnLocation(p.getLocation());
        plugin.getConfigManager().saveConfig();
        MessageUtils.sendMessage(p, "spawn-set");

        return true;
    }
}
