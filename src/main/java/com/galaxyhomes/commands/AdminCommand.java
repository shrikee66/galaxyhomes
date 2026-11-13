
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.command.*;
import java.io.*;
import java.nio.file.*;

public class AdminCommand extends BaseCommand {

    public AdminCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.admin")) return true;

        if (a.length == 0) {
            s.sendMessage("§e/crossplayhomes <reload|backup|stats>");
            return true;
        }

        switch (a[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                s.sendMessage("§aGalaxyHomes reloaded.");
            }
            case "stats" -> {
                s.sendMessage("§bPlayers: §f" + plugin.getPlayerDataManager().getLoadedPlayerCount()
                    + " §bHomes: §f" + plugin.getPlayerDataManager().getTotalHomeCount()
                    + " §bTPA: §f" + plugin.getTPAManager().getActiveRequestCount());
            }
            case "backup" -> {
                try {
                    File dest = plugin.getBackupFolder();
                    dest.mkdirs();

                    for (File f : plugin.getPlayersFolder().listFiles((d, n) -> n.endsWith(".yml"))) {
                        Files.copy(f.toPath(),
                            new File(dest, f.getName() + ".manual-backup").toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                    }

                    s.sendMessage("§aPlayer data backed up.");
                } catch (Exception e) {
                    s.sendMessage("§cBackup failed: " + e.getMessage());
                }
            }
            default -> s.sendMessage("§e/crossplayhomes <reload|backup|stats>");
        }

        return true;
    }
}
