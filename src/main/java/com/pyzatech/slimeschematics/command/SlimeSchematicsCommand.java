package com.pyzatech.slimeschematics.command;

import com.pyzatech.slimeschematics.SlimeSchematicsPlugin;
import com.pyzatech.slimeschematics.config.PluginSettings;
import com.pyzatech.slimeschematics.schematic.AnchorCapture;
import com.pyzatech.slimeschematics.schematic.RememberedAnchor;
import com.pyzatech.slimeschematics.schematic.SchematicService;
import com.pyzatech.slimeschematics.worldedit.WorldEditBridge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.TreeType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SlimeSchematicsCommand implements CommandExecutor, TabCompleter {

    private final PluginSettings settings;
    private final SchematicService schematics;
    private final SlimeSchematicsPlugin plugin;

    public SlimeSchematicsCommand(PluginSettings settings, SchematicService schematics, SlimeSchematicsPlugin plugin) {
        this.settings = settings;
        this.schematics = schematics;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slimeschematics.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0 || equalsAnyIgnoreCase(args[0], "help", "?")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            settings.reloadFromDisk();
            schematics.clearCache();
            sender.sendMessage(ChatColor.GREEN + "Reloaded SlimeSchematics config and schematic cache.");
            return true;
        }
        if ("status".equals(sub)) {
            boolean we = WorldEditBridge.isWorldEditPresent();
            sender.sendMessage((we ? ChatColor.GREEN : ChatColor.RED) + "WorldEdit available: " + we);
            sender.sendMessage(ChatColor.GRAY + "Trees folder: " + schematics.treesFolder().toAbsolutePath());
            sender.sendMessage(ChatColor.GRAY + "Mappings: " + settings.treeMappingsView().size());
            return true;
        }
        if ("list".equals(sub)) {
            Map<String, List<String>> map = settings.treeMappingsView();
            if (map.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No TreeType mappings are configured yet.");
                return true;
            }
            sender.sendMessage(ChatColor.GOLD + "TreeType -> schematic pool (random pick each growth)");
            map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sender.sendMessage(
                            ChatColor.GRAY + e.getKey() + " -> " + String.join(", ", e.getValue())));
            return true;
        }
        if ("set".equals(sub)) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " set <TreeType> <schematicId> [<schematicId> ...]");
                return true;
            }
            TreeType type = parseTreeType(args[1]);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown TreeType: " + args[1]);
                maybeSpruceHint(sender, args[1]);
                return true;
            }
            List<String> ids = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
            settings.setMapping(type, ids);
            sender.sendMessage(ChatColor.GREEN + "Set " + type.name() + " -> pool of " + ids.size() + ": "
                    + String.join(", ", ids));
            return true;
        }
        if ("setoak".equals(sub)) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setoak <schematicId> [<schematicId> ...]");
                sender.sendMessage(ChatColor.GRAY + "Sets TREE, BIG_TREE, and SWAMP (oak-shaped types) to the same random pool.");
                return true;
            }
            List<String> ids = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
            settings.setOakLikeSchematics(ids);
            sender.sendMessage(ChatColor.GREEN + "Set oak pool (TREE, BIG_TREE, SWAMP) -> " + ids.size()
                    + " id(s): " + String.join(", ", ids));
            return true;
        }
        if ("clearoak".equals(sub)) {
            settings.clearOakLikeMappings();
            sender.sendMessage(ChatColor.GREEN + "Cleared TREE, BIG_TREE, and SWAMP mappings.");
            return true;
        }
        if ("setspruce".equals(sub)) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " setspruce <schematicId> [<schematicId> ...]");
                sender.sendMessage(ChatColor.GRAY + "Sets REDWOOD, TALL_REDWOOD, and MEGA_REDWOOD (spruce in Bukkit) to the same pool.");
                return true;
            }
            List<String> ids = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
            settings.setSpruceLikeSchematics(ids);
            sender.sendMessage(ChatColor.GREEN + "Set spruce pool (REDWOOD, TALL_REDWOOD, MEGA_REDWOOD) -> " + ids.size()
                    + " id(s): " + String.join(", ", ids));
            return true;
        }
        if ("clearspruce".equals(sub)) {
            settings.clearSpruceLikeMappings();
            sender.sendMessage(ChatColor.GREEN + "Cleared REDWOOD, TALL_REDWOOD, and MEGA_REDWOOD mappings.");
            return true;
        }
        if ("clear".equals(sub)) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " clear <TreeType>");
                return true;
            }
            TreeType type = parseTreeType(args[1]);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown TreeType: " + args[1]);
                maybeSpruceHint(sender, args[1]);
                return true;
            }
            settings.clearMapping(type);
            sender.sendMessage(ChatColor.GREEN + "Cleared mapping for " + type.name() + ".");
            return true;
        }
        if ("remember-anchor".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Players only.");
                return true;
            }
            Player player = (Player) sender;
            if (!WorldEditBridge.isWorldEditPresent()) {
                sender.sendMessage(ChatColor.RED + "WorldEdit is required.");
                return true;
            }
            if (args.length >= 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " remember-anchor (look at the sapling block, then run this)");
                return true;
            }
            try {
                RememberedAnchor anchor = AnchorCapture.captureLookTarget(player);
                plugin.setRememberedAnchor(player.getUniqueId(), anchor);
                sender.sendMessage(ChatColor.GREEN + "Remembered anchor at " + anchor.x() + ", " + anchor.y() + ", " + anchor.z()
                        + " (" + anchor.blockDataAsString() + "). Run //copy then /" + label + " save <id>; that block becomes the paste origin.");
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Could not remember anchor: " + ex.getMessage());
            }
            return true;
        }
        if ("forget-anchor".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Players only.");
                return true;
            }
            plugin.clearRememberedAnchor(((Player) sender).getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Cleared remembered anchor for your next save.");
            return true;
        }
        if ("save".equals(sub)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Players only.");
                return true;
            }
            Player player = (Player) sender;
            if (!WorldEditBridge.isWorldEditPresent()) {
                sender.sendMessage(ChatColor.RED + "WorldEdit is required to save from a clipboard.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " save <schematicId>");
                return true;
            }
            Optional<RememberedAnchor> remembered = plugin.getRememberedAnchor(player.getUniqueId());
            try {
                schematics.saveClipboardFromWorldEditPlayer(player, args[1], remembered);
                remembered.ifPresent(r -> plugin.clearRememberedAnchor(player.getUniqueId()));
                sender.sendMessage(ChatColor.GREEN + "Saved schematic '" + args[1].trim() + "'.");
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Save failed: " + ex.getMessage());
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Try /" + label + " help");
        return true;
    }

    private static void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "SlimeSchematics commands");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " reload");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " status");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " list");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " set <TreeType> <id> [<id> ...] (random pool)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " setoak <id> [<id> ...] (TREE, BIG_TREE, SWAMP)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " clearoak (clears those three)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " setspruce <id> [<id> ...] (REDWOOD, TALL_REDWOOD, MEGA_REDWOOD)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " clearspruce (clears those three)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " clear <TreeType>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " remember-anchor (look at sapling block, then //copy, then save)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " forget-anchor");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " save <schematicId> (needs //copy)");
    }

    private static boolean equalsAnyIgnoreCase(String value, String... options) {
        for (String o : options) {
            if (value.equalsIgnoreCase(o)) {
                return true;
            }
        }
        return false;
    }

    private static TreeType parseTreeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TreeType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void maybeSpruceHint(CommandSender sender, String attempted) {
        if (attempted == null) {
            return;
        }
        if ("SPRUCE".equals(attempted.trim().toUpperCase(Locale.ROOT))) {
            sender.sendMessage(ChatColor.GRAY + "There is no TreeType SPRUCE. Use REDWOOD, TALL_REDWOOD, MEGA_REDWOOD, or /sst setspruce <id>.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("slimeschematics.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return partial(
                    Arrays.asList(
                            "help", "reload", "status", "list", "set", "setoak", "clearoak", "setspruce", "clearspruce",
                            "clear", "remember-anchor", "forget-anchor", "save"
                    ),
                    args[0]
            );
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("set".equals(sub) || "clear".equals(sub)) {
                return partial(allTreeTypeNames(), args[1]);
            }
        }
        return Collections.emptyList();
    }

    /** Uses runtime enum constants so newer Minecraft versions still tab-complete new TreeTypes. */
    private static List<String> allTreeTypeNames() {
        List<String> names = new ArrayList<>();
        for (Object constant : TreeType.class.getEnumConstants()) {
            names.add(((Enum<?>) constant).name());
        }
        return names;
    }

    private static List<String> partial(List<String> options, String token) {
        String t = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(t)) {
                out.add(o);
            }
        }
        return out;
    }
}
