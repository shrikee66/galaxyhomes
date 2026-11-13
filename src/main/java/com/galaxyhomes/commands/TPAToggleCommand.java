
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TPAToggleCommand extends BaseCommand {
    public TPAToggleCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpatoggle")) return true;

        Player p = player(s);
        if (p == null) return true;

        var d = data(p).getTPAData();
        d.setToggle(!d.isToggle());
        data(p).setDirty(true);

        MessageUtils.sendMessage(p, d.isToggle() ? "tpa-toggled-off" : "tpa-toggled-on");
        return true;
    }
}
