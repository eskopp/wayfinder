package io.github.eskopp.wayfinder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class WaypointStore {

    private final WayfinderPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Location>> waypoints = new LinkedHashMap<>();

    public WaypointStore(WayfinderPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "waypoints.yml");
    }

    public void load() {
        waypoints.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuidKey : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid player id: " + uuidKey);
                continue;
            }
            ConfigurationSection entries = players.getConfigurationSection(uuidKey);
            if (entries == null) {
                continue;
            }
            Map<String, Location> own = new LinkedHashMap<>();
            for (String name : entries.getKeys(false)) {
                ConfigurationSection s = entries.getConfigurationSection(name);
                if (s == null) {
                    continue;
                }
                String worldName = s.getString("world", "");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Unknown world '" + worldName + "' for waypoint '" + name + "'");
                    continue;
                }
                own.put(name, new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                        (float) s.getDouble("yaw"), (float) s.getDouble("pitch")));
            }
            waypoints.put(uuid, own);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Location>> player : waypoints.entrySet()) {
            for (Map.Entry<String, Location> entry : player.getValue().entrySet()) {
                Location l = entry.getValue();
                String path = "players." + player.getKey() + "." + entry.getKey();
                yaml.set(path + ".world", l.getWorld().getName());
                yaml.set(path + ".x", l.getX());
                yaml.set(path + ".y", l.getY());
                yaml.set(path + ".z", l.getZ());
                yaml.set(path + ".yaw", l.getYaw());
                yaml.set(path + ".pitch", l.getPitch());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save waypoints: " + e.getMessage());
        }
    }

    public Map<String, Location> of(UUID player) {
        return waypoints.getOrDefault(player, Collections.emptyMap());
    }

    public Set<String> names(UUID player) {
        return new TreeSet<>(of(player).keySet());
    }

    public boolean set(UUID player, String name, Location location) {
        Map<String, Location> own = waypoints.computeIfAbsent(player, k -> new LinkedHashMap<>());
        boolean existed = own.containsKey(name);
        own.put(name, location.clone());
        save();
        return existed;
    }

    public boolean remove(UUID player, String name) {
        Map<String, Location> own = waypoints.get(player);
        if (own == null || own.remove(name) == null) {
            return false;
        }
        save();
        return true;
    }
}
