package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class PlayerSetRankMenu extends PocketMenu {

    private PocketWorld world;
    private UUID userToSetRank;
    private ManagePlayerMenu previousMenu;

    public PlayerSetRankMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                             UUID userToSetRank, ManagePlayerMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.userToSetRank = userToSetRank;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lSet";
    }

    @Override
    public int getMenuSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        String name = Bukkit.getPlayer(userToSetRank).getName();
        WorldRank rank = world.getUsers().get(userToSetRank);

        ItemStack userIcon = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&a" + name)
                .lore(List.of("&7Rank: " + rank.name(), " ", "&8" + userToSetRank))
                .build().getAsSkull(name);

        ItemStack backButton = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .displayName("&aMain Menu")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        ItemStack member = new PocketItem.Builder(plugin)
                .material(Material.COAL)
                .displayName("&6Member")
                .lore(List.of("&7Click to set this user to member rank"))
                .tag("set-rank-member")
                .build().get();

        ItemStack mod = new PocketItem.Builder(plugin)
                .material(Material.GOLD_INGOT)
                .displayName("&6Mod")
                .lore(List.of("&7Click to set this user to member rank"))
                .tag("set-rank-mod")
                .build().get();

        ItemStack owner = new PocketItem.Builder(plugin)
                .material(Material.DIAMOND)
                .displayName("&6Owner")
                .lore(List.of("&7Click to transfer ownership to this user"))
                .tag("set-rank-owner")
                .build().get();

        inventory.setItem(13, userIcon);
        inventory.setItem(20, member);
        inventory.setItem(22, mod);
        inventory.setItem(24, owner);
        inventory.setItem(27, backButton);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLUE_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build().get();

        addFillerBorder(fillerItem);
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

        WorldRank rank = world.getUsers().get(userToSetRank);
        String name = Bukkit.getPlayer(userToSetRank).getName();

        //TODO: Announce rank updates to online world members

        if (tag.equalsIgnoreCase("is-back-button")) {
            previousMenu.open();
        } else if (tag.equalsIgnoreCase("set-rank-member")) {
            if (rank == WorldRank.MEMBER) {
                player.sendMessage(ColorFormat.format("&4&lERROR &r&cUser is already a MEMBER."));
            }
            player.closeInventory();

            world.getUsers().put(userToSetRank, WorldRank.MEMBER);
            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aRank of user '" + name + "' set to MEMBER"));
        } else if (tag.equalsIgnoreCase("set-rank-mod")) {
            if (rank == WorldRank.MOD) {
                player.sendMessage(ColorFormat.format("&4&lERROR &r&cUser is already a MOD"));
                return;
            }

            player.closeInventory();

            world.getUsers().put(userToSetRank, WorldRank.MOD);
            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aRank of user '" + name + "' set to MOD"));
        } else if (tag.equalsIgnoreCase("set-rank-owner")) {
            player.closeInventory();
            world.getUsers().put(userToSetRank, WorldRank.OWNER);
            world.getUsers().put(player.getUniqueId(), WorldRank.MOD);

            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aWorld owner transferred to '" + name + "'"));
        }
    }
}
