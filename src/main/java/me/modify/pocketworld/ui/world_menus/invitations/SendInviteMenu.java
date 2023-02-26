package me.modify.pocketworld.ui.world_menus.invitations;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketAnvilMenu;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.InteractiveText;
import me.modify.pocketworld.world.PocketWorld;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SendInviteMenu extends PocketAnvilMenu {

    private PocketWorld world;
    public SendInviteMenu(Player player, PocketWorldPlugin plugin, PocketWorld world) {
        super(player, plugin);
        this.world = world;
    }

    @Override
    public void open() {
        new AnvilGUI.Builder()
                .onComplete((p, text) -> {
                    Player target = Bukkit.getPlayer(text);
                    if (target == null) {
                        return AnvilGUI.Response.text("Player not online.");
                    }

                    if (world.getUsers().containsKey(target.getUniqueId())) {
                        return AnvilGUI.Response.text("Already member!");
                    }

                    Map<UUID, UUID> invitations = world.getInvitations();
                    if (invitations.containsKey(target.getUniqueId())) {
                        return AnvilGUI.Response.text("Already Invited!");
                    }



                    world.sendInvitation(plugin, player, target);
                    player.closeInventory();
                    return AnvilGUI.Response.close();
                })
                .itemLeft(getItemLeft())
                .title("Send a new invitation")
                .plugin(plugin)
                .open(player);
    }

    private ItemStack getItemLeft() {
        return new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("Enter username.")
                .lore(List.of("&7Note: Target must be online."))
                .build().get();
    }
}
