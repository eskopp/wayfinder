package io.github.eskopp.wayfinder;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class WayfinderPlugin extends JavaPlugin {

    private WaypointStore store;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new WaypointStore(this);
        store.load();

        PluginCommand command = getCommand("wf");
        if (command == null) {
            getLogger().severe("Command 'wf' is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        TpaRequests requests = new TpaRequests();
        WayfinderCommand handler = new WayfinderCommand(this, store, requests);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                requests.clear(event.getPlayer().getUniqueId());
            }
        }, this);
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
    }
}
