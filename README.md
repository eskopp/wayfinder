# Wayfinder

A small Paper plugin for Minecraft 26.3. Players save personal waypoints and teleport back to them.

## Commands

| Command | Description |
|---|---|
| `/wf set <name>` | Save your current position as a waypoint |
| `/wf tp <name>` | Teleport to one of your waypoints |
| `/wf list` | List your waypoints |
| `/wf delete <name>` | Delete a waypoint |
| `/wf tp <x> <y> <z> [world]` | Teleport to coordinates (needs `wayfinder.tp.coords`, operators only by default) |
| `/wf tpa <player>` | Ask to teleport to another online player |
| `/wf tpahere <player>` | Ask another online player to teleport to you |
| `/wf accept` | Accept the pending request. For `tpa` the sender is teleported to you, for `tpahere` you are teleported to the sender |
| `/wf deny` | Deny the pending request |
| `/wf help` | Show the commands you are allowed to use (also shown by `/wf` without arguments) |

Names may contain `a-z`, `0-9`, `_` and `-` (up to 32 characters). Waypoints are stored per player and work across dimensions.

The command is `/wf`, with `/wayfinder` as a longer alias. The plugin registers no other names, so it does not shadow commands such as `/home`, `/warp`, `/tpa` or the vanilla `/waypoint`. Paper also exposes the prefixed form `/wayfinder:wf`, which keeps working if another plugin already uses `/wf` or `/wayfinder`.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `wayfinder.use` | everyone | Use the command |
| `wayfinder.set` | everyone | Create waypoints |
| `wayfinder.tp` | everyone | Teleport to waypoints |
| `wayfinder.delete` | everyone | Delete waypoints |
| `wayfinder.tp.coords` | operators | Teleport to coordinates |
| `wayfinder.tpa` | everyone | Ask to teleport to a player |
| `wayfinder.tpahere` | everyone | Ask a player to teleport to you |
| `wayfinder.tpa.accept` | everyone | Accept or deny teleport requests |

## Configuration

`plugins/Wayfinder/config.yml`:

```yaml
max-waypoints: 10
teleport-enabled: true
tpa-enabled: true
tpa-timeout-seconds: 30
```

With `teleport-enabled: false`, `/wf tp <name>` only prints the coordinates of the waypoint and does not teleport the player. With `tpa-enabled: false`, `tpa` and `tpahere` requests between players are disabled. A request expires after `tpa-timeout-seconds`. Each player can have one pending request at a time, and a new one replaces the old one. Requests are dropped when either player leaves. Changes to the configuration take effect after a server restart.

Waypoints are saved in `plugins/Wayfinder/waypoints.yml`.

## Build

Requires Java 25.

```bash
./gradlew build
```

The plugin jar is written to `build/libs/`. Copy it to the `plugins` folder of a Paper 26.3 server.

## Releases

Publishing a GitHub release builds the plugin and attaches the jar to it. The jar version is taken from the release tag, so the tag `v0.2.0` produces `Wayfinder-0.2.0.jar`.

## License

GPL-3.0. See [LICENSE](LICENSE).
