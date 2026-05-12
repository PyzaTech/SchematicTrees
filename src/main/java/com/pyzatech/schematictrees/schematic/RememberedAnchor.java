package com.pyzatech.schematictrees.schematic;

import java.util.Objects;

/**
 * Block and world position captured before {@code /schematictrees save} so the clipboard can be patched and
 * its paste origin set to that block (the sapling / trunk base you looked at when running {@code /schematictrees remember-anchor}).
 */
public final class RememberedAnchor {

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final String blockDataAsString;

    public RememberedAnchor(String worldName, int x, int y, int z, String blockDataAsString) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockDataAsString = Objects.requireNonNull(blockDataAsString, "blockDataAsString");
    }

    public String worldName() {
        return worldName;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public String blockDataAsString() {
        return blockDataAsString;
    }
}
