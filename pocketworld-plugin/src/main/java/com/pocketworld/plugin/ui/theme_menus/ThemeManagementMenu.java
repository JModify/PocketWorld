package com.pocketworld.plugin.ui.theme_menus;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /theme manage} - a paginated grid of every theme, matching the pattern
 * {@code WorldManagementListMenu} already uses for pocket worlds. Replaces the previous chat-text
 * listing, whose "delete" button only ever suggested typing {@code /theme delete <id>} rather than
 * doing anything itself - clicking a theme here opens {@link ThemeManageMenu} for real actions.
 * Top-level command entry point, so its home button just closes the inventory.
 */
public class ThemeManagementMenu extends PocketPaginatedMenu {

    private final List<PocketTheme> themes;

    public ThemeManagementMenu(Player player, PocketWorldPlugin plugin, List<PocketTheme> themes) {
        super(player, plugin);
        this.themes = themes;
    }

    @Override
    public String getMenuName() {
        return "&4&lPocketThemes";
    }

    @Override
    public void setMenuItems() {
        addMenuBorder(Material.PURPLE_STAINED_GLASS_PANE);

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= themes.size()) {
                break;
            }

            PocketTheme theme = themes.get(index);
            if (theme == null) {
                continue;
            }

            ItemStack themeIcon = new PocketItem.Builder(plugin)
                    .material(theme.getIcon())
                    .displayName("&b" + theme.getName())
                    .lore(List.of("&7Click to view/manage this theme.", " ",
                            "&6Properties", "&eBiome: " + theme.getBiome(),
                            "&eDescription: " + theme.getDescription(), " ", "&8" + theme.getId()))
                    .tag(theme.getId().toString())
                    .build().get();

            getInventory().addItem(themeIcon);
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
            player.closeInventory();
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (index + 1 < themes.size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        } else {
            UUID themeId = UUID.fromString(tag);
            Optional<PocketTheme> theme = themes.stream().filter(t -> t.getId().equals(themeId)).findFirst();
            theme.ifPresent(t -> new ThemeManageMenu(player, plugin, t, this::open).open());
        }
    }
}
