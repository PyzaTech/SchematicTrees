package com.pyzatech.slimeschematics.schematic;

import java.io.IOException;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class AnchorCapture {

    private AnchorCapture() {
    }

    /**
     * Remembers the solid block the player is looking at (sapling / trunk base). Used as paste origin on {@code /sst save}.
     */
    public static RememberedAnchor captureLookTarget(Player player) throws IOException {
        return fromLook(player);
    }

    @SuppressWarnings("deprecation")
    private static RememberedAnchor fromLook(Player player) throws IOException {
        Block target = player.getTargetBlock(null, 120);
        if (target == null || AnchorClipboard.isAirLikeBlock(target)) {
            throw new IOException("No solid block in sight (within range). Look at the block where the sapling will sit (trunk base).");
        }
        return fromWorldBlock(player, target.getX(), target.getY(), target.getZ());
    }

    private static RememberedAnchor fromWorldBlock(Player player, int bx, int by, int bz) throws IOException {
        Block block = player.getWorld().getBlockAt(bx, by, bz);
        if (AnchorClipboard.isAirLikeBlock(block)) {
            throw new IOException("That block is empty. Look at a solid block (sapling / trunk base).");
        }
        return new RememberedAnchor(
                player.getWorld().getName(),
                bx,
                by,
                bz,
                block.getBlockData().getAsString()
        );
    }
}
