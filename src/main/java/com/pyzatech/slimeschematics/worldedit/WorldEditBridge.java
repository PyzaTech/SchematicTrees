package com.pyzatech.slimeschematics.worldedit;

import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Bukkit;

public final class WorldEditBridge {

    private WorldEditBridge() {
    }

    public static boolean isWorldEditPresent() {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null
                && Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
            return false;
        }
        try {
            WorldEdit.getInstance();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
