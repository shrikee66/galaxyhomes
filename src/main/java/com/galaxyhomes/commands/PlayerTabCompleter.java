
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class PlayerTabCompleter implements TabCompleter {

    private final GalaxyHomes plugin;

    public PlayerTabCompleter(GalaxyHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length != 1) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);

        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !sender.equals(p))
            .map(Player::getName)
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
            .sorted()
            .toList();
    }
}
