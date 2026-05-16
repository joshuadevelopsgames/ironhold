package kingdom.smp.block.wardheart;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.UUID;

public class WardheartBlockEntity extends BlockEntity {


    public static final int FUEL_PER_CHARGE = 600;
    public static final int FUEL_DECAY_PER_TICK_OVERCHARGED = 4;
    public static final int FUEL_DECAY_PER_TICK_NORMAL      = 1;
    public static final int FUEL_DECAY_INTERVAL = 20; // 1s

    public static final int OVERCHARGE_TICK_LIMIT = 20 * 60; // 60s before overload
    public static final int PROJECTILE_SCAN_INTERVAL = 2;    // every 2 ticks — still catches arrows (~3 b/tick) within the dome
    public static final int MOB_PUSH_INTERVAL = 4;           // every 4 ticks — mob walk ~0.3 b/tick, plenty responsive at 5 Hz

    private int fuel = 0;
    private WardheartTier cachedTier = WardheartTier.DORMANT;
    private WardheartAccess access = WardheartAccess.MOBS_BLOCKED;
    private UUID ownerId;
    private String ownerName = "";
    private int overchargeTicks = 0;

    /** When true, the wardheart is locked to MICRO tier, never decays, never
     *  overcharges, and has no owner. Used by auto-spawned natural shields
     *  (e.g., the ones around dragon end crystals). */
    private boolean permanent = false;

    // Client-only ripple state (last impact position relative to BE, plus age)
    private float impactX, impactY, impactZ;
    private int impactAge = 9999;
    private static final int IMPACT_LIFETIME = 30;

    // Animated phase 0..1 for client renderer pulse
    private float clientPhase = 0f;

    public WardheartBlockEntity(BlockPos pos, BlockState state) {
        super(Ironhold.WARDHEART_BLOCK_ENTITY.get(), pos, state);
    }

    public int getFuel() { return fuel; }
    public WardheartTier getTier() { return cachedTier; }
    public WardheartAccess getAccess() { return access; }
    public UUID getOwner() { return ownerId; }
    public String getOwnerName() { return ownerName; }

    public void setOwner(UUID id, String name) {
        this.ownerId = id;
        this.ownerName = name == null ? "" : name;
    }

    public void setAccess(WardheartAccess a) {
        this.access = a;
        markUpdate();
    }

    public boolean isPermanent() { return permanent; }

    /** Mark this wardheart as a permanent natural shield (MICRO tier, no decay).
     *  Should be called immediately after the BE is created — e.g., right after
     *  placing the block during dragon-crystal spawn. */
    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
        if (permanent) {
            this.cachedTier = WardheartTier.MICRO;
            this.fuel = 0; // fuel is irrelevant for permanent shields
            this.ownerId = null;
            this.access = WardheartAccess.MOBS_BLOCKED;
        }
        markUpdate();
    }

    public boolean canFeed(Player p) {
        return access != WardheartAccess.OWNER_ONLY || ownerId == null || ownerId.equals(p.getUUID());
    }

    public boolean canConfigure(Player p) {
        return ownerId == null || ownerId.equals(p.getUUID()) || p.isCreative();
    }

    public void addFuel(int amount) {
        int max = WardheartTier.OVERCHARGED.fuelToReach() + 4000;
        this.fuel = Math.min(max, this.fuel + amount);
        recomputeTier();
    }

    private void recomputeTier() {
        // Permanent wardhearts are locked to MICRO regardless of fuel.
        if (permanent) {
            if (cachedTier != WardheartTier.MICRO) {
                cachedTier = WardheartTier.MICRO;
                markUpdate();
            }
            return;
        }
        WardheartTier t = WardheartTier.fromFuel(fuel);
        if (t != cachedTier) {
            WardheartTier prev = cachedTier;
            cachedTier = t;
            if (level != null && !level.isClientSide()) {
                onTierChanged(prev, t);
            }
            markUpdate();
        }
    }

    private void onTierChanged(WardheartTier prev, WardheartTier next) {
        if (level == null || level.isClientSide()) return;
        BlockPos pos = getBlockPos();
        if (next.ordinal() > prev.ordinal()) {
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.1f, 1.4f);
        } else if (next == WardheartTier.DORMANT) {
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.9f);
        } else {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 0.7f);
        }
    }

    /** Permanent shields validate that what they're protecting still exists.
     *  For now, the only natural permanent shield is the one around dragon end
     *  crystals — so we look for an EndCrystal entity within a couple blocks. */
    private boolean hasProtectedTarget(ServerLevel sl) {
        AABB nearby = new AABB(getBlockPos()).inflate(2);
        return !sl.getEntitiesOfClass(
            net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, nearby).isEmpty();
    }

    public Vec3 domeCenter() {
        BlockPos p = getBlockPos();
        return new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
    }

    public boolean isInsideDome(Vec3 point) {
        if (!cachedTier.isActive()) return false;
        double r = cachedTier.radius();
        return point.distanceToSqr(domeCenter()) <= r * r;
    }

    public boolean isAlly(Entity e) {
        if (e instanceof Player p) {
            if (ownerId == null) return access != WardheartAccess.OWNER_ONLY;
            if (ownerId.equals(p.getUUID())) return true;
            return access == WardheartAccess.EVERYONE || access == WardheartAccess.MOBS_BLOCKED;
        }
        // Tameable / villager logic could go here. For now, treat villagers/animals as allies.
        if (e instanceof PathfinderMob pm) {
            return !(pm instanceof Enemy);
        }
        return !(e instanceof Enemy);
    }

    /** Trigger a client-side ripple at this world-space point (called from server via update tag). */
    public void triggerImpact(Vec3 worldPoint) {
        Vec3 c = domeCenter();
        this.impactX = (float) (worldPoint.x - c.x);
        this.impactY = (float) (worldPoint.y - c.y);
        this.impactZ = (float) (worldPoint.z - c.z);
        this.impactAge = 0;
        markUpdate();
    }

    public float getImpactX() { return impactX; }
    public float getImpactY() { return impactY; }
    public float getImpactZ() { return impactZ; }
    public int getImpactAge() { return impactAge; }
    public float getImpactProgress() {
        if (impactAge >= IMPACT_LIFETIME) return -1f;
        return impactAge / (float) IMPACT_LIFETIME;
    }
    public float getClientPhase() { return clientPhase; }

    // ── Tickers ──────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, WardheartBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;

        if (be.permanent) {
            // Permanent shields skip fuel decay and overcharge entirely. They
            // do still need to verify their protected target is alive — if the
            // dragon crystal they wrap was destroyed, clean ourselves up.
            if (level.getGameTime() % 100 == 0 && !be.hasProtectedTarget(sl)) {
                level.removeBlock(pos, false);
                return;
            }
        } else {
            // Fuel decay
            if (level.getGameTime() % FUEL_DECAY_INTERVAL == 0 && be.fuel > 0) {
                int decay = be.cachedTier == WardheartTier.OVERCHARGED
                    ? FUEL_DECAY_PER_TICK_OVERCHARGED * FUEL_DECAY_INTERVAL
                    : FUEL_DECAY_PER_TICK_NORMAL * FUEL_DECAY_INTERVAL;
                be.fuel = Math.max(0, be.fuel - decay);
                be.recomputeTier();
                be.setChanged();
            }

            // Overcharge meltdown
            if (be.cachedTier == WardheartTier.OVERCHARGED) {
                be.overchargeTicks++;
                if (be.overchargeTicks >= OVERCHARGE_TICK_LIMIT) {
                    be.detonate(sl);
                    return;
                }
            } else {
                be.overchargeTicks = Math.max(0, be.overchargeTicks - 1);
            }
        }

        if (!be.cachedTier.isActive()) return;

        // Projectile scan
        if (level.getGameTime() % PROJECTILE_SCAN_INTERVAL == 0) {
            be.scanAndBlockProjectiles(sl);
        }

        // Mob push-out
        if (level.getGameTime() % MOB_PUSH_INTERVAL == 0) {
            be.pushHostileMobs(sl);
        }

        // Idle ambient particles at the dome edge
        if (level.getRandom().nextInt(8) == 0) {
            be.spawnAmbientParticle(sl);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WardheartBlockEntity be) {
        be.clientPhase = (be.clientPhase + 0.012f) % 1.0f;
        if (be.impactAge < 9999) be.impactAge++;
    }

    private void scanAndBlockProjectiles(ServerLevel sl) {
        Vec3 c = domeCenter();
        double r = cachedTier.radius();
        AABB bb = new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
        List<Projectile> projectiles = sl.getEntitiesOfClass(Projectile.class, bb);
        if (projectiles.isEmpty()) return;
        for (Projectile p : projectiles) {
            if (p.isRemoved()) continue;

            Vec3 pos = p.position();
            double dist = pos.distanceTo(c);
            if (dist > r) continue; // Outside the sphere — not our concern this tick

            // Bounce any projectile inside the sphere whose velocity has an inward
            // component (heading deeper into the dome). Outward-bound projectiles
            // are allowed to leave — that's how the dome owner shoots out.
            // No "ally" check on projectiles: a force field deflects everything
            // that hits it, regardless of who fired it.
            Vec3 dir = (dist < 1e-3 ? new Vec3(0, 1, 0) : pos.subtract(c).scale(1.0 / dist));
            Vec3 vel = p.getDeltaMovement();
            double outwardSpeed = vel.dot(dir);
            if (outwardSpeed > 0.0) {
                // Already heading outward — let it leave the dome naturally
                continue;
            }

            // Project current position onto the sphere surface for the visual
            // bounce point — works whether the projectile just crossed the
            // boundary or tunneled a few blocks past it.
            Vec3 surface = c.add(dir.scale(r));
            blockProjectile(sl, p, surface);
        }
    }

    private void blockProjectile(ServerLevel sl, Projectile p, Vec3 surface) {
        sl.sendParticles(ParticleTypes.PORTAL, surface.x, surface.y, surface.z,
            14, 0.25, 0.25, 0.25, 0.5);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, surface.x, surface.y, surface.z,
            10, 0.2, 0.2, 0.2, 0.3);
        sl.playSound(null, surface.x, surface.y, surface.z,
            SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.6f, 1.6f);
        triggerImpact(surface);
        // Drain a small amount of fuel per impact
        this.fuel = Math.max(0, this.fuel - 5);
        recomputeTier();
        setChanged();

        // Arrows that already stuck into a block inside the dome — and any
        // projectile with effectively zero velocity — get disintegrated rather
        // than bounced. Bouncing a stuck arrow doesn't unstick it; the inGround
        // flag would keep it frozen even after we move it. Disintegration reads
        // as "the shield burned the arrow out of existence", which is fine
        // narratively.
        if (p instanceof AbstractArrow || p.getDeltaMovement().lengthSqr() < 0.005) {
            sl.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY(), p.getZ(),
                12, 0.2, 0.2, 0.2, 0.4);
            p.discard();
            return;
        }

        // Reflect the projectile back outward across the surface normal.
        Vec3 n = surface.subtract(domeCenter()).normalize();
        Vec3 vel = p.getDeltaMovement();
        double dot = vel.dot(n);
        // Mirror the inward component, retain ~75% energy
        Vec3 reflected = vel.subtract(n.scale(2 * dot)).scale(0.75);
        // Ensure the bounce always carries the projectile back outward, even
        // for slow-moving things, so it doesn't hover at the boundary.
        double minOutward = 0.35;
        Vec3 outwardComponent = n.scale(reflected.dot(n));
        if (outwardComponent.dot(n) < minOutward) {
            reflected = reflected.add(n.scale(minOutward - outwardComponent.dot(n)));
        }
        p.setDeltaMovement(reflected);
        // Place the projectile just outside the surface so the next tick's scan
        // doesn't immediately catch it again. AbstractHurtingProjectile-style
        // projectiles (fireballs etc.) accelerate along their current velocity
        // each tick, so setDeltaMovement is enough — no need to touch the
        // private acceleration vector.
        p.setPos(surface.x + n.x * 1.5, surface.y + n.y * 1.5, surface.z + n.z * 1.5);
        if (p.getOwner() == null) {
            // Orphaned projectile (shooter despawned) — clean up rather than
            // letting it fly forever.
            p.discard();
        }
    }

    private void pushHostileMobs(ServerLevel sl) {
        Vec3 c = domeCenter();
        double r = cachedTier.radius();
        AABB bb = new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, bb,
            le -> le.isAlive() && !isAlly(le));
        for (LivingEntity le : targets) {
            Vec3 pos = le.position().add(0, le.getBbHeight() * 0.5, 0);
            double dist = pos.distanceTo(c);
            if (dist > r) continue;

            Vec3 dir = (dist < 1e-3) ? new Vec3(0, 1, 0) : pos.subtract(c).scale(1.0 / dist);
            // OVERRIDE the mob's velocity (rather than add to it). A mob sprinting
            // inward at +0.5/tick will resist a +0.6 add — but if we *replace*
            // the inward component with a strong outward velocity, even angered
            // endermen (movement speed ~0.45) can't outpace it.
            Vec3 currentVel = le.getDeltaMovement();
            // Strip out any inward component
            double inward = currentVel.dot(dir);
            Vec3 outwardOnlyVel = (inward < 0) ? currentVel.subtract(dir.scale(inward)) : currentVel;
            // Add a strong outward push that scales with how deep they are inside
            double depthFactor = 1.0 + Math.min(2.0, (r - dist) * 0.1); // deeper = harder shove
            Vec3 push = dir.scale(1.6 * depthFactor);
            Vec3 newVel = outwardOnlyVel.add(push);
            // Always include some upward lift so they pop out cleanly even on flat ground
            le.setDeltaMovement(newVel.x, Math.max(newVel.y, 0.35), newVel.z);
            le.hurtMarked = true;

            // For mobs that are persistently inside (deep penetration), hard-teleport
            // them back to just outside the surface. Endermen with momentum + speed
            // can otherwise bounce around inside before our push catches them.
            if (le instanceof Mob && dist < r * 0.9) {
                Vec3 outsidePos = c.add(dir.scale(r + 2.0));
                le.teleportTo(outsidePos.x, outsidePos.y, outsidePos.z);
            }

            // Damage + impact ripple every 20 ticks so they don't get spammed but
            // do learn to stay out.
            if (le instanceof Mob && sl.getGameTime() % 20 == 0) {
                Vec3 surface = c.add(dir.scale(r));
                sl.sendParticles(ParticleTypes.PORTAL, surface.x, surface.y, surface.z,
                    8, 0.2, 0.2, 0.2, 0.3);
                triggerImpact(surface);
                le.hurtServer(sl, sl.damageSources().magic(), 2.0f);
                this.fuel = Math.max(0, this.fuel - 2);
                recomputeTier();
                setChanged();
            }
        }
    }

    private void spawnAmbientParticle(ServerLevel sl) {
        RandomSource rand = sl.getRandom();
        double r = cachedTier.radius();
        // Random surface point on upper hemisphere
        double theta = rand.nextDouble() * Math.PI * 2.0;
        double phi = Math.acos(rand.nextDouble()); // 0..pi/2
        Vec3 c = domeCenter();
        double x = c.x + r * Math.sin(phi) * Math.cos(theta);
        double y = c.y + r * Math.cos(phi);
        double z = c.z + r * Math.sin(phi) * Math.sin(theta);
        sl.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0, 0, 0, 0.0);
    }

    private void detonate(ServerLevel sl) {
        BlockPos pos = getBlockPos();
        sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            240, 1.0, 1.0, 1.0, 1.0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
            120, 1.5, 0.8, 1.5, 0.5);
        sl.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0f, 0.5f);
        sl.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 0.8f, 1.3f);
        // Knock back nearby entities and apply small damage
        double r = cachedTier.radius();
        AABB bb = new AABB(pos).inflate(r);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, bb)) {
            Vec3 push = le.position().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (push.lengthSqr() < 0.001) push = new Vec3(0, 1, 0);
            push = push.normalize().scale(1.5);
            le.setDeltaMovement(le.getDeltaMovement().add(push.x, push.y + 0.5, push.z));
            le.hurtMarked = true;
        }
        // Wipe out fuel and downgrade
        this.fuel = 0;
        this.overchargeTicks = 0;
        recomputeTier();
        setChanged();
    }

    // ── Persistence & client sync ────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Fuel", fuel);
        out.putInt("OverchargeTicks", overchargeTicks);
        out.putBoolean("Permanent", permanent);
        out.putString("Access", access.getSerializedName());
        if (ownerId != null) {
            out.store("OwnerId", net.minecraft.core.UUIDUtil.CODEC, ownerId);
            out.putString("OwnerName", ownerName == null ? "" : ownerName);
        }
        // Sync fields used for client renderer
        out.putFloat("ImpactX", impactX);
        out.putFloat("ImpactY", impactY);
        out.putFloat("ImpactZ", impactZ);
        out.putInt("ImpactAge", impactAge);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        fuel = in.getIntOr("Fuel", 0);
        overchargeTicks = in.getIntOr("OverchargeTicks", 0);
        permanent = in.getBooleanOr("Permanent", false);
        access = WardheartAccess.byName(in.getStringOr("Access", WardheartAccess.MOBS_BLOCKED.getSerializedName()));
        ownerId = in.read("OwnerId", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        ownerName = in.getStringOr("OwnerName", "");
        impactX = in.getFloatOr("ImpactX", 0f);
        impactY = in.getFloatOr("ImpactY", 0f);
        impactZ = in.getFloatOr("ImpactZ", 0f);
        impactAge = in.getIntOr("ImpactAge", 9999);
        recomputeTier();
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
