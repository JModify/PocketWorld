package me.modify.pocketworld.user;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.mongo.MongoConstant;
import org.bson.Document;
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
import java.util.UUID;

public class UserInventory {

    public static void saveUserInventory(PocketWorldPlugin plugin, Player player) {
        Inventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream);

            bukkitObjectOutputStream.writeInt(items.length);

            for (ItemStack item : items) {
                bukkitObjectOutputStream.writeObject(item);
            }
            bukkitObjectOutputStream.flush();

            byte[] rawData = byteArrayOutputStream.toByteArray();
            String encodedInventory = Base64.getEncoder().encodeToString(rawData);

            bukkitObjectOutputStream.close();
            byteArrayOutputStream.close();

            NamespacedKey namespacedKey = new NamespacedKey(plugin, "pw-user-inventory");
            PersistentDataContainer data = player.getPersistentDataContainer();
            data.set(namespacedKey, PersistentDataType.STRING, encodedInventory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void restoreUserInventory(PocketWorldPlugin plugin, Player player) {
        NamespacedKey namespacedKey = new NamespacedKey(plugin, "pw-user-inventory");

        PersistentDataContainer data = player.getPersistentDataContainer();
        if (!data.has(namespacedKey, PersistentDataType.STRING)) {
            return;
        }

        String encodedInventory = data.get(namespacedKey, PersistentDataType.STRING);
        if (encodedInventory == null || encodedInventory.isEmpty() || encodedInventory.isBlank()) {
            return;
        }

        byte[] rawData = Base64.getDecoder().decode(encodedInventory);
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(rawData);
            BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(byteArrayInputStream);

            int itemsCount = bukkitObjectInputStream.readInt();
            ItemStack[] items = new ItemStack[itemsCount];

            for (int i = 0; i < itemsCount; i++) {
                items[i] = (ItemStack) bukkitObjectInputStream.readObject();
            }

            bukkitObjectInputStream.close();
            byteArrayInputStream.close();

            data.remove(namespacedKey);
            player.getInventory().setContents(items);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
