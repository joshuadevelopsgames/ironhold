package kingdom.smp.structure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Saves and loads {@link IscStructure} files to {@code <world>/ironhold/structures/<name>.isc},
 * with a read-only fallback to the shared game-dir library {@code <gamedir>/ironhold/structures}
 * (so dev-client runs ({@code run/ironhold/structures}) and every world on a server can use one
 * copy of repo-generated structures without per-world copying). Scans/saves/deletes stay
 * world-local. Names are restricted to [a-z0-9_-] to keep them filesystem-safe and easy to type
 * in commands.
 */
public final class IscStorage {
    private static final Pattern SAFE_NAME = Pattern.compile("[a-z0-9_-]{1,64}");
    private static final String EXT = ".isc";

    private IscStorage() {}

    public static Path directory(MinecraftServer server) throws IOException {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("ironhold").resolve("structures");
        Files.createDirectories(dir);
        return dir;
    }

    /** Shared game-dir library, read by {@link #load}/{@link #list} as a fallback. */
    public static Path globalDirectory(MinecraftServer server) throws IOException {
        Path dir = server.getServerDirectory().resolve("ironhold").resolve("structures");
        Files.createDirectories(dir);
        return dir;
    }

    public static Path pathFor(MinecraftServer server, String name) throws IOException {
        if (!SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Name must match [a-z0-9_-]{1,64} (got '" + name + "')");
        }
        return directory(server).resolve(name + EXT);
    }

    public static Path save(MinecraftServer server, String name, IscStructure structure) throws IOException {
        Path path = pathFor(server, name);
        Files.writeString(path, structure.encode(), StandardCharsets.UTF_8);
        return path;
    }

    public static IscStructure load(MinecraftServer server, String name) throws IOException {
        Path path = pathFor(server, name);
        if (!Files.exists(path)) {
            Path global = globalDirectory(server).resolve(name + EXT);
            if (!Files.exists(global)) {
                throw new IOException("No structure named '" + name + "' at " + path + " or " + global);
            }
            path = global;
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        HolderLookup<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        return IscStructure.decode(text, blocks);
    }

    /** World-local only — never deletes from the shared game-dir library. */
    public static boolean delete(MinecraftServer server, String name) throws IOException {
        return Files.deleteIfExists(pathFor(server, name));
    }

    public static List<String> list(MinecraftServer server) throws IOException {
        TreeSet<String> out = new TreeSet<>();
        for (Path dir : new Path[]{ directory(server), globalDirectory(server) }) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(EXT))
                    .map(p -> {
                        String n = p.getFileName().toString();
                        return n.substring(0, n.length() - EXT.length());
                    })
                    .forEach(out::add);
            }
        }
        return List.copyOf(out);
    }
}
