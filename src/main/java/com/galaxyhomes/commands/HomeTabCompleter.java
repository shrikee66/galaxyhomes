
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class HomeTabCompleter implements TabCompleter {

    private final GalaxyHomes p;

    public HomeTabCompleter(GalaxyHomes p) {
        this.p = p;
    }

    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (!(s instanceof Player pl)) return List.of();
        if (args.length != 1) return List.of();

        return p.getPlayerDataManager()
            .getPlayerData(pl.getUniqueId())
            .getHomes()
            .keySet()
            .stream()
            .filter(n -> n.startsWith(args[0].toLowerCase()))
            .toList();
    }
}
