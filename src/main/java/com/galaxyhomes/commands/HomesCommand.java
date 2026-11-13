package com.galaxyhomes.commands;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.data.Home;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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
        Inventory inv = Bukkit.createInventory(null, 54, "Homes");
        NamespacedKey actionKey = new NamespacedKey(plugin, "menu_action");
        NamespacedKey homeKey = new NamespacedKey(plugin, "home_name");

        List<Home> homes = new ArrayList<>(data(player).getHomes().values());

        ItemStack newHome = createActionItem(
            Material.GREEN_WOOL,
            ChatColor.GREEN + "New home here",
            List.of(ChatColor.GRAY + "Create a home at your current spot"),
            "new_home",
            null,
            actionKey,
            homeKey
        );
        inv.setItem(45, newHome);

        ItemStack creator = createActionItem(
            Material.NETHER_STAR,
            ChatColor.GOLD + "Made by shrikee <3",
            List.of(ChatColor.YELLOW + "Made by shrikee <3"),
            "creator_info",
            null,
            actionKey,
            homeKey
        );
        inv.setItem(49, creator);

        ItemStack manageHomes = createActionItem(
            Material.ORANGE_WOOL,
            ChatColor.GOLD + "Manage homes",
            List.of(ChatColor.GRAY + "Edit, rename and delete your homes"),
            "manage_homes",
            null,
            actionKey,
            homeKey
        );
        inv.setItem(53, manageHomes);

        int slot = 0;
        for (Home home : homes) {
            ItemStack item = createActionItem(
                Material.RED_BED,
                ChatColor.YELLOW + home.getName(),
                List.of(
                    ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(),
                    ChatColor.GRAY + "X: " + ChatColor.WHITE + (int) home.getX() +
                        ChatColor.GRAY + " Y: " + ChatColor.WHITE + (int) home.getY() +
                        ChatColor.GRAY + " Z: " + ChatColor.WHITE + (int) home.getZ(),
                    ChatColor.GREEN + "Click to manage"
                ),
                "open_home",
                home.getName(),
                actionKey,
                homeKey
            );
            inv.setItem(slot++, item);
        }

        if (homes.isEmpty()) {
            inv.setItem(22, createActionItem(
                Material.BOOK,
                ChatColor.RED + "No homes yet",
                List.of(ChatColor.GRAY + "Use the green wool to create one"),
                "none",
                null,
                actionKey,
                homeKey
            ));
        }

        player.openInventory(inv);
    }

    public void openManageHomesMenu(Player player) {
        List<Home> homes = new ArrayList<>(data(player).getHomes().values());
        Inventory inv = Bukkit.createInventory(null, 54, "Manage homes");
        NamespacedKey actionKey = new NamespacedKey(plugin, "menu_action");
        NamespacedKey homeKey = new NamespacedKey(plugin, "home_name");

        int slot = 0;
        for (Home home : homes) {
            inv.setItem(slot++, createActionItem(
                Material.RED_BED,
                ChatColor.YELLOW + home.getName(),
                List.of(
                    ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(),
                    ChatColor.GRAY + "Click to manage"
                ),
                "open_home",
                home.getName(),
                actionKey,
                homeKey
            ));
        }

        if (homes.isEmpty()) {
            inv.setItem(0, createActionItem(
                Material.BOOK,
                ChatColor.RED + "No homes yet",
                List.of(ChatColor.GRAY + "Use /sethome first"),
                "none",
                null,
                actionKey,
                homeKey
            ));
        }

        player.openInventory(inv);
    }

    public void openHomeDetailMenu(Player player, String homeName) {
        Home home = data(player).getHome(homeName);
        if (home == null) {
            MessageUtils.sendMessage(player, "home-not-found", Map.of("name", homeName));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, home.getName());
        NamespacedKey actionKey = new NamespacedKey(plugin, "menu_action");
        NamespacedKey homeKey = new NamespacedKey(plugin, "home_name");

        inv.setItem(10, createActionItem(
            Material.ENDER_PEARL,
            ChatColor.GREEN + "Teleport",
            List.of(ChatColor.GRAY + "Travel to this home"),
            "teleport_home",
            home.getName(),
            actionKey,
            homeKey
        ));

        inv.setItem(19, createActionItem(
            Material.NAME_TAG,
            ChatColor.AQUA + "Rename",
            List.of(ChatColor.GRAY + "Rename this home"),
            "rename_home",
            home.getName(),
            actionKey,
            homeKey
        ));

        inv.setItem(28, createActionItem(
            Material.BUCKET,
            ChatColor.RED + "Delete",
            List.of(ChatColor.GRAY + "Remove this home"),
            "delete_home",
            home.getName(),
            actionKey,
            homeKey
        ));

        itemInfoHeader(inv, home);
        player.openInventory(inv);
    }

    private void itemInfoHeader(Inventory inv, Home home) {
        ItemStack header = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = header.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(ChatColor.WHITE + home.getName());
        meta.setLore(List.of(
            ChatColor.GRAY + "World: " + ChatColor.WHITE + home.getWorldName(),
            ChatColor.GRAY + "X: " + ChatColor.WHITE + (int) home.getX() +
                ChatColor.GRAY + " Y: " + ChatColor.WHITE + (int) home.getY() +
                ChatColor.GRAY + " Z: " + ChatColor.WHITE + (int) home.getZ()
        ));
        header.setItemMeta(meta);
        inv.setItem(13, header);
    }

    private ItemStack createActionItem(
        Material material,
        String displayName,
        List<String> lore,
        String action,
        String homeName,
        NamespacedKey actionKey,
        NamespacedKey homeKey
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(displayName);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (homeName != null && !homeName.isBlank()) {
            meta.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, homeName);
        }
        item.setItemMeta(meta);
        return item;
    }
}






























































































































































































































