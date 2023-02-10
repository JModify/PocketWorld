package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketPaginatedMenu;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerManagementListMenu extends PocketPaginatedMenu {

    private ManageWorldMenu previousMenu;
    private PocketWorld world;

    public PlayerManagementListMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                                    ManageWorldMenu previousMenu) {
        super(player, plugin);
        this.previousMenu = previousMenu;
        this.world = world;
    }

    @Override
    public String getMenuName() {
        return "&4&lPlayer Management Menu";
    }

    @Override
    public void setMenuItems() {

        Inventory inventory = getInventory();

        addMenuBorder(Material.BLUE_STAINED_GLASS_PANE);

        String members = world.getUsers().keySet().stream()
                .map(uuid -> Bukkit.getPlayer(uuid).getName())
                .collect(Collectors.joining(", "));
        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + members,
                        "&7Size: " + getWorldSizeFormatted(world.getWorldSize()), " ", "&8" + world.getId().toString()))
                .build().getAsSkull("BlockminersTV");
        inventory.setItem(4, globe);

        List<UUID> userIds = new ArrayList<>(world.getUsers().keySet());

        if (!userIds.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= userIds.size()) break;

                UUID id = userIds.get(i);

                if (id == null) continue;

                String name = Bukkit.getPlayer(id).getName();
                WorldRank rank = world.getUsers().get(id);

                ItemStack userIcon = new PocketItem.Builder(plugin)
                        .material(Material.PLAYER_HEAD)
                        .displayName("&a" + name)
                        .lore(List.of("&7Rank: " + rank.name(), " ", "&8" + id))
                        .tag(id.toString())
                        .build().getAsSkull(name);

                inventory.addItem(userIcon);
            }
        }
    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {
        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null) {
            return;
        }

        String tag = PocketItem.getTag(plugin, item);
        if (tag == null) {
            return;
        }

        if (tag.equalsIgnoreCase("is-home-button")) {
            previousMenu.open();
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (!((index + 1) >= world.getUsers().size())) {
                page = page + 1;
                super.open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page = page - 1;
                super.open();
            }
        } else {
            Map<UUID, WorldRank> users = world.getUsers();

            WorldRank playerRank = users.get(player.getUniqueId());
            if (playerRank != WorldRank.OWNER && playerRank != WorldRank.MOD) {
                return;
            }

            Optional<UUID> userId = world.getUsers().keySet().stream()
                    .filter(id -> id.equals(UUID.fromString(ChatColor.stripColor(tag)))).findFirst();

            if (userId.isEmpty()) {
                return;
            }

            ManagePlayerMenu nextMenu = new ManagePlayerMenu(player, plugin, world,
                    userId.get(), this);
            nextMenu.open();

        }
    }

    private String getWorldSizeFormatted(int worldSize) {
        return worldSize + "x" + worldSize;
    }
}
