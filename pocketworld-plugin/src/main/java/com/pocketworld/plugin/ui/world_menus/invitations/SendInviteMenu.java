package com.pocketworld.plugin.ui.world_menus.invitations;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketAnvilMenu;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Prompts the world owner/mod to type the username of the player they want to invite. */
public class SendInviteMenu extends PocketAnvilMenu {

    private final PocketWorld world;

    public SendInviteMenu(Player player, PocketWorldPlugin plugin, PocketWorld world) {
        super(player, plugin);
        this.world = world;
    }

    @Override
    public void open() {
        player.closeInventory();
        ColorFormat.formatList(java.util.List.of(
                "&6&lSend Invitation",
                "&eType the username of the player to invite in chat.",
                "&8Note: Player must be online.")).forEach(player::sendMessage);
        plugin.getChatInputRegistry().await(player.getUniqueId(), this::handleInput);
    }

    private boolean handleInput(String username) {
        Player target = Bukkit.getPlayer(username);
        if (target == null) {
            player.sendMessage(ColorFormat.format("&cPlayer not online."));
            return false;
        }
        if (world.getUsers().containsKey(target.getUniqueId())) {
            player.sendMessage(ColorFormat.format("&cAlready a member!"));
            return false;
        }
        if (world.getInvitations().containsKey(target.getUniqueId())) {
            player.sendMessage(ColorFormat.format("&cAlready invited!"));
            return false;
        }

        world.sendInvitation(plugin, player, target);
        return true;
    }
}
