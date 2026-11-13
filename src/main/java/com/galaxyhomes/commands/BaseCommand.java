
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.PlayerData;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public abstract class BaseCommand implements CommandExecutor {

    protected final GalaxyHomes plugin;

    protected BaseCommand(GalaxyHomes plugin) {
        this.plugin = plugin;
    }

    protected Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        MessageUtils.sendMessage(s, "player-only");
        return null;
    }

    protected PlayerData data(Player p) {
        return plugin.getPlayerDataManager().getPlayerData(p.getUniqueId());
    }

    protected boolean permission(CommandSender s, String node) {
        if (s.hasPermission(node)) return true;
        MessageUtils.sendMessage(s, "no-permission");
        return false;
    }

    protected String arg(String[] a, int start) {
        return String.join(" ", Arrays.copyOfRange(a, start, a.length));
    }
}
