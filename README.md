# SchematicTrees

![Minecraft](https://img.shields.io/badge/Minecraft-1.13.2_--_1.21.11-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen)

SchematicTrees is a basic 1.13.2 to 1.21.x Minecraft plugin for Spigot and Paper that replaces sapling tree growth with custom WorldEdit schematics you drop in `plugins/SchematicTrees/trees/`

## Features

- **WorldEdit schematic trees:** Map each Bukkit `TreeType` to one schematic id or a list so every growth can roll a random tree shape, with optional random yaw and paste offset in `config.yml`.
- **Optional preserve mode:** With `preserve-existing-blocks-on-paste: true` (legacy key `preserve-solid-terrain` still works if the new key is omitted), pasted trees skip cells where the world already has blocks that should stay (detected with [XSeries](https://github.com/CryptoMorin/XSeries)), while air, grass, leaves, flowers, and similar still get replaced. Preserve mode applies the schematic with **Bukkit** `setBlockData` (not a WorldEdit `EditSession` paste) so **FastAsyncWorldEdit cannot bypass** the mask. The release jar embeds XSeries via Maven Shade. Set `debug-messages: true` and reload: you get a paste summary line every growth, and per-cell mask lines when preserve mode is on.
- **Low performance footprint:** The plugin only hooks `StructureGrowEvent` and pastes when a mapping exists; no heavy tick tasks.
- **User-friendly:** In-game commands to save from `//copy`, set pools (`/schtrees set`, `/schtrees setoak`), remember a trunk block before copy (`/schtrees remember-anchor`), and reload after edits. Shorter aliases: `/st` and `/schematictrees`.

## Getting Started

### Prerequisites

- Minecraft server running Spigot or Paper 1.13.2 to 1.21.x, Java 11 or newer, plus WorldEdit 7.2.9+ or FastAsyncWorldEdit (FAWE).

### Installation

1. **Download the Plugin:**
   Download the latest version of SchematicTrees from the [Releases](https://github.com/PizzaThatCodes/SchematicTrees/releases) page, or build it with `mvn clean package` and use `target/SchematicTrees-1.0.1.jar`.

2. **Install the Plugin:**
   Install WorldEdit or FAWE if needed, then place the `SchematicTrees` jar in your server's `plugins` directory.

3. **Restart the Server:**
   Restart your Minecraft server to load the plugin and create `plugins/SchematicTrees/config.yml` and the `trees/` folder. If you already have `config.yml` and `trees/` under a different plugin folder from an older install, copy them into `plugins/SchematicTrees/`.

### Tutorial: make one custom tree (end to end)

Follow these steps once on your server to replace one vanilla tree shape with your own schematic.

1. **Join with admin and WorldEdit**
   - You need `schematictrees.admin` (ops have it by default). If you still have LuckPerms nodes that use the old permission id `slimeschematics.admin`, the plugin still accepts that string, but prefer renaming them to `schematictrees.admin`.

2. **Build or pick a tree in the world**
   - Build the tree you want, or select an existing custom tree. You will copy it with WorldEdit.

3. **Select the tree in WorldEdit**
   - Use `//wand` (or your usual tool), then set positions so the full tree is inside the cuboid. Include the block you want to sit on the sapling (usually the bottom log or dirt under the trunk).

4. **Remember the anchor (strongly recommended)**
   - **Look at the block where the sapling will sit** (usually the bottom log or dirt under the trunk). Run `/schtrees remember-anchor` with no extra arguments. It always uses the solid block you are looking at (crosshair), within range.
   - You should see a confirmation with coordinates. If you skip this step, paste uses WorldEdit’s normal `//copy` origin (often near `//pos1`), which usually does not match the sapling.

5. **Copy to the WorldEdit clipboard**
   - Run `//copy`. The plugin reads from your clipboard on save, not from the selection directly.

6. **Save the schematic file**
   - Run `/schtrees save <id>` with a simple name, for example `/schtrees save my_oak_a`.
   - That writes `plugins/SchematicTrees/trees/my_oak_a.schem`. The remembered anchor (if any) is baked into that file for correct alignment.

7. **Map a `TreeType` to your id**
   - Vanilla decides a Bukkit `TreeType` when the sapling grows (for example oak saplings can become `TREE`, `BIG_TREE`, or `SWAMP`, not a single “OAK” type).
   - **Spruce:** there is no `SPRUCE` name in the API. Spruce saplings use `REDWOOD`, `TALL_REDWOOD`, and `MEGA_REDWOOD`. Use `/schtrees setspruce my_spruce_a` to map all three at once, or set them individually with `/schtrees set REDWOOD ...`, etc.
   - In game, either:
     - `/schtrees set <TreeType> my_oak_a` for one type, or
     - `/schtrees setoak my_oak_a` to point oak-shaped types (`TREE`, `BIG_TREE`, `SWAMP`) at the same schematic in one command.
   - Or edit `config.yml` under `trees:` (see comments in that file), then run `/schtrees reload`.

8. **Reload if you only changed files on disk**
   - After hand-editing `config.yml` or dropping a `.schem` into `trees/`, run `/schtrees reload` so the server picks it up.

9. **Test in game**
   - Plant the matching sapling type, use bone meal or wait for natural growth. The vanilla tree should be cancelled and your schematic pasted instead (with random yaw if that is enabled in `config.yml`).
   - Use `/schtrees status` to confirm WorldEdit is detected and `/schtrees list` to see current mappings.

**Tips**

- If the trunk leaves a hole at the sapling, either copy with the log still there (WorldEdit `//toggleplace` helps) or set `fill-anchor-if-air` in `config.yml` to a log material name.
- Schematic ids must be safe filenames: no slashes, no `..`. The file must be named exactly `<id>.schem` under `plugins/SchematicTrees/trees/`.

### Commands and Permissions

- **Commands:** `/schematictrees` with aliases `/st` and `/schtrees` (short for schematic trees; replaces the old `sst` alias). Subcommands: `help`, `reload`, `status`, `list`, `set`, `setoak`, `clearoak`, `setspruce`, `clearspruce`, `clear`, `remember-anchor`, `forget-anchor`, and `save`. Run `/schtrees help` for usage.
- **Permission:** `schematictrees.admin` (default: op).

## Contributing

We welcome contributions from the community! To contribute:

1. Fork the repository.
2. Create a new branch with your feature or bugfix.
3. Submit a pull request.

## License

SchematicTrees is released under the [MIT License](LICENSE). You may use, modify, and distribute it freely; keep the copyright notice in copies or substantial portions of the code.

## Contact

For support and inquiries, visit [PyzaTech.com](https://PyzaTech.com) or open an issue on GitHub.

---

Thank you for using SchematicTrees! I hope it enhances your Minecraft server experience.
