# SlimeSchematics wiki

## Making one custom tree (quick steps)

1. Install SlimeSchematics and WorldEdit (or FAWE) on your Spigot or Paper server. Restart once so `plugins/SlimeSchematics/` and `trees/` exist.

2. Build or choose a tree in the world. Select all of it with WorldEdit (`//pos1`, `//pos2`). Include the block that will match the sapling spot (trunk base or ground under it).

3. **Look at the block the sapling will grow from** (the block that should line up when the tree grows). Run **`/sst remember-anchor`** with nothing after it. You should get a green message with coordinates.

4. Run **`//copy`**.

5. Run **`/sst save <id>`** (example: `/sst save my_oak_a`). That creates `plugins/SlimeSchematics/trees/<id>.schem`.

6. Map a vanilla tree type to that id, for example **`/sst setoak my_oak_a`** for oak-shaped growth, **`/sst setspruce my_spruce_a`** for spruce (Bukkit names `REDWOOD`, `TALL_REDWOOD`, `MEGA_REDWOOD`; there is no `SPRUCE` type), or **`/sst set BIRCH my_oak_a`** for birch. Or edit `config.yml` under `trees:` and run **`/sst reload`**.

7. Plant the right sapling, use bone meal or wait for growth. The schematic should paste with that block on the sapling.

**Note:** `/sst remember-anchor` does not take `selection`, `look`, or `here` anymore. It only uses the block you are looking at.

For full detail (permissions, tips, commands), open the **README** in the main repository root (not every GitHub Wiki view can link to it).
