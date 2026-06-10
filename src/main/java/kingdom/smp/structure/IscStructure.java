package kingdom.smp.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ironhold Structure Code (ISC) — a compact, human- and AI-readable text encoding
 * of a rectangular block region. Format:
 *
 * <pre>
 * # Ironhold Structure Code v1
 * size 5 4 3
 * palette
 *   00 minecraft:air
 *   01 minecraft:stone
 *   02 minecraft:oak_log[axis=y]
 * body
 * y=0
 *   01 01 01 01 01
 *   01 02 00 02 01
 *   ...
 * y=1
 *   ...
 * </pre>
 *
 * Coordinates: per Y-slice (bottom-up), each line is one Z-row (north→south),
 * each token is one X column (west→east). Palette is 2-char base-36 (0–9, a–z),
 * so up to 1296 unique blockstates per structure. Code {@code 00} is reserved
 * for air. The palette is local to each structure — no global block ID drift.
 */
public record IscStructure(int sizeX, int sizeY, int sizeZ, List<BlockState> palette, int[] data) {

    public static final int VERSION = 1;
    // Raised from 64 for full-fortress builds (dark_castle_v3 is 80x58x80).
    // 128^3 = ~2M ints (~8 MB) in memory and a ~6 MB .isc — still safe to parse + place.
    public static final int MAX_DIM = 128;
    public static final int MAX_VOLUME = 128 * 128 * 128;
    public static final int MAX_PALETTE = 36 * 36;

    public IscStructure {
        if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
            throw new IllegalArgumentException("Size must be >= 1 on every axis (got "
                + sizeX + "x" + sizeY + "x" + sizeZ + ")");
        }
        if (sizeX > MAX_DIM || sizeY > MAX_DIM || sizeZ > MAX_DIM) {
            throw new IllegalArgumentException("Each dimension capped at " + MAX_DIM
                + " (got " + sizeX + "x" + sizeY + "x" + sizeZ + ")");
        }
        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > MAX_VOLUME) {
            throw new IllegalArgumentException("Volume capped at " + MAX_VOLUME
                + " blocks (got " + volume + ")");
        }
        if (data.length != volume) {
            throw new IllegalArgumentException("data length " + data.length
                + " does not match volume " + volume);
        }
        if (palette.isEmpty() || palette.size() > MAX_PALETTE) {
            throw new IllegalArgumentException("Palette size must be 1.." + MAX_PALETTE
                + " (got " + palette.size() + ")");
        }
        palette = List.copyOf(palette);
        data = data.clone();
    }

    public int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    public BlockState at(int x, int y, int z) {
        return palette.get(data[index(x, y, z)]);
    }

    public int volume() {
        return sizeX * sizeY * sizeZ;
    }

    /** Count of non-air blocks. */
    public int solidCount() {
        int n = 0;
        for (int idx : data) {
            if (!palette.get(idx).isAir()) n++;
        }
        return n;
    }

    // ── Encoding ────────────────────────────────────────────────────────────

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Ironhold Structure Code v").append(VERSION).append('\n');
        sb.append("size ").append(sizeX).append(' ').append(sizeY).append(' ').append(sizeZ).append('\n');
        sb.append("palette\n");
        for (int i = 0; i < palette.size(); i++) {
            sb.append("  ").append(code(i)).append(' ').append(serializeState(palette.get(i))).append('\n');
        }
        sb.append("body\n");
        for (int y = 0; y < sizeY; y++) {
            sb.append("y=").append(y).append('\n');
            for (int z = 0; z < sizeZ; z++) {
                sb.append("  ");
                for (int x = 0; x < sizeX; x++) {
                    if (x > 0) sb.append(' ');
                    sb.append(code(data[index(x, y, z)]));
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public static IscStructure decode(String text, HolderLookup<Block> blockLookup) {
        int sizeX = -1, sizeY = -1, sizeZ = -1;
        Map<Integer, BlockState> paletteMap = new LinkedHashMap<>();
        int[] data = null;
        int writeY = -1;
        int writeZ = 0;

        enum Section { HEAD, PALETTE, BODY }
        Section section = Section.HEAD;

        String[] lines = text.split("\\R", -1);
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = stripComment(raw).strip();
            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase("palette")) { section = Section.PALETTE; continue; }
            if (line.equalsIgnoreCase("body")) {
                if (sizeX < 0) throw fail(lineNo, "size header missing before body");
                section = Section.BODY;
                data = new int[sizeX * sizeY * sizeZ];
                continue;
            }

            switch (section) {
                case HEAD -> {
                    if (line.startsWith("size ")) {
                        String[] parts = line.substring(5).trim().split("\\s+");
                        if (parts.length != 3) throw fail(lineNo, "size needs 3 ints");
                        try {
                            sizeX = Integer.parseInt(parts[0]);
                            sizeY = Integer.parseInt(parts[1]);
                            sizeZ = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            throw fail(lineNo, "size has non-integer values: " + line);
                        }
                    }
                    // ignore other head tokens (version line etc.)
                }
                case PALETTE -> {
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length != 2) throw fail(lineNo, "palette line needs '<code> <blockstate>'");
                    int idx = parseCode(parts[0], lineNo);
                    BlockState state = parseState(parts[1], blockLookup, lineNo);
                    paletteMap.put(idx, state);
                }
                case BODY -> {
                    if (line.startsWith("y=")) {
                        try {
                            writeY = Integer.parseInt(line.substring(2).trim());
                            writeZ = 0;
                        } catch (NumberFormatException e) {
                            throw fail(lineNo, "bad y header: " + line);
                        }
                        if (writeY < 0 || writeY >= sizeY) throw fail(lineNo, "y=" + writeY + " out of range");
                    } else {
                        if (writeY < 0) throw fail(lineNo, "body row before any y= header");
                        if (writeZ >= sizeZ) throw fail(lineNo, "too many rows in y=" + writeY);
                        String[] tokens = line.split("\\s+");
                        if (tokens.length != sizeX) {
                            throw fail(lineNo, "row has " + tokens.length + " codes, expected " + sizeX);
                        }
                        for (int x = 0; x < sizeX; x++) {
                            int paletteIdx = parseCode(tokens[x], lineNo);
                            if (!paletteMap.containsKey(paletteIdx)) {
                                throw fail(lineNo, "code " + tokens[x] + " not in palette");
                            }
                            data[(writeY * sizeZ + writeZ) * sizeX + x] = paletteIdx;
                        }
                        writeZ++;
                    }
                }
            }
        }

        if (sizeX < 0 || data == null) throw new IscParseException("missing size or body");

        // Densify the palette: indices in the file may have gaps; rewrite data to use
        // dense indices into the final palette list.
        List<BlockState> ordered = new ArrayList<>(paletteMap.size());
        Map<Integer, Integer> remap = new LinkedHashMap<>();
        int dense = 0;
        for (var entry : paletteMap.entrySet()) {
            remap.put(entry.getKey(), dense++);
            ordered.add(entry.getValue());
        }
        for (int i = 0; i < data.length; i++) {
            data[i] = remap.get(data[i]);
        }
        return new IscStructure(sizeX, sizeY, sizeZ, ordered, data);
    }

    // ── Base-36 codes ───────────────────────────────────────────────────────

    public static String code(int i) {
        if (i < 0 || i >= MAX_PALETTE) throw new IllegalArgumentException("code out of range: " + i);
        char hi = digit(i / 36);
        char lo = digit(i % 36);
        return "" + hi + lo;
    }

    private static char digit(int n) {
        return n < 10 ? (char) ('0' + n) : (char) ('a' + n - 10);
    }

    private static int parseCode(String s, int lineNo) {
        if (s.length() != 2) throw fail(lineNo, "code '" + s + "' must be 2 chars");
        return digitValue(s.charAt(0), lineNo) * 36 + digitValue(s.charAt(1), lineNo);
    }

    private static int digitValue(char c, int lineNo) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'z') return c - 'a' + 10;
        if (c >= 'A' && c <= 'Z') return c - 'A' + 10;
        throw fail(lineNo, "bad base-36 digit: '" + c + "'");
    }

    // ── BlockState serialization ────────────────────────────────────────────

    public static String serializeState(BlockState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if (!state.getProperties().isEmpty()) {
            sb.append('[');
            sb.append(state.getValues()
                .map(v -> v.property().getName() + "=" + v.valueName())
                .collect(Collectors.joining(",")));
            sb.append(']');
        }
        return sb.toString();
    }

    private static BlockState parseState(String input, HolderLookup<Block> blockLookup, int lineNo) {
        try {
            BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(blockLookup, input, false);
            return result.blockState();
        } catch (CommandSyntaxException e) {
            // Don't fail the whole import if a single block ID was removed/renamed —
            // substitute air with a clear notice so the rest of the build still works.
            System.err.println("[Ironhold ISC] line " + lineNo + ": unknown block '"
                + input + "' — substituting air. (" + e.getMessage() + ")");
            return Blocks.AIR.defaultBlockState();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String stripComment(String raw) {
        int hash = raw.indexOf('#');
        return hash < 0 ? raw : raw.substring(0, hash);
    }

    private static IscParseException fail(int lineNo, String msg) {
        return new IscParseException("line " + lineNo + ": " + msg);
    }

    public static final class IscParseException extends RuntimeException {
        public IscParseException(String msg) { super(msg); }
    }
}
