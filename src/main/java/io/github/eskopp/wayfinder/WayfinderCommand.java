package io.github.eskopp.wayfinder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class WayfinderCommand implements CommandExecutor, TabCompleter {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final List<String> SUBCOMMANDS = List.of("set", "tp", "list", "delete", "tpa", "accept", "deny", "help");

    private final WayfinderPlugin plugin;
    private final WaypointStore store;
    private final TpaRequests requests;

    public WayfinderCommand(WayfinderPlugin plugin, WaypointStore store, TpaRequests requests) {
        this.plugin = plugin;
        this.store = store;
        this.requests = requests;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            handleHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set" -> handleSet(player, args);
            case "tp" -> handleTeleport(player, args);
            case "list" -> handleList(player);
            case "delete" -> handleDelete(player, args);
            case "tpa" -> handleTpa(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "help" -> handleHelp(player);
            default -> {
                handleHelp(player);
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
        if (!plugin.getConfig().getBoolean("teleport-enabled", true)) {
            info(player, "'" + name + "' is at " + target.getWorld().getName() + " "
                    + target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ()
                    + " (teleporting is disabled on this server).");
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
            info(player, "You have no waypoints. Use /wf set <name>.");
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

    private void handleTpa(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("tpa-enabled", true)) {
            error(player, "Teleport requests are disabled on this server.");
            return;
        }
        if (!player.hasPermission("wayfinder.tpa")) {
            error(player, "You are not allowed to send teleport requests.");
            return;
        }
        if (args.length < 2) {
            error(player, "Usage: /wf tpa <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            error(player, "Player '" + args[1] + "' is not online.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            error(player, "You cannot send a request to yourself.");
            return;
        }
        int seconds = plugin.getConfig().getInt("tpa-timeout-seconds", 30);
        requests.create(player.getUniqueId(), target.getUniqueId(), seconds * 1000L);
        info(player, "Request sent to " + target.getName() + ". It expires in " + seconds + " seconds.");
        target.sendMessage(Component.text(player.getName() + " wants to teleport to you. ", NamedTextColor.YELLOW)
                .append(Component.text("[Accept]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/wf accept")))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text("[Deny]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/wf deny"))));
    }

    private void handleAccept(Player player) {
        if (!player.hasPermission("wayfinder.tpa.accept")) {
            error(player, "You are not allowed to accept teleport requests.");
            return;
        }
        UUID requesterId = requests.take(player.getUniqueId()).orElse(null);
        if (requesterId == null) {
            error(player, "You have no pending teleport request.");
            return;
        }
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null) {
            error(player, "The player who asked is no longer online.");
            return;
        }
        requester.teleportAsync(player.getLocation()).thenAccept(ok -> {
            if (ok) {
                info(requester, "Teleported to " + player.getName() + ".");
                info(player, requester.getName() + " teleported to you.");
            } else {
                error(requester, "Teleport was cancelled.");
            }
        });
    }

    private void handleDeny(Player player) {
        UUID requesterId = requests.take(player.getUniqueId()).orElse(null);
        if (requesterId == null) {
            error(player, "You have no pending teleport request.");
            return;
        }
        info(player, "Request denied.");
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null) {
            error(requester, player.getName() + " denied your teleport request.");
        }
    }

    private void handleHelp(Player player) {
        player.sendMessage(Component.text("Wayfinder", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Save places as waypoints and teleport back to them at any time.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("You can also ask another player if you may teleport to them.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Commands:", NamedTextColor.GOLD));
        helpLine(player, "wayfinder.set", "/wf set <name>", "Save your current position");
        helpLine(player, "wayfinder.tp", "/wf tp <name>", "Teleport to a waypoint");
        helpLine(player, "wayfinder.use", "/wf list", "List your waypoints");
        helpLine(player, "wayfinder.delete", "/wf delete <name>", "Delete a waypoint");
        if (plugin.getConfig().getBoolean("tpa-enabled", true)) {
            helpLine(player, "wayfinder.tpa", "/wf tpa <player>", "Ask to teleport to a player");
            helpLine(player, "wayfinder.tpa.accept", "/wf accept", "Accept a teleport request");
            helpLine(player, "wayfinder.tpa.accept", "/wf deny", "Deny a teleport request");
        }
        helpLine(player, "wayfinder.use", "/wf help", "Show this help");
    }

    private void helpLine(Player player, String permission, String usage, String description) {
        if (!player.hasPermission(permission)) {
            return;
        }
        player.sendMessage(Component.text(usage, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.suggestCommand(usage.split(" <")[0]))
                .append(Component.text(" - " + description, NamedTextColor.GRAY)));
    }

    private String requireName(Player player, String[] args) {
        if (args.length < 2) {
            error(player, "Usage: /wf " + args[0].toLowerCase(Locale.ROOT) + " <name>");
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
        if (args.length == 2 && args[0].equalsIgnoreCase("tpa")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
