package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketAnvilMenu;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/** Prompts the admin to type the username of the player they want to teleport into the world -
 *  same chat-input mechanism {@code SendInviteMenu} uses for invite targets. */
public class AdminTeleportPlayerPrompt extends PocketAnvilMenu {

    private final PocketWorld world;

    public AdminTeleportPlayerPrompt(Player player, PocketWorldPlugin plugin, PocketWorld world) {
        super(player, plugin);
        this.world = world;
    }

    @Override
    public void open() {
        player.closeInventory();
        ColorFormat.formatList(List.of(
                "&6&lTeleport a Player",
                "&eType the username of the player to teleport into \"" + world.getWorldName() + "\" in chat.",
                "&8Note: Player must be online.")).forEach(player::sendMessage);
        plugin.getChatInputRegistry().await(player.getUniqueId(), this::handleInput);
    }

    private boolean handleInput(String username) {
        Player target = Bukkit.getPlayer(username);
        if (target == null) {
            player.sendMessage(ColorFormat.format("&cPlayer not online."));
            return false;
        }

        if (!world.isLoaded()) {
            world.load(plugin, target.getUniqueId(), true, true);
        } else {
            world.teleport(target);
        }

        player.sendMessage(ColorFormat.format("&aTeleported " + target.getName() + " into \"" + world.getWorldName() + "\"."));
        return true;
    }
}
