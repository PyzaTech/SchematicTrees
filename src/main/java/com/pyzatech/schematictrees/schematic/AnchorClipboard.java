package com.pyzatech.schematictrees.schematic;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import java.io.IOException;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class AnchorClipboard {

    private AnchorClipboard() {
    }

    public static void patchClipboard(Player player, Clipboard clipboard, RememberedAnchor anchor) throws IOException {
        if (!anchor.worldName().equals(player.getWorld().getName())) {
            throw new IOException("Anchor was saved in world \"" + anchor.worldName()
                    + "\" but you are in \"" + player.getWorld().getName() + "\".");
        }
        BlockVector3 pos = BlockVector3.at(anchor.x(), anchor.y(), anchor.z());
        if (!clipboard.getRegion().contains(pos)) {
            throw new IOException(
                    "Remembered anchor (" + anchor.x() + ", " + anchor.y() + ", " + anchor.z()
                            + ") is outside the current WorldEdit clipboard. Include that block in //pos1//pos2 and //copy.");
        }
        org.bukkit.block.data.BlockData bukkitData;
        try {
            bukkitData = org.bukkit.Bukkit.createBlockData(anchor.blockDataAsString());
        } catch (IllegalArgumentException ex) {
            throw new IOException("Could not parse remembered block data.", ex);
        }
        BlockState weState = BukkitAdapter.adapt(bukkitData);
        if (weState == null) {
            throw new IOException("WorldEdit could not convert the remembered block for the clipboard.");
        }
        try {
            clipboard.setBlock(pos, weState.toBaseBlock());
        } catch (WorldEditException ex) {
            throw new IOException("Could not write remembered block into clipboard: " + ex.getMessage(), ex);
        }
        // Paste aligns the clipboard origin to the sapling; without this, the origin stays whatever //copy used (often near pos1).
        clipboard.setOrigin(pos);
    }

    static boolean isAirLikeBlock(Block block) {
        Material t = block.getType();
        if (t == Material.AIR || t == Material.CAVE_AIR) {
            return true;
        }
        return "VOID_AIR".equals(t.name());
    }
}
