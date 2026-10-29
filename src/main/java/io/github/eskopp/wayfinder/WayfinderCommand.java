package io.github.eskopp.wayfinder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class WayfinderCommand implements CommandExecutor, TabCompleter {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final List<String> SUBCOMMANDS = List.of("set", "tp", "list", "delete");

    private final WayfinderPlugin plugin;
    private final WaypointStore store;

    public WayfinderCommand(WayfinderPlugin plugin, WaypointStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set" -> handleSet(player, args);
            case "tp" -> handleTeleport(player, args);
            case "list" -> handleList(player);
            case "delete" -> handleDelete(player, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleSet(Player player, String[] args) {
        if (!player.hasPermission("wayfinder.set")) {
            error(player, "You are not allowed to create waypoints.");
            return;
        }
        String name = requireName(player, args);
        if (name == null) {
            return;
        }
        int max = plugin.getConfig().getInt("max-waypoints", 10);
        boolean exists = store.of(player.getUniqueId()).containsKey(name);
        if (!exists && store.of(player.getUniqueId()).size() >= max) {
            error(player, "You have reached the limit of " + max + " waypoints.");
            return;
        }
        store.set(player.getUniqueId(), name, player.getLocation());
        info(player, (exists ? "Updated waypoint '" : "Saved waypoint '") + name + "'.");
    }

    private void handleTeleport(Player player, String[] args) {
        if (!player.hasPermission("wayfinder.tp")) {
            error(player, "You are not allowed to teleport to waypoints.");
            return;
        }
        String name = requireName(player, args);
        if (name == null) {
            return;
        }
        Location target = store.of(player.getUniqueId()).get(name);
        if (target == null) {
            error(player, "No waypoint named '" + name + "'.");
            return;
        }
        player.teleportAsync(target).thenAccept(ok -> {
            if (ok) {
                info(player, "Teleported to '" + name + "'.");
            } else {
                error(player, "Teleport was cancelled.");
            }
        });
    }

    private void handleList(Player player) {
        var names = store.names(player.getUniqueId());
        if (names.isEmpty()) {
            info(player, "You have no waypoints. Use /wayfinder set <name>.");
            return;
        }
        info(player, "Your waypoints (" + names.size() + "):");
        for (String name : names) {
            Location l = store.of(player.getUniqueId()).get(name);
            player.sendMessage(Component.text(" - " + name + " ("
                    + l.getWorld().getName() + " "
                    + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ() + ")", NamedTextColor.GRAY));
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("wayfinder.delete")) {
            error(player, "You are not allowed to delete waypoints.");
            return;
        }
        String name = requireName(player, args);
        if (name == null) {
            return;
        }
        if (store.remove(player.getUniqueId(), name)) {
            info(player, "Deleted waypoint '" + name + "'.");
        } else {
            error(player, "No waypoint named '" + name + "'.");
        }
    }

    private String requireName(Player player, String[] args) {
        if (args.length < 2) {
            error(player, "Usage: /wayfinder " + args[0].toLowerCase(Locale.ROOT) + " <name>");
            return null;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (!VALID_NAME.matcher(name).matches()) {
            error(player, "Names may only contain a-z, 0-9, '_' and '-' (max. 32 characters).");
            return null;
        }
        return name;
    }

    private void info(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private void error(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && List.of("tp", "delete").contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return store.names(player.getUniqueId()).stream().filter(n -> n.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
