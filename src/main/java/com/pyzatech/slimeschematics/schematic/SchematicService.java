package com.pyzatech.slimeschematics.schematic;

import com.pyzatech.slimeschematics.SlimeSchematicsPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.bukkit.Location;

public final class SchematicService {

    private final SlimeSchematicsPlugin plugin;
    private final Path treesFolder;
    private final Map<String, Clipboard> cache = new ConcurrentHashMap<>();
    /** Last-modified millis of the file backing each cached clipboard (invalidates cache when the file changes on disk). */
    private final Map<String, Long> cacheFileMtimeMillis = new ConcurrentHashMap<>();

    public SchematicService(SlimeSchematicsPlugin plugin) {
        this.plugin = plugin;
        this.treesFolder = plugin.getDataFolder().toPath().resolve("trees");
    }

    public Path treesFolder() {
        return treesFolder;
    }

    public void clearCache() {
        cache.clear();
        cacheFileMtimeMillis.clear();
    }

    public Optional<Clipboard> loadClipboard(String schematicId) throws IOException {
        String safeId = schematicId.trim();
        if (safeId.isEmpty()) {
            return Optional.empty();
        }
        if (safeId.contains("..") || safeId.indexOf('/') >= 0 || safeId.indexOf('\\') >= 0) {
            throw new IOException("Invalid schematic id.");
        }
        Path file = treesFolder.resolve(safeId + ".schem");
        if (!Files.isRegularFile(file)) {
            invalidate(safeId);
            return Optional.empty();
        }
        Path absolute = file.toAbsolutePath().normalize();
        if (Files.size(absolute) == 0L) {
            invalidate(safeId);
            throw new IOException("Schematic file is empty: " + absolute);
        }
        long mtimeMillis = fileMtimeMillis(absolute);
        Clipboard cached = cache.get(safeId);
        Long cachedMtime = cacheFileMtimeMillis.get(safeId);
        if (cached != null && cachedMtime != null && cachedMtime == mtimeMillis) {
            return Optional.of(cached);
        }
        Clipboard loaded = readClipboard(absolute);
        long postReadMtime = fileMtimeMillis(absolute);
        cache.put(safeId, loaded);
        cacheFileMtimeMillis.put(safeId, postReadMtime);
        return Optional.of(loaded);
    }

    private static Clipboard readClipboard(Path file) throws IOException {
        IOException last = null;

        ClipboardFormat detected = ClipboardFormats.findByFile(file.toFile());
        if (detected == null) {
            detected = sniffFormatByStream(file);
        }
        if (detected != null) {
            try {
                return readWithFormat(detected, file);
            } catch (Exception ex) {
                last = unpackIo(ex);
            }
        }

        for (BuiltInClipboardFormat candidate : spongeFormatsNewestFirst()) {
            try {
                return readWithFormat(candidate, file);
            } catch (Exception ex) {
                last = chainLast(last, unpackIo(ex));
            }
        }

        for (String alias : new String[] {"sponge", "schem", "sponge.3", "sponge.2", "sponge.1"}) {
            ClipboardFormat byAlias = ClipboardFormats.findByAlias(alias);
            if (byAlias == null) {
                continue;
            }
            try {
                return readWithFormat(byAlias, file);
            } catch (Exception ex) {
                last = chainLast(last, unpackIo(ex));
            }
        }

        for (ClipboardFormat cf : ClipboardFormats.getAll()) {
            try {
                return readWithFormat(cf, file);
            } catch (Exception ex) {
                last = chainLast(last, unpackIo(ex));
            }
        }

        if (last != null) {
            throw new IOException(schematicReadUserMessage(file, last), last);
        }
        throw new IOException("Unknown schematic format for file: " + file);
    }

    private static Clipboard readWithFormat(ClipboardFormat format, Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            ClipboardReader reader = format.getReader(in);
            if (reader == null) {
                throw new IOException("No reader for format: " + format.getName());
            }
            return reader.read();
        }
    }

    private static String schematicReadUserMessage(Path file, IOException last) {
        String base = "Could not read schematic: " + file + " (" + last.getMessage() + ")";
        if (isTruncatedOrCorruptGzip(last)) {
            return base
                    + " The file looks truncated or corrupt (unfinished save, bad copy, or an older SlimeSchematics build that did not close the schematic writer)."
                    + " Delete plugins/SlimeSchematics/trees/"
                    + file.getFileName()
                    + " then run //copy and /sst save <id> again (or paste a valid .schem from //schem save).";
        }
        return base;
    }

    private static boolean isTruncatedOrCorruptGzip(Throwable t) {
        while (t != null) {
            if (t instanceof EOFException) {
                return true;
            }
            String m = t.getMessage();
            if (m != null && (m.contains("ZLIB") || m.contains("truncat") || m.contains("Unexpected end"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static IOException unpackIo(Exception ex) {
        if (ex instanceof IOException) {
            return (IOException) ex;
        }
        return new IOException(ex);
    }

    private static IOException chainLast(IOException last, IOException next) {
        if (last == null) {
            return next;
        }
        last.addSuppressed(next);
        return last;
    }

    /**
     * Newer WorldEdit exposes {@code ClipboardFormats.findByInputStream}; older 7.2.x does not.
     */
    private static ClipboardFormat sniffFormatByStream(Path file) {
        try {
            Method m = ClipboardFormats.class.getMethod("findByInputStream", Supplier.class);
            @SuppressWarnings("unchecked")
            ClipboardFormat found = (ClipboardFormat) m.invoke(null, (Supplier<InputStream>) () -> {
                try {
                    return Files.newInputStream(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return found;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Runtime enum may include SPONGE_V3 / V2 / V1 (WE 7.3+) or only SPONGE_SCHEMATIC (WE 7.2).
     */
    private static List<BuiltInClipboardFormat> spongeFormatsNewestFirst() {
        List<BuiltInClipboardFormat> sponges = new ArrayList<>();
        for (BuiltInClipboardFormat f : BuiltInClipboardFormat.values()) {
            String n = f.name();
            if (n.startsWith("SPONGE")) {
                sponges.add(f);
            }
        }
        sponges.sort(Comparator.comparingInt((BuiltInClipboardFormat f) -> spongeFormatPriority(f.name())).reversed());
        return sponges;
    }

    private static int spongeFormatPriority(String enumName) {
        if (enumName.contains("V3")) {
            return 3;
        }
        if (enumName.contains("V2")) {
            return 2;
        }
        if (enumName.contains("V1")) {
            return 1;
        }
        if ("SPONGE_SCHEMATIC".equals(enumName)) {
            return 0;
        }
        return -1;
    }

    /**
     * Sponge schematic format used for {@code /sst save}. Prefers v3 when the runtime WorldEdit enum has it.
     */
    private static BuiltInClipboardFormat spongeSaveFormat() {
        for (String name : new String[] {"SPONGE_V3_SCHEMATIC", "SPONGE_V2_SCHEMATIC", "SPONGE_SCHEMATIC"}) {
            try {
                return BuiltInClipboardFormat.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // not present on this WorldEdit version
            }
        }
        return BuiltInClipboardFormat.SPONGE_SCHEMATIC;
    }

    public void invalidate(String schematicId) {
        String id = schematicId.trim();
        cache.remove(id);
        cacheFileMtimeMillis.remove(id);
    }

    private static long fileMtimeMillis(Path absolute) throws IOException {
        FileTime t = Files.getLastModifiedTime(absolute);
        return t.toMillis();
    }

    public void paste(
            org.bukkit.World bukkitWorld,
            Location pasteOrigin,
            Clipboard clipboard,
            int yawDegrees
    ) throws Exception {
        World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 to = BlockVector3.at(
                pasteOrigin.getBlockX(),
                pasteOrigin.getBlockY(),
                pasteOrigin.getBlockZ()
        );

        ClipboardHolder holder = new ClipboardHolder(clipboard);
        holder.setTransform(new AffineTransform().rotateY(yawDegrees));

        try (EditSession editSession =
                WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1)) {
            Operations.complete(
                    holder.createPaste(editSession).to(to).ignoreAirBlocks(false).build());
        }
    }

    public void saveClipboardFromWorldEditPlayer(
            org.bukkit.entity.Player player,
            String schematicId,
            Optional<RememberedAnchor> rememberedAnchor
    ) throws IOException {
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
        com.sk89q.worldedit.LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        ClipboardHolder holder;
        try {
            holder = session.getClipboard();
        } catch (Exception ex) {
            throw new IOException("No WorldEdit clipboard found. Use //copy first.", ex);
        }
        Clipboard clipboard = holder.getClipboard();
        if (rememberedAnchor.isPresent()) {
            AnchorClipboard.patchClipboard(player, clipboard, rememberedAnchor.get());
        }

        Files.createDirectories(treesFolder);
        String safeId = schematicId.trim();
        if (safeId.isEmpty() || safeId.contains("..") || safeId.indexOf('/') >= 0 || safeId.indexOf('\\') >= 0) {
            throw new IOException("Invalid schematic id.");
        }
        Path finalPath = treesFolder.resolve(safeId + ".schem");
        String tmpName = "." + safeId + "." + ThreadLocalRandom.current().nextLong() + "." + UUID.randomUUID() + ".tmp";
        Path tempPath = treesFolder.resolve(tmpName);

        BuiltInClipboardFormat saveFormat = spongeSaveFormat();
        try (OutputStream output = Files.newOutputStream(tempPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                ClipboardWriter writer = saveFormat.getWriter(output)) {
            writer.write(clipboard);
        } catch (Throwable writeErr) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // best effort cleanup
            }
            if (writeErr instanceof IOException) {
                throw (IOException) writeErr;
            }
            if (writeErr instanceof RuntimeException) {
                throw writeErr;
            }
            throw new IOException(writeErr);
        }

        try {
            readWithFormat(saveFormat, tempPath);
        } catch (Exception verifyErr) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // best effort cleanup
            }
            throw new IOException(
                    "The schematic was written but could not be read back on this server ("
                            + verifyErr.getMessage()
                            + "). If this persists, save with WorldEdit (//schem save) and copy the file into "
                            + treesFolder.toAbsolutePath()
                            + ".",
                    verifyErr
            );
        }

        try {
            try {
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException moveErr) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // ignore
            }
            throw moveErr;
        }

        invalidate(safeId);
        plugin.getLogger().info("Saved schematic '" + safeId + "' to " + finalPath.toAbsolutePath());
    }
}
