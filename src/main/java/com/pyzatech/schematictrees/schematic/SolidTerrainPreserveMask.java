package com.pyzatech.schematictrees.schematic;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XTag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.math.BlockVector3;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Destination mask for schematic paste: returns true where the paste may replace the world block.
 * When preserve mode is on, existing terrain and structures that XSeries treats as non-replaceable stay intact;
 * air, fluids, fire, flowers, leaves, saplings, crops, kelp, seagrass, and other replaceable plants still get replaced.
 */
public final class SolidTerrainPreserveMask implements Mask {

    private static final int MAX_DEBUG_LINES = 400;

    private final World world;
    private final boolean logDecisions;
    private final Logger log;
    private final AtomicInteger debugLineCounter = new AtomicInteger(0);

    public SolidTerrainPreserveMask(World world) {
        this(world, false, null);
    }

    public SolidTerrainPreserveMask(World world, boolean logDecisions, Logger log) {
        this.world = world;
        this.logDecisions = logDecisions;
        this.log = log;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        Block block = world.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
        OverwriteVerdict verdict = evaluate(block);
        if (logDecisions && log != null) {
            int n = debugLineCounter.incrementAndGet();
            if (n <= MAX_DEBUG_LINES) {
                String dataStr = blockDataSummary(block);
                log.info("[SchematicTrees preserve-mask] "
                        + vector.getBlockX() + "," + vector.getBlockY() + "," + vector.getBlockZ()
                        + " material=" + block.getType()
                        + " data=" + dataStr
                        + " -> " + (verdict.allowOverwrite ? "PASTE" : "SKIP")
                        + " (" + verdict.reason + ")");
            } else if (n == MAX_DEBUG_LINES + 1) {
                log.info("[SchematicTrees preserve-mask] further lines omitted for this paste (cap " + MAX_DEBUG_LINES + ")");
            }
        }
        return verdict.allowOverwrite;
    }

    private static String blockDataSummary(Block block) {
        try {
            return block.getBlockData().getAsString(true);
        } catch (Throwable t) {
            return "?";
        }
    }

    /**
     * {@code true} when the schematic block should be written over this existing material.
     * Uses XSeries instead of {@link Material#isSolid()} so behavior stays correct on modern Paper builds.
     */
    public static boolean allowsSchematicToOverwrite(Material type) {
        return evaluate(type).allowOverwrite;
    }

    static OverwriteVerdict evaluate(Block block) {
        return evaluate(block.getType());
    }

    private static OverwriteVerdict evaluate(Material type) {
        if (XBlock.isAir(type)) {
            return new OverwriteVerdict(true, "XBlock.isAir");
        }
        if (XBlock.isWater(type) || XBlock.isLava(type)) {
            return new OverwriteVerdict(true, "XBlock.isWaterOrLava");
        }
        if (type.name().equals("BUBBLE_COLUMN")) {
            return new OverwriteVerdict(true, "BUBBLE_COLUMN");
        }
        if (type == Material.SEAGRASS || type == Material.TALL_SEAGRASS) {
            return new OverwriteVerdict(true, "seagrass");
        }
        if (type == Material.SNOW) {
            return new OverwriteVerdict(true, "SNOW");
        }

        XMaterial x = XMaterial.matchXMaterial(type);
        if (x == null) {
            return new OverwriteVerdict(false, "preserve:XMaterial.matchXMaterial=null");
        }

        if (XTag.FLUID.isTagged(x)) {
            return new OverwriteVerdict(true, "XTag.FLUID");
        }
        if (XTag.FIRE.isTagged(x)) {
            return new OverwriteVerdict(true, "XTag.FIRE");
        }
        if (XTag.REPLACEABLE_PLANTS.isTagged(x)) {
            return new OverwriteVerdict(true, "XTag.REPLACEABLE_PLANTS");
        }
        if (XTag.SMALL_FLOWERS.isTagged(x) || XTag.TALL_FLOWERS.isTagged(x) || XTag.FLOWERS.isTagged(x)) {
            return new OverwriteVerdict(true, "XTag.FLOWERS");
        }
        if (XTag.LEAVES.isTagged(x)) {
            return new OverwriteVerdict(true, "XTag.LEAVES");
        }
        if (XTag.SAPLINGS.isTagged(x)) {
            return new OverwriteVerdict(true, "XTag.SAPLINGS");
        }
        if (XTag.CROPS.isTagged(x)) {
            return new OverwriteVerdict(true, "XTag.CROPS");
        }
        return new OverwriteVerdict(false, "preserve:no-tag (xMaterial=" + x + ")");
    }

    static final class OverwriteVerdict {
        final boolean allowOverwrite;
        final String reason;

        OverwriteVerdict(boolean allowOverwrite, String reason) {
            this.allowOverwrite = allowOverwrite;
            this.reason = reason;
        }
    }

    @Override
    public Mask2D toMask2D() {
        return null;
    }
}
