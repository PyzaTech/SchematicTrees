# SlimeSchematics

![Minecraft](https://img.shields.io/badge/Minecraft-1.13.2_--_1.21.11-brightgreen)
![License](https://img.shields.io/badge/License-See%20repository-lightgrey)
![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen)

SlimeSchematics is a basic 1.13.2 to 1.21.x Minecraft plugin for Spigot and Paper that replaces sapling tree growth with custom WorldEdit schematics you drop in `plugins/SlimeSchematics/trees/`

## Features

- **WorldEdit schematic trees:** Map each Bukkit `TreeType` to one schematic id or a list so every growth can roll a random tree shape, with optional random yaw and paste offset in `config.yml`.
- **Low performance footprint:** The plugin only hooks `StructureGrowEvent` and pastes when a mapping exists; no heavy tick tasks.
- **User-friendly:** In-game commands to save from `//copy`, set pools (`/sst set`, `/sst setoak`), remember a trunk block before copy (`/sst remember-anchor`), and reload after edits.

## Getting Started

### Prerequisites

- Minecraft server running Spigot or Paper 1.13.2 to 1.21.x, Java 11 or newer, plus WorldEdit 7.2.9+ or FastAsyncWorldEdit (FAWE).

### Installation

1. **Download the Plugin:**
   Download the latest version of SlimeSchematics from the [Releases](https://github.com/PizzaThatCodes/SchematicTrees/releases) page, or build it with `mvn clean package` and use `target/SlimeSchematics-1.0.0.jar`.

2. **Install the Plugin:**
   Install WorldEdit or FAWE if needed, then place the `SlimeSchematics` jar in your server's `plugins` directory.

3. **Restart the Server:**
   Restart your Minecraft server to load the plugin and create `plugins/SlimeSchematics/config.yml` and the `trees/` folder.

### Tutorial: make one custom tree (end to end)

Follow these steps once on your server to replace one vanilla tree shape with your own schematic.

1. **Join with admin and WorldEdit**
   - You need `slimeschematics.admin` (ops have it by default) and a working WorldEdit or FAWE session.

2. **Build or pick a tree in the world**
   - Build the tree you want, or select an existing custom tree. You will copy it with WorldEdit.

3. **Select the tree in WorldEdit**
   - Use `//wand` (or your usual tool), then set positions so the full tree is inside the cuboid. Include the block you want to sit on the sapling (usually the bottom log or dirt under the trunk).

4. **Remember the anchor (strongly recommended)**
   - **Look at the block where the sapling will sit** (usually the bottom log or dirt under the trunk). Run `/sst remember-anchor` with no extra arguments. It always uses the solid block you are looking at (crosshair), within range.
   - You should see a confirmation with coordinates. If you skip this step, paste uses WorldEdit’s normal `//copy` origin (often near `//pos1`), which usually does not match the sapling.

5. **Copy to the WorldEdit clipboard**
   - Run `//copy`. The plugin reads from your clipboard on save, not from the selection directly.

6. **Save the schematic file**
   - Run `/sst save <id>` with a simple name, for example `/sst save my_oak_a`.
   - That writes `plugins/SlimeSchematics/trees/my_oak_a.schem`. The remembered anchor (if any) is baked into that file for correct alignment.

7. **Map a `TreeType` to your id**
   - Vanilla decides a Bukkit `TreeType` when the sapling grows (for example oak saplings can become `TREE`, `BIG_TREE`, or `SWAMP`, not a single “OAK” type).
   - **Spruce:** there is no `SPRUCE` name in the API. Spruce saplings use `REDWOOD`, `TALL_REDWOOD`, and `MEGA_REDWOOD`. Use `/sst setspruce my_spruce_a` to map all three at once, or set them individually with `/sst set REDWOOD ...`, etc.
   - In game, either:
     - `/sst set <TreeType> my_oak_a` for one type, or
     - `/sst setoak my_oak_a` to point oak-shaped types (`TREE`, `BIG_TREE`, `SWAMP`) at the same schematic in one command.
   - Or edit `config.yml` under `trees:` (see comments in that file), then run `/sst reload`.

8. **Reload if you only changed files on disk**
   - After hand-editing `config.yml` or dropping a `.schem` into `trees/`, run `/sst reload` so the server picks it up.

9. **Test in game**
   - Plant the matching sapling type, use bone meal or wait for natural growth. The vanilla tree should be cancelled and your schematic pasted instead (with random yaw if that is enabled in `config.yml`).
   - Use `/sst status` to confirm WorldEdit is detected and `/sst list` to see current mappings.

**Tips**

- If the trunk leaves a hole at the sapling, either copy with the log still there (WorldEdit `//toggleplace` helps) or set `fill-anchor-if-air` in `config.yml` to a log material name.
- Schematic ids must be safe filenames: no slashes, no `..`. The file must be named exactly `<id>.schem` under `plugins/SlimeSchematics/trees/`.

### Commands and Permissions

- **Commands:** `/slimeschematics` (aliases `sst`, `slimeschem`) with `help`, `reload`, `status`, `list`, `set`, `setoak`, `clearoak`, `setspruce`, `clearspruce`, `clear`, `remember-anchor`, `forget-anchor`, and `save`. Run `/sst help` for usage.
- **Permission:** `slimeschematics.admin` (default: op).

## Contributing

We welcome contributions from the community! To contribute:

1. Fork the repository.
2. Create a new branch with your feature or bugfix.
3. Submit a pull request.

## License

Add a [LICENSE](LICENSE) file to this repository to publish terms; update this section when you do.

## Contact

For support and inquiries, visit [PyzaTech.com](https://PyzaTech.com) or open an issue on GitHub.

---

Thank you for using SlimeSchematics! I hope it enhances your Minecraft server experience.
