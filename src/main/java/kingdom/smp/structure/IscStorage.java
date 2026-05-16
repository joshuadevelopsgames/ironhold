package kingdom.smp.structure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Saves and loads {@link IscStructure} files to {@code <world>/ironhold/structures/<name>.isc}.
 * Names are restricted to [a-z0-9_-] to keep them filesystem-safe and easy to type in commands.
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
        if (!Files.exists(path)) throw new IOException("No structure named '" + name + "' at " + path);
        String text = Files.readString(path, StandardCharsets.UTF_8);
        HolderLookup<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        return IscStructure.decode(text, blocks);
    }

    public static boolean delete(MinecraftServer server, String name) throws IOException {
        return Files.deleteIfExists(pathFor(server, name));
    }

    public static List<String> list(MinecraftServer server) throws IOException {
        Path dir = directory(server);
        List<String> out = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(EXT))
                .map(p -> {
                    String n = p.getFileName().toString();
                    return n.substring(0, n.length() - EXT.length());
                })
                .sorted()
                .forEach(out::add);
        }
        return out;
    }
}
