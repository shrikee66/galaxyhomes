# GalaxyHomes

A Paper homes, spawn and TPA plugin targeting **Paper 1.21.11 / Java 21**.

## Build

```bash
mvn clean package
```

The output JAR is created in `target/`. GitHub Actions also builds the project on pushes and pull requests to `main` or `master`; download the JAR from the workflow run's **Artifacts** section.

## Commands

| Command | Description |
|---|---|
| `/sethome [name]` | Save or update a home |
| `/home [name]` | Open the homes inventory or teleport to a named home |
| `/homes` | Open the homes inventory |
| `/delhome <name>` | Delete a home |
| `/spawn` | Teleport to configured spawn |
| `/setspawn` | Set server spawn (operator by default) |
| `/tpa <player>` | Request to teleport to a player |
| `/tpahere <player>` | Request a player to teleport to you |
| `/tpaccept`, `/tpdeny`, `/tpacancel` | Manage TPA requests |
| `/tpatoggle` | Toggle incoming requests |
| `/crossplayhomes <reload|backup|stats>` | Admin tools (`/chomes`, `/galaxyhomes` aliases) |

## Data

Player data is stored per UUID at `plugins/GalaxyHomes/players/<UUID>.yml`. Backups are written to the configured backup folder. Back up the existing data folder before replacing an older plugin build.

## Compatibility notes

Floodgate detection is optional and reflective, so the plugin can load on Java-only servers without Floodgate. The current source includes a text fallback for Bedrock TPA prompts; native Cumulus form menus and advanced home-management GUI actions are not implemented in this build. Validate on a test server before deploying to a live server.
