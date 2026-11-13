
package com.galaxyhomes.commands;

import org.bukkit.command.*;
import java.util.*;

public class AdminTabCompleter implements TabCompleter {
    public AdminTabCompleter(Object ignored) {}

    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return args.length == 1
                ? List.of("reload", "backup", "stats").stream()
                    .filter(x -> x.startsWith(args[0].toLowerCase()))
                    .toList()
                : List.of();
    }
}
