package kingdom.smp.block;

import com.mojang.serialization.Codec;
import kingdom.smp.ModEntities;
import kingdom.smp.entity.ButterflyEntity;
import kingdom.smp.entity.ButterflySpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing state for {@link ButterflyJarBlock} — a placeable jar that displays up
 * to {@link #MAX_BUTTERFLIES} captured butterflies. Stores only the species (the
 * visual is reconstructed by {@code ButterflyJarRenderer}); the list is synced to
 * the client so the renderer knows which species to draw, and persists across
 * break/place via the {@code BUTTERFLY_JAR_CONTENTS} data component (see the block).
 */
public class ButterflyJarBlockEntity extends BlockEntity {

    public static final int MAX_BUTTERFLIES = 3;

    private final List<ButterflySpecies> contents = new ArrayList<>(MAX_BUTTERFLIES);

    /** Client-only: one persistent display mob per slot. Each must be a distinct instance
     *  (sharing one across slots/jars freezes GeckoLib's per-instance animation). */
    private ButterflyEntity[] displayMobs;

    /** Hands out unique negative entity ids to display dummies (see {@link #displayMob}). */
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_DUMMY_ID =
        new java.util.concurrent.atomic.AtomicInteger(-1_000_000);

    public ButterflyJarBlockEntity(BlockPos pos, BlockState state) {
        super(kingdom.smp.ModBlocks.BUTTERFLY_TERRARIUM_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * The client-side display butterfly for a given slot (lazily created, world-less, not ticked).
     * Returns null off-client or for an empty slot. Rebuilt automatically if the slot's species changed.
     */
    public ButterflyEntity displayMob(int slot) {
        if (level == null || !level.isClientSide() || slot < 0 || slot >= contents.size()) {
            return null;
        }
        if (displayMobs == null) {
            displayMobs = new ButterflyEntity[MAX_BUTTERFLIES];
        }
        ButterflySpecies species = contents.get(slot);
        ButterflyEntity mob = displayMobs[slot];
        if (mob == null || mob.getSpecies() != species) {
            mob = ModEntities.BUTTERFLY.get().create(level, EntitySpawnReason.LOAD);
            if (mob == null) {
                mob = new ButterflyEntity(ModEntities.BUTTERFLY.get(), level);
            }
            mob.setSpecies(species);
            mob.setNoGravity(true);
            // Unique (negative, so it never clashes with real world entities) id per dummy —
            // GeckoLib keys its animation state by entity id, so dummies sharing id 0 would
            // collide and freeze each other's flutter.
            mob.setId(NEXT_DUMMY_ID.getAndDecrement());
            displayMobs[slot] = mob;
        }
        return mob;
    }

    public List<ButterflySpecies> getContents() {
        return contents;
    }

    public boolean isFull() {
        return contents.size() >= MAX_BUTTERFLIES;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public boolean addButterfly(ButterflySpecies species) {
        if (species == null || isFull()) return false;
        contents.add(species);
        markUpdate();
        return true;
    }

    /** Pop the most-recently-added butterfly (or null if empty). */
    public ButterflySpecies removeLast() {
        if (contents.isEmpty()) return null;
        ButterflySpecies removed = contents.remove(contents.size() - 1);
        markUpdate();
        return removed;
    }

    /** Species ids, for the data component / drops. */
    public List<String> contentIds() {
        List<String> ids = new ArrayList<>(contents.size());
        for (ButterflySpecies s : contents) ids.add(s.id());
        return ids;
    }

    /** Replace contents from a list of species ids (used on placement / load). */
    public void setFromIds(List<String> ids) {
        contents.clear();
        if (ids != null) {
            for (String id : ids) {
                ButterflySpecies s = ButterflySpecies.byId(id);
                if (s != null && contents.size() < MAX_BUTTERFLIES) contents.add(s);
            }
        }
        markUpdate();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!contents.isEmpty()) {
            out.store("Contents", Codec.STRING.listOf(), contentIds());
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        contents.clear();
        in.read("Contents", Codec.STRING.listOf()).ifPresent(ids -> {
            for (String id : ids) {
                ButterflySpecies s = ButterflySpecies.byId(id);
                if (s != null && contents.size() < MAX_BUTTERFLIES) contents.add(s);
            }
        });
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdate() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
