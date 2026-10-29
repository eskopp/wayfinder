# Wayfinder

A small Paper plugin for Minecraft 26.3. Players save personal waypoints and teleport back to them.

## Commands

| Command | Description |
|---|---|
| `/wayfinder set <name>` | Save your current position as a waypoint |
| `/wayfinder tp <name>` | Teleport to one of your waypoints |
| `/wayfinder list` | List your waypoints |
| `/wayfinder delete <name>` | Delete a waypoint |

Names may contain `a-z`, `0-9`, `_` and `-` (up to 32 characters). Waypoints are stored per player and work across dimensions.

The plugin registers only `/wayfinder` and no aliases, so it does not shadow commands such as `/home`, `/warp`, `/tpa` or the vanilla `/waypoint`. Paper also exposes it as `/wayfinder:wayfinder`.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `wayfinder.use` | everyone | Use the command |
| `wayfinder.set` | everyone | Create waypoints |
| `wayfinder.tp` | everyone | Teleport to waypoints |
| `wayfinder.delete` | everyone | Delete waypoints |

## Configuration

`plugins/Wayfinder/config.yml`:

```yaml
max-waypoints: 10
```

Waypoints are saved in `plugins/Wayfinder/waypoints.yml`.

## Build

Requires Java 25.

```bash
./gradlew build
```

The plugin jar is written to `build/libs/`. Copy it to the `plugins` folder of a Paper 26.3 server.

## License

GPL-3.0. See [LICENSE](LICENSE).
