package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.utils.GeyserUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TPACommand extends BaseCommand {
    private static final int BEDROCK_PAGE_SIZE = 8;

    public TPACommand(GalaxyHomes p) {
        super(p);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.tpa")) return true;

        Player p = player(s);
        if (p == null) return true;

        if (a.length == 0) {
            openPlayerPicker(p, 0);
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

    private void openPlayerPicker(Player player, int page) {
        List<Player> players = Bukkit.getOnlinePlayers()
            .stream()
            .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());

        if (players.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No players are online.");
            return;
        }

        if (GeyserUtils.isBedrockPlayer(player)) {
            openBedrockPlayerPicker(player, players, page);
            return;
        }

        int size = Math.max(9, ((players.size() + 8) / 9) * 9);
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, size, "Select player for TPA");

        org.bukkit.NamespacedKey targetKey = new org.bukkit.NamespacedKey(plugin, "target_uuid");
        org.bukkit.NamespacedKey typeKey = new org.bukkit.NamespacedKey(plugin, "request_type");

        for (Player target : players) {
            org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(target.getUniqueId()));
                meta.setDisplayName(ChatColor.YELLOW + target.getName());
                meta.setLore(List.of(ChatColor.GRAY + "Request teleport to this player"));
                meta.getPersistentDataContainer().set(targetKey, org.bukkit.persistence.PersistentDataType.STRING, target.getUniqueId().toString());
                meta.getPersistentDataContainer().set(typeKey, org.bukkit.persistence.PersistentDataType.STRING, "tpa");
                head.setItemMeta(meta);
            }
            inv.addItem(head);
        }

        player.openInventory(inv);
    }

    private void openBedrockPlayerPicker(Player player, List<Player> players, int page) {
        int pageCount = Math.max(1, (players.size() + BEDROCK_PAGE_SIZE - 1) / BEDROCK_PAGE_SIZE);
        int currentPage = Math.max(0, Math.min(page, pageCount - 1));
        int start = currentPage * BEDROCK_PAGE_SIZE;
        int end = Math.min(start + BEDROCK_PAGE_SIZE, players.size());
        List<GeyserUtils.Button> buttons = new ArrayList<>();

        for (int i = start; i < end; i++) {
            Player target = players.get(i);
            buttons.add(new GeyserUtils.Button(target.getName(), "player:" + target.getUniqueId()));
        }
        if (currentPage > 0) buttons.add(new GeyserUtils.Button("Previous page", "page:" + (currentPage - 1)));
        if (currentPage + 1 < pageCount) buttons.add(new GeyserUtils.Button("Next page", "page:" + (currentPage + 1)));

        GeyserUtils.sendForm(player, "tpa-player-picker", "Select player for TPA",
            "Online players: " + players.size() + " (page " + (currentPage + 1) + "/" + pageCount + ")",
            buttons,
            (bedrockPlayer, response) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (response == null) return;
                if (response.startsWith("page:")) {
                    try {
                        openPlayerPicker(bedrockPlayer, Integer.parseInt(response.substring(5)));
                    } catch (NumberFormatException ignored) {
                        openPlayerPicker(bedrockPlayer, currentPage);
                    }
                    return;
                }
                if (!response.startsWith("player:")) return;
                try {
                    Player target = Bukkit.getPlayer(UUID.fromString(response.substring(7)));
                    if (target == null) {
                        bedrockPlayer.sendMessage(ChatColor.RED + "Player is no longer online.");
                        openPlayerPicker(bedrockPlayer, currentPage);
                        return;
                    }
                    plugin.getTPAManager().sendTPARequest(bedrockPlayer, target);
                } catch (IllegalArgumentException ignored) {
                    bedrockPlayer.sendMessage(ChatColor.RED + "Invalid player selection.");
                }
            }));
    }
}
