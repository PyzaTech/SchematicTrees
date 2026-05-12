package com.pyzatech.slimeschematics;

import com.pyzatech.slimeschematics.command.SlimeSchematicsCommand;
import com.pyzatech.slimeschematics.config.PluginSettings;
import com.pyzatech.slimeschematics.listener.TreeGrowListener;
import com.pyzatech.slimeschematics.schematic.RememberedAnchor;
import com.pyzatech.slimeschematics.schematic.SchematicService;
import com.pyzatech.slimeschematics.worldedit.WorldEditBridge;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public final class SlimeSchematicsPlugin extends JavaPlugin {

    private PluginSettings settings;
    private SchematicService schematicService;
    private final ConcurrentHashMap<UUID, RememberedAnchor> rememberedAnchors = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            Files.createDirectories(getDataFolder().toPath().resolve("trees"));
        } catch (Exception ex) {
            getLogger().severe("Could not create plugin data folders: " + ex.getMessage());
        }
        this.settings = new PluginSettings(this);
        this.settings.reloadFromDisk();
        this.schematicService = new SchematicService(this);
        if (!WorldEditBridge.isWorldEditPresent()) {
            getLogger().warning(
                    "WorldEdit (or FastAsyncWorldEdit) was not detected. Schematic pasting will stay disabled until it is installed.");
        }
        getServer().getPluginManager().registerEvents(new TreeGrowListener(this, settings, schematicService), this);
        org.bukkit.command.PluginCommand command = getCommand("slimeschematics");
        if (command != null) {
            SlimeSchematicsCommand executor = new SlimeSchematicsCommand(settings, schematicService, this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (schematicService != null) {
            schematicService.clearCache();
        }
    }

    public PluginSettings settings() {
        return settings;
    }

    public SchematicService schematicService() {
        return schematicService;
    }

    public Optional<RememberedAnchor> getRememberedAnchor(UUID playerId) {
        return Optional.ofNullable(rememberedAnchors.get(playerId));
    }

    public void setRememberedAnchor(UUID playerId, RememberedAnchor anchor) {
        rememberedAnchors.put(playerId, anchor);
    }

    public void clearRememberedAnchor(UUID playerId) {
        rememberedAnchors.remove(playerId);
    }
}
