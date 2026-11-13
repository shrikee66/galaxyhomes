
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Map;

public class SetHomeCommand extends BaseCommand {

    public SetHomeCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.sethome")) return true;

        Player p = player(s);
        if (p == null) return true;

        String n = a.length == 0
            ? plugin.getConfigManager().getDefaultHomeName()
            : arg(a, 0);

        if (n.isBlank() || n.length() > plugin.getConfigManager().getMaxNameLength()) {
            MessageUtils.sendMessage(p, "home-name-too-long",
                Map.of("max", String.valueOf(plugin.getConfigManager().getMaxNameLength())));
            return true;
        }

        var d = data(p);

        if (d.getHome(n) != null) {
            d.addHome(new Home(n, p.getLocation()));
            MessageUtils.sendMessage(p, "home-updated", Map.of("name", n));
            return true;
        }

        int max = plugin.getConfigManager().getMaxHomes();

        if (max >= 0 && d.getHomeCount() >= max) {
            MessageUtils.sendMessage(p, "home-limit-reached",
                Map.of("limit", String.valueOf(max)));
            return true;
        }

        d.addHome(new Home(n, p.getLocation()));
        MessageUtils.sendMessage(p, "home-set", Map.of("name", n));
        return true;
    }
}
