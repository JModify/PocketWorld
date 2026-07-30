package com.pocketworld.plugin.ui;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.util.ColorFormat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

/** A menu item builder that tags its {@link ItemStack} via PDC, so menus can route clicks by tag. */
public class PocketItem {

    private final PocketWorldPlugin plugin;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int stackSize;
    private final String tag;
    private final boolean enchantGlow;

    public PocketItem(Builder itemBuilder) {
        this.plugin = itemBuilder.plugin;
        this.material = itemBuilder.material;
        this.displayName = itemBuilder.displayName;
        this.lore = itemBuilder.lore;
        this.tag = itemBuilder.tag;
        this.stackSize = itemBuilder.stackSize;
        this.enchantGlow = itemBuilder.enchantGlow;
    }

    public ItemStack get() {
        ItemStack item = new ItemStack(material);
        item.setAmount(stackSize != 0 ? stackSize : 1);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName != null ? ColorFormat.format(displayName) : " ");
        meta.setLore(lore != null ? ColorFormat.formatList(lore) : null);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (enchantGlow) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (tag != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, tag);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, 1);
        }

        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
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
        return meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.INTEGER);
    }

    /** Returns true if the item has any of the given tags applied. */
    public static boolean hasAnyTags(PocketWorldPlugin plugin, ItemStack item, String... tags) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (String tag : tags) {
            if (container.has(new NamespacedKey(plugin, tag), PersistentDataType.INTEGER)) {
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

    public static final class Builder {
        private final PocketWorldPlugin plugin;

        private Material material;
        private String displayName;
        private List<String> lore;
        private String tag;
        private int stackSize;
        private boolean enchantGlow;

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

        public Builder glow(boolean enchantGlow) {
            this.enchantGlow = enchantGlow;
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
