package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.GeyserUtils;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomesCommand extends BaseCommand {

    public HomesCommand(GalaxyHomes p) {
        super(p);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.homes")) return true;

        Player p = player(s);
        if (p == null) return true;

        open(p, 0);
        return true;
    }

    public void open(Player player, int page) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());

        if (homes.isEmpty()) {
            MessageUtils.sendMessage(player, "home-list-empty");
            return;
        }

        if (GeyserUtils.isBedrockPlayer(player)) {
            openBedrockHomes(player, homes);
            return;
        }

        int size = 54;
        Inventory inv = Bukkit.createInventory(
            null, size, MessageUtils.color("&8&lYour Homes")
        );

        int start = page * 45;

        for (int i = start; i < Math.min(start + 45, homes.size()); i++) {
            Home home = homes.get(i);

            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            meta.setDisplayName(MessageUtils.color("&b&l" + home.getName()));
            meta.setLore(List.of(
                "§7World: §f" + home.getWorldName(),
                "§7X: §f" + (int) home.getX() +
                    " §7Y: §f" + (int) home.getY() +
                    " §7Z: §f" + (int) home.getZ(),
                "§aLeft-click to teleport"
            ));

            item.setItemMeta(meta);
            inv.setItem(i - start, item);
        }

        if (page > 0) inv.setItem(45, new ItemStack(Material.ARROW));
        if (start + 45 < homes.size()) inv.setItem(53, new ItemStack(Material.ARROW));

        player.openInventory(inv);
    }

    private void openBedrockHomes(Player player, List<Home> homes) {
        List<GeyserUtils.Button> buttons = new ArrayList<>();
        for (Home home : homes) {
            String label = home.getName() + " (" + home.getWorldName() + ")";
            buttons.add(new GeyserUtils.Button(label, "home:" + home.getName()));
        }

        GeyserUtils.sendForm(
            player,
            "bedrock-homes-" + player.getUniqueId(),
            "Your Homes",
            "Choose a home to teleport to.",
            buttons,
            (bedrockPlayer, response) -> {
                if (response == null || !response.startsWith("home:")) return;
                String homeName = response.substring("home:".length());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Home selected = data(bedrockPlayer).getHome(homeName);
                    if (selected == null) {
                        MessageUtils.sendMessage(bedrockPlayer, "home-not-found", Map.of("name", homeName));
                        return;
                    }
                    teleportToHome(bedrockPlayer, selected);
                });
            }
        );
    }

    private void teleportToHome(Player player, Home home) {
        var location = home.toLocation();
        if (location == null) {
            player.sendMessage("§cThat home's world is unavailable.");
            return;
        }

        MessageUtils.sendMessage(player, "home-teleporting", Map.of("name", home.getName()));
        plugin.getTeleportManager().startTeleport(player, location, "home", success -> {
            if (success) {
                MessageUtils.sendMessage(player, "home-teleported", Map.of("name", home.getName()));
            }
        });
    }
}
