package io.github.eskopp.wayfinder;

import org.bukkit.command.PluginCommand;
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
        WayfinderCommand handler = new WayfinderCommand(this, store);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
    }
}
