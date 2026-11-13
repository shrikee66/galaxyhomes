
package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.*;
import java.util.*;

public class HomesCommand extends BaseCommand {

    public HomesCommand(GalaxyHomes p) {
        super(p);
    }

    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!permission(s, "galaxyhomes.homes")) return true;

        Player p = player(s);
        if (p == null) return true;

        open(p, 0);
        return true;
    }

    public void open(Player p, int page) {
        List<Home> homes = new ArrayList<>(data(p).getHomes().values());

        if (homes.isEmpty()) {
            MessageUtils.sendMessage(p, "home-list-empty");
            return;
        }

        int size = 54;
        Inventory inv = Bukkit.createInventory(
            null, size, MessageUtils.color("&8&lYour Homes")
        );

        int start = page * 45;

        for (int i = start; i < Math.min(start + 45, homes.size()); i++) {
            Home h = homes.get(i);

            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta m = item.getItemMeta();

            m.setDisplayName(MessageUtils.color("&b&l" + h.getName()));
            m.setLore(List.of(
                "§7World: §f" + h.getWorldName(),
                "§7X: §f" + (int) h.getX() +
                    " §7Y: §f" + (int) h.getY() +
                    " §7Z: §f" + (int) h.getZ(),
                "§aLeft-click to teleport"
            ));

            item.setItemMeta(m);
            inv.setItem(i - start, item);
        }

        if (page > 0) inv.setItem(45, new ItemStack(Material.ARROW));
        if (start + 45 < homes.size()) inv.setItem(53, new ItemStack(Material.ARROW));

        p.openInventory(inv);
    }
}
