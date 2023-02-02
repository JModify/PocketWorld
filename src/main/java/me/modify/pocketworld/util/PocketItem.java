package me.modify.pocketworld.util;

import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

public class PocketItem {

    @Getter
    private final PocketWorldPlugin plugin; //required

    @Getter
    private final Material material;

    @Getter
    private final String displayName;

    @Getter
    private final List<String> lore;

    @Getter
    private final int stackSize;

    @Getter
    private final String tag;

    public PocketItem(Builder itemBuilder) {
        this.plugin = itemBuilder.plugin;
        this.material = itemBuilder.material;
        this.displayName = itemBuilder.displayName;
        this.lore = itemBuilder.lore;
        this.tag = itemBuilder.tag;
        this.stackSize = itemBuilder.stackSize;
    }

    /**
     * Retrieves this pocket item as an item stack.
     *
     * @return pocket item as item stack.
     */
    public ItemStack get() {
        ItemStack item = new ItemStack(material);
        item.setAmount(stackSize != 0 ? stackSize : 1);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName != null ? ColorFormat.format(displayName) : " ");
        meta.setLore(lore != null ? ColorFormat.formatList(lore) : null);

        if (tag != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, tag);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, 1);
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getAsSkull(String owner) {
        ItemStack item = get();
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        item.setItemMeta(skullMeta);
        return item;
    }

    public static boolean hasTag(PocketWorldPlugin plugin, ItemStack item, String tag) {
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, tag);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(namespacedKey, PersistentDataType.INTEGER);
    }

    /**
     * Determines if an item has any of the multiple tags specified in the parameters.
     *
     * Returns true if any "tag" parameter is applied to the item specified, else false.
     *
     * @param plugin plugin instance
     * @param item item to check
     * @param tags tags to search for.
     *
     * @return true if item has one of the tags, else false
     */
    public static boolean hasAnyTags(PocketWorldPlugin plugin, ItemStack item, String... tags) {
        ItemMeta meta = item.getItemMeta();

        for (int i = 0; i < tags.length; i++) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, tags[i]);
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (container.has(namespacedKey, PersistentDataType.INTEGER)) {
                return true;
            }
        }

        return false;
    }

    public static String getTag(PocketWorldPlugin plugin, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Optional<NamespacedKey> key = container.getKeys().stream().findFirst();
        return key.map(NamespacedKey::getKey).orElse(null);
    }

    public static class Builder {
        private final PocketWorldPlugin plugin;

        private Material material;
        private String displayName;
        private List<String> lore;
        private String tag;
        private int stackSize;

        public Builder(PocketWorldPlugin plugin) {
            this.plugin = plugin;
        }

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public PocketItem build() {
            return new PocketItem(this);
        }

    }
}
