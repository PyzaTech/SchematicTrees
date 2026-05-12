package com.pyzatech.slimeschematics.listener;

import com.pyzatech.slimeschematics.SlimeSchematicsPlugin;
import com.pyzatech.slimeschematics.config.PluginSettings;
import com.pyzatech.slimeschematics.schematic.SchematicService;
import com.pyzatech.slimeschematics.worldedit.WorldEditBridge;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

public final class TreeGrowListener implements Listener {

    private final SlimeSchematicsPlugin plugin;
    private final PluginSettings settings;
    private final SchematicService schematics;

    public TreeGrowListener(SlimeSchematicsPlugin plugin, PluginSettings settings, SchematicService schematics) {
        this.plugin = plugin;
        this.settings = settings;
        this.schematics = schematics;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!WorldEditBridge.isWorldEditPresent()) {
            return;
        }
        if (!settings.isWorldEnabled(event.getWorld().getName())) {
            return;
        }
        if (event.isFromBonemeal() && !settings.replaceBoneMealGrowth()) {
            return;
        }
        if (!event.isFromBonemeal() && !settings.replaceNaturalGrowth()) {
            return;
        }

        TreeType type = event.getSpecies();
        List<String> pool = settings.schematicIdsFor(type);
        if (pool.isEmpty()) {
            return;
        }

        String schematicId = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        Optional<Clipboard> clipboard;
        try {
            clipboard = schematics.loadClipboard(schematicId);
        } catch (Exception ex) {
            schematics.invalidate(schematicId);
            plugin.getLogger().log(Level.WARNING, "Failed to load schematic '" + schematicId + "': " + ex.getMessage(), ex);
            return;
        }
        if (clipboard.isEmpty()) {
            if (settings.debugMessages()) {
                plugin.getLogger().info("No schematic file for id '" + schematicId + "' (type " + type + ").");
            }
            return;
        }

        event.setCancelled(true);

        Location origin = event.getLocation().clone();
        origin.add(settings.pasteOffsetX(), settings.pasteOffsetY(), settings.pasteOffsetZ());

        Block saplingBlock = event.getLocation().getBlock();
        saplingBlock.setBlockData(org.bukkit.Material.AIR.createBlockData(), false);

        int yaw = pickYawDegrees();
        try {
            schematics.paste(event.getWorld(), origin, clipboard.get(), yaw);
            maybeFillAirAnchor(origin);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to paste schematic '" + schematicId + "': " + ex.getMessage());
        }
    }

    /**
     * If {@code settings.fill-anchor-if-air} is set, replaces air-like blocks at the paste anchor so schematics
     * saved with a temporary hole at the trunk still grow a solid base.
     */
    private void maybeFillAirAnchor(Location pasteOrigin) {
        settings.fillAnchorIfAir().ifPresent(fill -> {
            Block block = pasteOrigin.getBlock();
            if (isAirLike(block.getType())) {
                block.setType(fill, false);
            }
        });
    }

    private static boolean isAirLike(Material type) {
        if (type == Material.AIR || type == Material.CAVE_AIR) {
            return true;
        }
        return "VOID_AIR".equals(type.name());
    }

    private int pickYawDegrees() {
        if (!settings.randomYaw()) {
            return 0;
        }
        List<Integer> choices = settings.yawChoicesDegrees();
        if (choices == null || choices.isEmpty()) {
            return 0;
        }
        return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
    }
}
