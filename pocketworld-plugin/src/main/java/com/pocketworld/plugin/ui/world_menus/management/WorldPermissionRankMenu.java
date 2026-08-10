package com.pocketworld.plugin.ui.world_menus.management;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.world.PermissionRank;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldAction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Toggles which {@link WorldAction}s a single {@link PermissionRank} is allowed to perform in this
 * world. Visitors get the physical-interaction actions (build/break/interact); members and mods get
 * the membership actions (invite/kick/set spawn) - the same split {@link WorldAction}'s own javadoc
 * documents. Changes are held on the in-memory {@link PocketWorld} and persist the same way every
 * other world property does (on cache flush), so no explicit save is needed here.
 */
public class WorldPermissionRankMenu extends PocketMenu {

    private static final List<WorldAction> VISITOR_ACTIONS = List.of(WorldAction.BUILD, WorldAction.BREAK, WorldAction.INTERACT);
    private static final List<WorldAction> MEMBER_ACTIONS = List.of(WorldAction.INVITE, WorldAction.KICK, WorldAction.SET_SPAWN);

    private final PocketWorld world;
    private final PermissionRank rank;
    private final WorldPermissionsMenu previousMenu;
    private final List<WorldAction> applicableActions;

    public WorldPermissionRankMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, PermissionRank rank,
                                    WorldPermissionsMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.rank = rank;
        this.previousMenu = previousMenu;
        this.applicableActions = rank == PermissionRank.VISITOR ? VISITOR_ACTIONS : MEMBER_ACTIONS;
    }

    @Override
    public String getMenuName() {
        return "&4&l" + capitalize(rank.name()) + " Permissions";
    }

    @Override
    public int getMenuSlots() {
        return 36;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        int[] slots = {20, 22, 24};
        for (int i = 0; i < applicableActions.size(); i++) {
            inventory.setItem(slots[i], getActionToggle(applicableActions.get(i)));
        }

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aGo Back")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();
        inventory.setItem(27, back);

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

        if (tag.equalsIgnoreCase("is-back-button")) {
            previousMenu.open();
            return;
        }

        for (WorldAction action : applicableActions) {
            if (tag.equalsIgnoreCase("permission-toggle-" + action.name())) {
                Set<WorldAction> actions = world.getPermissions().computeIfAbsent(rank, r -> EnumSet.noneOf(WorldAction.class));
                if (!actions.add(action)) {
                    actions.remove(action);
                }
                open();
                return;
            }
        }
    }

    private ItemStack getActionToggle(WorldAction action) {
        boolean enabled = world.getPermissions().getOrDefault(rank, Set.of()).contains(action);
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String displayName = (enabled ? "&a" : "&c") + capitalize(action.name());

        List<String> lore = new ArrayList<>();
        lore.add(enabled ? "&7Enabled" : "&7Disabled");
        lore.add(" ");
        lore.add(enabled ? "&8Click to disable." : "&8Click to enable.");

        return new PocketItem.Builder(plugin)
                .material(material)
                .displayName(displayName)
                .lore(lore)
                .tag("permission-toggle-" + action.name())
                .build().get();
    }

    private static String capitalize(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase();
    }
}
