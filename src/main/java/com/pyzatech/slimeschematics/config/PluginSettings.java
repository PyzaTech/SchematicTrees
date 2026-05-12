package com.pyzatech.slimeschematics.config;

import com.pyzatech.slimeschematics.SlimeSchematicsPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {

    private final SlimeSchematicsPlugin plugin;

    private Set<String> enabledWorldsLower = Collections.emptySet();
    private boolean replaceNaturalGrowth = true;
    private boolean replaceBoneMealGrowth = true;
    private boolean randomYaw = true;
    private List<Integer> yawChoicesDegrees = Arrays.asList(0, 90, 180, 270);
    private int pasteOffsetX;
    private int pasteOffsetY;
    private int pasteOffsetZ;
    private boolean debugMessages;
    /** When set, after a successful paste the anchor block is set to this if it is still air (fixes schematics saved with a temporary hole at the trunk). */
    private Material fillAnchorIfAir;
    private final Map<String, List<String>> treeTypeToSchematicIds = new HashMap<>();

    public PluginSettings(SlimeSchematicsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadFromDisk() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        List<String> worlds = cfg.getStringList("settings.enabled-worlds");
        if (worlds == null || worlds.isEmpty()) {
            this.enabledWorldsLower = Collections.emptySet();
        } else {
            Set<String> tmp = new HashSet<>();
            for (String w : worlds) {
                if (w == null || w.isBlank()) {
                    continue;
                }
                tmp.add(w.toLowerCase(Locale.ROOT));
            }
            this.enabledWorldsLower = Collections.unmodifiableSet(tmp);
        }

        this.replaceNaturalGrowth = cfg.getBoolean("settings.replace-natural-growth", true);
        this.replaceBoneMealGrowth = cfg.getBoolean("settings.replace-bone-meal-growth", true);
        this.randomYaw = cfg.getBoolean("settings.random-yaw", true);
        this.yawChoicesDegrees = cfg.getIntegerList("settings.yaw-choices-degrees");
        if (this.yawChoicesDegrees == null || this.yawChoicesDegrees.isEmpty()) {
            this.yawChoicesDegrees = Collections.singletonList(0);
        }
        this.pasteOffsetX = cfg.getInt("settings.paste-offset.x", 0);
        this.pasteOffsetY = cfg.getInt("settings.paste-offset.y", 0);
        this.pasteOffsetZ = cfg.getInt("settings.paste-offset.z", 0);
        this.debugMessages = cfg.getBoolean("settings.debug-messages", false);

        this.fillAnchorIfAir = null;
        String fillAnchorRaw = cfg.getString("settings.fill-anchor-if-air");
        if (fillAnchorRaw != null && !fillAnchorRaw.isBlank()) {
            Material mat = Material.matchMaterial(fillAnchorRaw.trim(), false);
            if (mat == null) {
                plugin.getLogger().warning("Unknown material for settings.fill-anchor-if-air: " + fillAnchorRaw);
            } else {
                this.fillAnchorIfAir = mat;
            }
        }

        this.treeTypeToSchematicIds.clear();
        ConfigurationSection treesSection = cfg.getConfigurationSection("trees");
        if (treesSection != null) {
            for (String key : treesSection.getKeys(false)) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                String typeKey = key.trim().toUpperCase(Locale.ROOT);
                Object raw = treesSection.get(key);
                List<String> ids = parseSchematicIdList(raw);
                if (!ids.isEmpty()) {
                    this.treeTypeToSchematicIds.put(typeKey, Collections.unmodifiableList(ids));
                }
            }
        }
    }

    private static List<String> parseSchematicIdList(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            return s.isEmpty() ? Collections.emptyList() : Collections.singletonList(s);
        }
        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String part = o.toString().trim();
                if (!part.isEmpty()) {
                    out.add(part);
                }
            }
            return Collections.unmodifiableList(out);
        }
        return Collections.emptyList();
    }

    public boolean isWorldEnabled(String worldName) {
        if (enabledWorldsLower.isEmpty()) {
            return true;
        }
        return enabledWorldsLower.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public boolean replaceNaturalGrowth() {
        return replaceNaturalGrowth;
    }

    public boolean replaceBoneMealGrowth() {
        return replaceBoneMealGrowth;
    }

    public boolean randomYaw() {
        return randomYaw;
    }

    public List<Integer> yawChoicesDegrees() {
        return yawChoicesDegrees;
    }

    public int pasteOffsetX() {
        return pasteOffsetX;
    }

    public int pasteOffsetY() {
        return pasteOffsetY;
    }

    public int pasteOffsetZ() {
        return pasteOffsetZ;
    }

    public boolean debugMessages() {
        return debugMessages;
    }

    /**
     * When present, after a successful schematic paste the block at the paste anchor is set to this material
     * if it is still an air-like block (AIR, CAVE_AIR, VOID_AIR when available).
     */
    public Optional<Material> fillAnchorIfAir() {
        return Optional.ofNullable(fillAnchorIfAir);
    }

    public List<String> schematicIdsFor(TreeType type) {
        List<String> ids = treeTypeToSchematicIds.get(type.name());
        return ids == null ? Collections.emptyList() : ids;
    }

    public Map<String, List<String>> treeMappingsView() {
        return Collections.unmodifiableMap(new HashMap<>(treeTypeToSchematicIds));
    }

    public void setMapping(TreeType type, List<String> schematicIds) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "trees." + type.name();
        List<String> cleaned = new ArrayList<>();
        for (String id : schematicIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            cleaned.add(id.trim());
        }
        if (cleaned.isEmpty()) {
            cfg.set(path, null);
        } else if (cleaned.size() == 1) {
            cfg.set(path, cleaned.get(0));
        } else {
            cfg.set(path, cleaned);
        }
        plugin.saveConfig();
        reloadFromDisk();
    }

    public void setOakLikeSchematics(List<String> schematicIds) {
        FileConfiguration cfg = plugin.getConfig();
        List<String> cleaned = new ArrayList<>();
        for (String id : schematicIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            cleaned.add(id.trim());
        }
        Object yamlValue;
        if (cleaned.isEmpty()) {
            yamlValue = null;
        } else if (cleaned.size() == 1) {
            yamlValue = cleaned.get(0);
        } else {
            yamlValue = cleaned;
        }
        for (String typeName : new String[] {"TREE", "BIG_TREE", "SWAMP"}) {
            try {
                TreeType t = TreeType.valueOf(typeName);
                cfg.set("trees." + t.name(), yamlValue);
            } catch (IllegalArgumentException ignored) {
                // Older API stubs may omit some constants; skip safely.
            }
        }
        plugin.saveConfig();
        reloadFromDisk();
    }

    public void clearMapping(TreeType type) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("trees." + type.name(), null);
        plugin.saveConfig();
        reloadFromDisk();
    }

    public void clearOakLikeMappings() {
        FileConfiguration cfg = plugin.getConfig();
        for (String typeName : new String[] {"TREE", "BIG_TREE", "SWAMP"}) {
            try {
                TreeType t = TreeType.valueOf(typeName);
                cfg.set("trees." + t.name(), null);
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }
        plugin.saveConfig();
        reloadFromDisk();
    }

    /** Spruce saplings use Bukkit names REDWOOD, TALL_REDWOOD, MEGA_REDWOOD (there is no TreeType SPRUCE). */
    public void setSpruceLikeSchematics(List<String> schematicIds) {
        FileConfiguration cfg = plugin.getConfig();
        List<String> cleaned = new ArrayList<>();
        for (String id : schematicIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            cleaned.add(id.trim());
        }
        Object yamlValue;
        if (cleaned.isEmpty()) {
            yamlValue = null;
        } else if (cleaned.size() == 1) {
            yamlValue = cleaned.get(0);
        } else {
            yamlValue = cleaned;
        }
        for (String typeName : new String[] {"REDWOOD", "TALL_REDWOOD", "MEGA_REDWOOD"}) {
            try {
                TreeType t = TreeType.valueOf(typeName);
                cfg.set("trees." + t.name(), yamlValue);
            } catch (IllegalArgumentException ignored) {
                // Older API stubs may omit some constants; skip safely.
            }
        }
        plugin.saveConfig();
        reloadFromDisk();
    }

    public void clearSpruceLikeMappings() {
        FileConfiguration cfg = plugin.getConfig();
        for (String typeName : new String[] {"REDWOOD", "TALL_REDWOOD", "MEGA_REDWOOD"}) {
            try {
                TreeType t = TreeType.valueOf(typeName);
                cfg.set("trees." + t.name(), null);
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }
        plugin.saveConfig();
        reloadFromDisk();
    }
}
