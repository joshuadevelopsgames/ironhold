package kingdom.smp;

import java.util.Map;
import kingdom.smp.command.IronholdCommands;
import kingdom.smp.entity.BlackShulkerEntity;
import kingdom.smp.entity.KangarudeEntity;
import kingdom.smp.npc.NpcManifests;
import kingdom.smp.npc.NpcSessionGreetings;
import kingdom.smp.entity.WhiteShulkerEntity;
import kingdom.smp.entity.WillOWispEntity;
import kingdom.smp.game.ClassStatHandler;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.game.RpgXpBarSync;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.world.KingdomWorldData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class IronholdGameEvents {
    private IronholdGameEvents() {}

    /** Sync interval: send RPG data packet every N ticks (not every tick). */
    private static final int SYNC_INTERVAL = 10;

    /** Chance for the smallest (baby) slime to drop a Pink Slime Ball on death. */
    private static final float PINK_SLIME_BALL_DROP_CHANCE = 0.025F;

    /** Drop a Pink Slime Ball 2.5% of the time a baby (smallest) slime dies. */
    @SubscribeEvent
    public static void onBabySlimeDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Slime slime)) return;
        if (slime instanceof MagmaCube) return;
        if (slime.getSize() != 1) return; // size 1 = smallest "baby" slime
        if (slime.getRandom().nextFloat() >= PINK_SLIME_BALL_DROP_CHANCE) return;

        ItemStack drop = new ItemStack(Ironhold.PINK_SLIME_BALL.get());
        event.getDrops().add(new ItemEntity(slime.level(),
            slime.getX(), slime.getY(), slime.getZ(), drop));
    }


    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        IronholdCommands.register(event);
        kingdom.smp.command.SkillsCommand.register(event);
        // TEMP: WIP seasons feature disabled — re-enable when source compiles against current MC API.
        // kingdom.smp.seasons.SeasonsCommand.register(event);
    }

    @SubscribeEvent
    public static void onAddServerReloadListeners(
            net.neoforged.neoforge.event.AddServerReloadListenersEvent event) {
        event.addListener(NpcManifests.LISTENER_ID, new NpcManifests());
    }

    /**
     * Players being haunted by Kangabrine can't sleep — the night holds them.
     * Block bed use with the vanilla "monsters nearby" problem so the message reads
     * naturally, plus a custom system message for flavor.
     */
    @SubscribeEvent
    public static void onPlayerSleepAttempt(net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent event) {
        if (!KangarudeEntity.isBeingHaunted(event.getEntity().getUUID())) return;
        event.setProblem(net.minecraft.world.entity.player.Player.BedSleepingProblem.NOT_SAFE);
        event.getEntity().sendSystemMessage(
            Component.literal("§4§lYou cannot rest. He is watching.")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTravelDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel overworld = player.level().getServer().getLevel(Level.OVERWORLD);
        KingdomWorldData data = overworld.getDataStorage().computeIfAbsent(KingdomWorldData.TYPE);
        ResourceKey<Level> dest = event.getDimension();
        if (dimensionIdEquals(dest, Level.NETHER) && !data.isNetherUnlocked()) {
            event.setCanceled(true);
            player.sendSystemMessage(
                Component.literal(
                    "The Nether is sealed until a kingdom reaches "
                        + KingdomWorldData.NETHER_UNLOCK_XP
                        + " pooled Class XP."));
        } else if (dimensionIdEquals(dest, Level.END) && !data.isEndUnlocked()) {
            event.setCanceled(true);
            player.sendSystemMessage(
                Component.literal(
                    "The End is sealed until a kingdom reaches "
                        + KingdomWorldData.END_UNLOCK_XP
                        + " pooled Class XP."));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NpcSessionGreetings.forgetPlayer(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer sp) {
            kingdom.smp.fishing.FishingMinigameManager.clear(sp);
            kingdom.smp.blacksmithing.BlacksmithingMinigameManager.clear(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        ClassStatHandler.apply(player, rpg);
        RpgXpBarSync.sync(player, rpg);
        // Immediately sync full RPG state to the client for HUD rendering
        ModNetworking.syncToClient(player);
        // Sync profession-skill state for the SkillTreeScreen
        ModNetworking.syncSkillsToClient(player);
        // Sync use-to-level skills for the Practice tab
        ModNetworking.syncUseSkillsToClient(player);
        // Backfill any currently-online Kangarude/Kangabrine tab-list entries
        // so they appear in this player's list immediately on login (rather than
        // waiting for the next entity tick to broadcast).
        kingdom.smp.entity.KangarudePlayerListSync.resendAllTo(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        // Class attribute modifiers are transient, so they're wiped when the
        // player entity is recreated on respawn. Re-apply them so the class's
        // bonus hearts (and other stats) survive death instead of resetting.
        ClassStatHandler.apply(player, rpg);
        // On a death respawn, start at the full class-adjusted max health rather
        // than the vanilla default 20. End-portal returns keep their prior health.
        if (!event.isEndConquered()) {
            player.setHealth(player.getMaxHealth());
        }
        RpgXpBarSync.sync(player, rpg);
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        // Cheap when no-op — only re-applies attribute modifiers if the player
        // has been marked dirty by a class/level change since last apply.
        ClassStatHandler.applyIfDirty(player, rpg);
        EncumbranceHandler.tick(player, ModAttachments.PLAYER_RPG.get());
        RpgXpBarSync.sync(player, rpg);

        // Force pending promotions — re-show the class-selection screen on a
        // throttle until the player picks. Class XP gain is already blocked at
        // the grant site.
        kingdom.smp.game.RpgProgressionActions.reopenPromotionScreenIfNeeded(player);

        // Periodic RPG data sync to client (every SYNC_INTERVAL ticks)
        if (player.tickCount % SYNC_INTERVAL == 0) {
            ModNetworking.syncToClient(player);
            ModNetworking.syncUseSkillsToClient(player);
        }

    }

    /** ~12% chance to slip a piece of fool's gold into the catch. */
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity().level().getRandom().nextFloat() < 0.12f) {
            event.getDrops().add(new ItemStack(Ironhold.FOOLS_GOLD.get()));
        }
    }

    /**
     * Will-o'-the-Wisps suppress hostile mob spawns within {@link WillOWispEntity#LIGHT_RADIUS}
     * blocks, the same way a torch's light level would. Only blocks natural spawning —
     * spawn eggs, spawners, and structure spawns are unaffected.
     */
    @SubscribeEvent
    public static void onMobFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Enemy)) return;
        EntitySpawnReason reason = event.getSpawnType();
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) return;

        var level = event.getLevel().getLevel();
        double r = WillOWispEntity.LIGHT_RADIUS;
        AABB box = new AABB(
            event.getX() - r, event.getY() - r, event.getZ() - r,
            event.getX() + r, event.getY() + r, event.getZ() + r);

        if (!level.getEntitiesOfClass(WillOWispEntity.class, box).isEmpty()) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }

    /** Match destination dimension even if {@link ResourceKey} identity differs between loaders. */
    private static boolean dimensionIdEquals(ResourceKey<Level> a, ResourceKey<Level> b) {
        return a.identifier().equals(b.identifier());
    }

    /**
     * Black-shulker bullet override — when a regular shulker bullet from a
     * {@link BlackShulkerEntity} applies LEVITATION, swap it for BLINDNESS.
     * Vanilla {@code ShulkerBullet#onHitEntity} attaches the owning shulker
     * (or the bullet itself) as the effect source, so we recognise it there.
     */
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide()) return;
        var inst = event.getEffectInstance();
        if (!inst.is(MobEffects.LEVITATION)) return;

        var source = event.getEffectSource();
        boolean fromBlackShulker =
            source instanceof BlackShulkerEntity
            || (source instanceof ShulkerBullet bullet
                && bullet.getOwner() instanceof BlackShulkerEntity);
        if (!fromBlackShulker) return;

        event.getEntity().removeEffect(MobEffects.LEVITATION);
        event.getEntity().addEffect(new MobEffectInstance(
            MobEffects.BLINDNESS, 30, 0, false, true));
    }

    /**
     * Reroll vanilla shulker spawns into our variants. End-city structure
     * pieces spawn shulkers via {@code addFreshEntity} without ever calling
     * {@code Mob#finalizeSpawn}, so {@link FinalizeSpawnEvent} never fires
     * for them — we hook {@link EntityJoinLevelEvent} instead, which is the
     * one place every spawn path converges.
     *
     * Exactly one shulker per End city becomes Black: we look up the city's
     * actual structure bounding box via the structure manager and gate the
     * reroll on whether any Black already exists inside it. Towers and the
     * floating airship can put shulkers >80 blocks apart, so a fixed AABB
     * check used to let multiple Blacks slip in. Other shulkers in the city
     * fall through to the 15% White reroll. Spawn-egg uses outside the End
     * also hit the White reroll, which is desired for testing.
     */
    @SubscribeEvent
    public static void onShulkerJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        if (event.getEntity().getClass() != Shulker.class) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Shulker original = (Shulker) event.getEntity();
        double sx = original.getX();
        double sy = original.getY();
        double sz = original.getZ();

        boolean inEnd = dimensionIdEquals(level.dimension(), Level.END);

        if (inEnd) {
            StructureStart cityStart = level.structureManager()
                .getStructureWithPieceAt(original.blockPosition(), holder -> holder.is(BuiltinStructures.END_CITY));
            if (cityStart.isValid()) {
                BoundingBox bb = cityStart.getBoundingBox();
                AABB cityBox = AABB.of(bb);
                if (level.getEntitiesOfClass(BlackShulkerEntity.class, cityBox).isEmpty()) {
                    BlackShulkerEntity black = kingdom.smp.ModEntities.BLACK_SHULKER.get()
                        .create(level, EntitySpawnReason.STRUCTURE);
                    if (black != null) {
                        black.snapTo(sx, sy, sz, original.getYRot(), original.getXRot());
                        black.inheritAttachStateFrom(original);
                        level.addFreshEntity(black);
                        event.setCanceled(true);
                    }
                    return;
                }
            }
        }

        if (level.getRandom().nextFloat() < 0.15f) {
            WhiteShulkerEntity white = kingdom.smp.ModEntities.WHITE_SHULKER.get()
                .create(level, EntitySpawnReason.STRUCTURE);
            if (white != null) {
                white.snapTo(sx, sy, sz, original.getYRot(), original.getXRot());
                white.inheritAttachStateFrom(original);
                level.addFreshEntity(white);
                event.setCanceled(true);
            }
        }
    }

    /**
     * Watches for player-killed Mimics. If the killer has Halric's
     * "Quiet the Roads" quest in {@code OFFERED} state, mark it
     * {@code COMPLETED} so the staff is awarded on their next visit to Halric.
     * Any Mimic flavour counts — Mimic, BabyMimic, ShipwreckMimic — anything
     * that wears the disguise is fair game.
     */
    @SubscribeEvent
    public static void onMimicDeathForHalricQuest(LivingDeathEvent event) {
        var victim = event.getEntity();
        if (!(victim instanceof kingdom.smp.entity.MimicEntity
            || victim instanceof kingdom.smp.entity.BabyMimicEntity
            || victim instanceof kingdom.smp.entity.ShipwreckMimicEntity)) {
            return;
        }
        var source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer killer)) return;
        if (!(victim.level() instanceof ServerLevel sl)) return;
        kingdom.smp.entity.HalricQuestSavedData quest =
            kingdom.smp.entity.HalricQuestSavedData.get(sl);
        if (quest.stateFor(killer.getUUID())
                != kingdom.smp.entity.HalricQuestSavedData.State.OFFERED) {
            return;
        }
        quest.setState(killer.getUUID(),
            kingdom.smp.entity.HalricQuestSavedData.State.COMPLETED);
        killer.sendSystemMessage(Component.literal(
            "§6§o[The Mimic falls. Word will reach Warden Halric.]"));
    }

    /** Small chance for a special drop on every Nth player kill of certain mobs. */
    private static final int BREEZE_DROP_INTERVAL = 8;
    private static final int DROWNED_DROP_INTERVAL = 10;
    /** Chance a Woodland Mansion vindicator is rerolled into a Possessed Armor. */
    private static final float MANSION_POSSESSED_ARMOR_CHANCE = 0.10F;
    /** Every Nth pillager spawned at a Pillager Outpost is rerolled into a Possessed Armor. */
    private static final int OUTPOST_POSSESSED_ARMOR_INTERVAL = 15;
    private static final java.util.concurrent.atomic.AtomicInteger OUTPOST_PILLAGER_COUNT =
        new java.util.concurrent.atomic.AtomicInteger();

    /**
     * Periodic special drops: every 8th Breeze yields a Breeze in a Bottle and
     * every 10th Drowned yields a Seashell. Only player kills count, tallied
     * world-wide via {@link kingdom.smp.world.KillTallyData}.
     */
    @SubscribeEvent
    public static void onSpecialMobDrops(LivingDeathEvent event) {
        var victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;

        if (victim instanceof net.minecraft.world.entity.monster.breeze.Breeze) {
            if (killTally(level).recordAndShouldDrop("breeze", BREEZE_DROP_INTERVAL)) {
                victim.spawnAtLocation(level, new ItemStack(Ironhold.CLOUD_IN_A_BOTTLE.get()));
            }
        } else if (victim instanceof net.minecraft.world.entity.monster.zombie.Drowned) {
            if (killTally(level).recordAndShouldDrop("drowned", DROWNED_DROP_INTERVAL)) {
                victim.spawnAtLocation(level, new ItemStack(Ironhold.SEASHELL.get()));
            }
        } else if (victim instanceof kingdom.smp.entity.ArcaneMageEntity) {
            // The naturally-spawning arcane mob yields a Totem of Undying when slain by a player.
            victim.spawnAtLocation(level, new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        }
    }

    private static kingdom.smp.world.KillTallyData killTally(ServerLevel level) {
        return level.getServer().getLevel(Level.OVERWORLD)
            .getDataStorage().computeIfAbsent(kingdom.smp.world.KillTallyData.TYPE);
    }

    /**
     * Reroll a small fraction of Woodland Mansion vindicators into Possessed
     * Armor. The structure check gates this to mansion spawns only — raid and
     * wild vindicators are untouched. The rerolled armor is flagged to drop a
     * Wraith's Sigil at 100% on death.
     */
    @SubscribeEvent
    public static void onMansionVindicatorJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        if (event.getEntity().getClass() != net.minecraft.world.entity.monster.illager.Vindicator.class) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        var vindicator = event.getEntity();
        StructureStart mansion = level.structureManager()
            .getStructureWithPieceAt(vindicator.blockPosition(),
                holder -> holder.is(BuiltinStructures.WOODLAND_MANSION));
        if (!mansion.isValid()) return;
        if (level.getRandom().nextFloat() >= MANSION_POSSESSED_ARMOR_CHANCE) return;

        kingdom.smp.entity.PossessedArmorEntity armor =
            kingdom.smp.ModEntities.POSSESSED_ARMOR.get().create(level, EntitySpawnReason.STRUCTURE);
        if (armor == null) return;
        armor.snapTo(vindicator.getX(), vindicator.getY(), vindicator.getZ(),
            vindicator.getYRot(), vindicator.getXRot());
        armor.setDropsWraithsSigil(true);
        armor.finalizeSpawn(level, level.getCurrentDifficultyAt(vindicator.blockPosition()),
            EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntity(armor);
        event.setCanceled(true);
    }

    /**
     * Reroll every Nth pillager that spawns at a Pillager Outpost into a
     * Possessed Armor. The structure check gates this to outpost spawns only —
     * raid and wild pillagers are untouched. Uses a world-wide counter so the
     * cadence is deterministic rather than probabilistic.
     */
    @SubscribeEvent
    public static void onOutpostPillagerJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        if (event.getEntity().getClass() != net.minecraft.world.entity.monster.illager.Pillager.class) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        var pillager = event.getEntity();
        StructureStart outpost = level.structureManager()
            .getStructureWithPieceAt(pillager.blockPosition(),
                holder -> holder.is(BuiltinStructures.PILLAGER_OUTPOST));
        if (!outpost.isValid()) return;
        if (OUTPOST_PILLAGER_COUNT.incrementAndGet() % OUTPOST_POSSESSED_ARMOR_INTERVAL != 0) return;

        kingdom.smp.entity.PossessedArmorEntity armor =
            kingdom.smp.ModEntities.POSSESSED_ARMOR.get().create(level, EntitySpawnReason.STRUCTURE);
        if (armor == null) return;
        armor.snapTo(pillager.getX(), pillager.getY(), pillager.getZ(),
            pillager.getYRot(), pillager.getXRot());
        armor.finalizeSpawn(level, level.getCurrentDifficultyAt(pillager.blockPosition()),
            EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntity(armor);
        event.setCanceled(true);
    }

    /**
     * Keep village knights and iron golems from fighting each other. Knights
     * extend Monster (so a golem's anti-Enemy targeting picks them up) and a hurt
     * knight retaliates via its HurtByTarget goal — cancelling the target change
     * in both directions short-circuits the feud no matter who provoked it.
     */
    @SubscribeEvent
    public static void onKnightGolemTarget(LivingChangeTargetEvent event) {
        var seeker = event.getEntity();
        var target = event.getNewAboutToBeSetTarget();
        if (target == null) return;
        boolean knightVsGolem =
            seeker instanceof kingdom.smp.entity.KnightEntity
                && target instanceof net.minecraft.world.entity.animal.golem.IronGolem;
        boolean golemVsKnight =
            seeker instanceof net.minecraft.world.entity.animal.golem.IronGolem
                && target instanceof kingdom.smp.entity.KnightEntity;
        if (knightVsGolem || golemVsKnight) {
            event.setCanceled(true);
        }
    }

    /** Routes the active partner's chat messages to a Kangarude they're talking with. */
    @SubscribeEvent
    public static void onServerChatForKangarude(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        KangarudeEntity npc = KangarudeEntity.activePartnerOf(player.getUUID());
        if (npc == null) return;
        // Player wandered off — drop the conversation silently.
        if (player.distanceToSqr(npc) > 32 * 32) {
            npc.endConversation();
            return;
        }
        npc.onPartnerChat(player, event.getRawText());
    }

    /**
     * Server-stopping diagnostic. Logs every live non-daemon thread along with
     * the top of its stack. Non-daemon threads are exactly what keeps the JVM
     * from exiting after FML's "Clearing ModLoader" — if the server hangs on
     * shutdown, this output identifies the culprit.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Ironhold.LOGGER.info("[Ironhold] ServerStoppingEvent — beginning shutdown diagnostics");
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Ironhold.LOGGER.info("[Ironhold] ServerStoppedEvent — JVM exit blockers (non-daemon threads still alive):");
        try {
            Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
            int nonDaemon = 0;
            for (Map.Entry<Thread, StackTraceElement[]> e : all.entrySet()) {
                Thread t = e.getKey();
                if (t.isDaemon()) continue;
                if (!t.isAlive()) continue;
                if (t == Thread.currentThread()) continue;
                nonDaemon++;
                StackTraceElement[] stack = e.getValue();
                String top = stack.length > 0 ? stack[0].toString() : "(no stack)";
                String second = stack.length > 1 ? stack[1].toString() : "";
                Ironhold.LOGGER.info("[Ironhold]   - {} [state={}] @ {}{}",
                    t.getName(), t.getState(), top,
                    second.isEmpty() ? "" : " <- " + second);
            }
            Ironhold.LOGGER.info("[Ironhold] {} non-daemon thread(s) still alive at server-stopped — these will hold the JVM open",
                nonDaemon);

            // Defensive cleanup for known third-party offenders that hold the
            // JVM open after FML shutdown. We don't try to fix anything we
            // can't unambiguously identify — only threads with stable names
            // and a documented "this should have been a daemon" upstream.
            //
            // WorldEdit-RecursiveDirectoryWatcher: WorldEdit spawns a non-
            // daemon filesystem watcher that parks on LockSupport and is
            // never stopped on ServerStoppingEvent. Calling interrupt() wakes
            // it from the park, and its run-loop exits when it sees the
            // interrupt flag. Without this, the JVM hangs at "Clearing
            // ModLoader" until Folium force-kills it.
            for (Thread t : all.keySet()) {
                if (t.isDaemon() || !t.isAlive()) continue;
                if (t == Thread.currentThread()) continue;
                if ("main".equals(t.getName())) continue;
                String name = t.getName();
                if (name.startsWith("WorldEdit-")) {
                    Ironhold.LOGGER.warn("[Ironhold] Interrupting non-daemon WorldEdit thread '{}' so the JVM can exit",
                        name);
                    try { t.interrupt(); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Ironhold] thread-dump diagnostic failed: {}", t.toString());
        }
    }
}
