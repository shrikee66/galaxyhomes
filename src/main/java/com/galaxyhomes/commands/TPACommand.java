package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPACommand extends BaseCommand {
    public TPACommand(GalaxyHomes plugin) { super(plugin); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!permission(sender, "galaxyhomes.tpa")) return true;
        Player player = player(sender);
        if (player == null) return true;
        if (args.length == 0) {
            plugin.getServer().getPluginManager().getRegisteredListeners(plugin).stream();
            for (org.bukkit.plugin.RegisteredListener listener : plugin.getServer().getPluginManager().getRegisteredListeners(plugin)) {
                if (listener.getListener() instanceof com.galaxyhomes.listeners.InventoryListener inventoryListener) {
                    inventoryListener.openPlayerPicker(player, false);
                    return true;
                }
            }
            player.sendMessage("§cPlayer selection is unavailable.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { player.sendMessage("§cPlayer not found or offline."); return true; }
        plugin.getTPAManager().sendTPARequest(player, target);
        return true;
    }
}
