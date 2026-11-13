package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Comparator;
import java.util.List;

public class TPACommand extends BaseCommand {

    public TPACommand(GalaxyHomes p) {
        super(p);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpa")) return true;

        Player p = player(s);
        if (p == null) return true;

        if (a.length == 0) {
            openPlayerPicker(p);
            return true;
        }

        Player target = Bukkit.getPlayerExact(a[0]);
        if (target == null) {
            s.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        plugin.getTPAManager().sendTPARequest(p, target);
        return true;
    }

    private void openPlayerPicker(Player player) {
        List<Player> players = Bukkit.getOnlinePlayers()
            .stream()
            .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        if (players.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No players are online.");
            return;
        }

        int size = Math.max(9, ((players.size() + 8) / 9) * 9);
        Inventory inv = Bukkit.createInventory(null, size, "Select player for TPA");

        NamespacedKey targetKey = new NamespacedKey(plugin, "target_uuid");
        NamespacedKey typeKey = new NamespacedKey(plugin, "request_type");

        for (Player target : players) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(target.getUniqueId()));
                meta.setDisplayName(ChatColor.YELLOW + target.getName());
                meta.setLore(List.of(ChatColor.GRAY + "Request teleport to this player"));
                meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target.getUniqueId().toString());
                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "tpa");
                head.setItemMeta(meta);
            }
            inv.addItem(head);
        }

        player.openInventory(inv);
    }
}
