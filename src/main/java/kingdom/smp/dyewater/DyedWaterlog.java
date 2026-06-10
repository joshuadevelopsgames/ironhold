package kingdom.smp.dyewater;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import kingdom.smp.Ironhold;

/**
 * Per-position colour memory for <em>colour-preserving</em> waterlogging.
 *
 * <p>Vanilla waterlogging is a single boolean {@code WATERLOGGED} property and
 * {@code Block.getFluidState(state)} hard-returns plain {@link net.minecraft.world.level.material.Fluids#WATER}
 * — so the contained fluid can never <i>be</i> a dyed fluid. Instead we keep the water plain but remember,
 * per block position, which {@link DyeColor} it was waterlogged from, and tint the water render to match
 * (see {@code DyedWaterClientEvents}'s water-model override).
 *
 * <p>The memory lives in a {@link LevelChunk} {@link AttachmentType}: NeoForge persists it to disk
 * ({@link AttachmentType.Builder#serialize}) and auto-syncs it to tracking clients
 * ({@link AttachmentType.Builder#sync}) — no hand-written networking. Writes/clears go through the
 * universal {@code LevelChunkWaterlogColorMixin}; the colour is read back by the client tint source and by
 * the coloured-bucket pickup ({@code SimpleWaterloggedBlockDyedPickupMixin}).
 */
public final class DyedWaterlog {
    private DyedWaterlog() {}

    /** Mutable per-chunk map of packed {@link BlockPos} → dye id. */
    public static final class Colors {
        final Map<Long, Byte> map;
        public Colors() { this.map = new HashMap<>(); }
        private Colors(Map<Long, Byte> map) { this.map = map; }
    }

    private record Entry(long pos, int color) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
        Codec.intRange(0, 15).fieldOf("color").forGetter(Entry::color)
    ).apply(i, Entry::new));

    public static final Codec<Colors> CODEC = ENTRY_CODEC.listOf().xmap(
        list -> {
            Map<Long, Byte> m = new HashMap<>();
            for (Entry e : list) m.put(e.pos(), (byte) e.color());
            return new Colors(m);
        },
        colors -> colors.map.entrySet().stream()
            .map(en -> new Entry(en.getKey(), en.getValue() & 0xFF)).toList());

    /** Wire-format mirror of {@link #CODEC} for client sync. */
    public static final StreamCodec<FriendlyByteBuf, Colors> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public Colors decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            Map<Long, Byte> m = new HashMap<>(n);
            for (int i = 0; i < n; i++) {
                long pos = buf.readLong();
                m.put(pos, buf.readByte());
            }
            return new Colors(m);
        }

        @Override
        public void encode(FriendlyByteBuf buf, Colors value) {
            buf.writeVarInt(value.map.size());
            for (Map.Entry<Long, Byte> en : value.map.entrySet()) {
                buf.writeLong(en.getKey());
                buf.writeByte(en.getValue());
            }
        }
    };

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Ironhold.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Colors>> COLORS =
        ATTACHMENT_TYPES.register("waterlog_colors", () ->
            AttachmentType.builder((Supplier<Colors>) Colors::new)
                .serialize(CODEC.fieldOf("entries"), colors -> !colors.map.isEmpty())
                .sync(STREAM_CODEC)
                .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    /** Stamp the colour of the water at {@code pos} (bucket placement). Server-side. */
    public static void set(Level level, BlockPos pos, DyeColor color) {
        if (level.isClientSide()) return;
        if (level.getChunk(pos) instanceof LevelChunk chunk) {
            setOnChunk(chunk, pos, color);
        }
    }

    /** Record the colour of the water at {@code pos}. Server-side. */
    public static void setOnChunk(LevelChunk chunk, BlockPos pos, DyeColor color) {
        Colors data = chunk.getData(COLORS.get());
        byte id = (byte) color.getId();
        Byte prev = data.map.put(pos.asLong(), id);
        if (prev == null || prev != id) {
            chunk.setData(COLORS.get(), data); // flag for client re-sync
            chunk.markUnsaved();
        }
    }

    /** Forget the colour at {@code pos} (block removed or un-waterlogged). Server-side. */
    public static void clearOnChunk(LevelChunk chunk, BlockPos pos) {
        Colors data = chunk.getExistingDataOrNull(COLORS.get());
        if (data == null) return;
        if (data.map.remove(pos.asLong()) != null) {
            chunk.setData(COLORS.get(), data);
            chunk.markUnsaved();
        }
    }

    /** The waterlog colour at {@code pos}, or {@code null}. Works on both client and server. */
    public static DyeColor get(LevelReader level, BlockPos pos) {
        ChunkAccess ca = level.getChunk(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getZ()),
            ChunkStatus.FULL, false);
        if (!(ca instanceof LevelChunk chunk)) return null;
        Colors data = chunk.getExistingDataOrNull(COLORS.get());
        if (data == null) return null;
        Byte id = data.map.get(pos.asLong());
        return id == null ? null : DyeColor.byId(id);
    }
}
