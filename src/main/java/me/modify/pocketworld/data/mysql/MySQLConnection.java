package me.modify.pocketworld.data.mysql;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.Connection;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.exceptions.DataSourceConnectionException;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MySQLConnection implements Connection {

    private PocketWorldPlugin plugin;
    public MySQLConnection(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() throws DataSourceConnectionException {
    }

    @Override
    public void close() {

    }

    @Override
    public DAO getDAO() {
        return null;
    }


}
