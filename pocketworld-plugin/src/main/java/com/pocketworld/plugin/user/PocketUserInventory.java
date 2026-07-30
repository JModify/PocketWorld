package com.pocketworld.plugin.user;

import com.pocketworld.plugin.PocketWorldPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Stashes/restores a player's inventory via their own persistent data container, used while
 * they're building inside a theme's editor world.
 */
public final class PocketUserInventory {

    private static final String KEY = "pw-user-inventory";

    private PocketUserInventory() {}

    public static void saveUserInventory(PocketWorldPlugin plugin, Player player) {
        Inventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream)) {
                bukkitObjectOutputStream.writeInt(items.length);
                for (ItemStack item : items) {
                    bukkitObjectOutputStream.writeObject(item);
                }
            }

            String encodedInventory = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());

            NamespacedKey namespacedKey = new NamespacedKey(plugin, KEY);
            PersistentDataContainer data = player.getPersistentDataContainer();
            data.set(namespacedKey, PersistentDataType.STRING, encodedInventory);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save inventory for " + player.getName() + ": " + e);
        }
    }

    public static void restoreUserInventory(PocketWorldPlugin plugin, Player player) {
        NamespacedKey namespacedKey = new NamespacedKey(plugin, KEY);

        PersistentDataContainer data = player.getPersistentDataContainer();
        if (!data.has(namespacedKey, PersistentDataType.STRING)) {
            return;
        }

        String encodedInventory = data.get(namespacedKey, PersistentDataType.STRING);
        if (encodedInventory == null || encodedInventory.isBlank()) {
            return;
        }

        byte[] rawData = Base64.getDecoder().decode(encodedInventory);
        try (BukkitObjectInputStream bukkitObjectInputStream =
                     new BukkitObjectInputStream(new ByteArrayInputStream(rawData))) {
            int itemsCount = bukkitObjectInputStream.readInt();
            ItemStack[] items = new ItemStack[itemsCount];
            for (int i = 0; i < itemsCount; i++) {
                items[i] = (ItemStack) bukkitObjectInputStream.readObject();
            }

            data.remove(namespacedKey);
            player.getInventory().setContents(items);
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to restore inventory for " + player.getName() + ": " + e);
        }
    }
}
