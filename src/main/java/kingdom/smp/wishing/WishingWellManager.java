package kingdom.smp.wishing;

import kingdom.smp.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wishing well: toss a {@link ModItems#GOLD_COIN gold coin} into the configured well box (see
 * {@link WishingWellState}, set up via {@code /wishingwell}) and it's consumed for a wish — a
 * weighted random item reward from the full registry, or a 10% "special event" (status effect,
 * friendly/hostile mob spawn, random teleport, or teleport-to-structure), all modulated by the
 * player's luck.
 *
 * <p>Ported from the original Fabric/Yarn Kingdom SMP implementation to NeoForge/Mojang mappings.
 * Registered on the game event bus from {@code Ironhold}. Detection is event-driven
 * ({@link EntityJoinLevelEvent}) with a cached per-tick fallback ({@link LevelTickEvent.Post}).
 */
public final class WishingWellManager {
    private WishingWellManager() {}

    private static final int MAX_REWARD_ROLLS = 40;
    private static final int EFFECT_DURATION_TICKS = 3 * 60 * 20;
    private static final int RANDOM_TELEPORT_MIN_RADIUS = 128;
    private static final int RANDOM_TELEPORT_MAX_RADIUS = 512;
    private static final int RANDOM_TELEPORT_ATTEMPTS = 25;
    private static final double SPECIAL_EVENT_CHANCE = 0.1D;

    /** Datapack-extendable exclusion list — add items here to keep them out of the reward pool. */
    private static final TagKey<Item> WISHING_WELL_BLACKLIST =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("ironhold", "wishing_well_blacklist"));

    private static final Set<Identifier> BLACKLISTED_ITEMS = buildItemBlacklist();
    private static final Set<Identifier> BLACKLISTED_ENTITY_IDS = buildEntityBlacklist();
    private static final Map<Identifier, Double> ITEM_WEIGHT_OVERRIDES = buildItemWeightOverrides();

    private static final List<Holder<MobEffect>> POSITIVE_EFFECTS = List.of(
        MobEffects.REGENERATION, MobEffects.RESISTANCE, MobEffects.HASTE, MobEffects.LUCK,
        MobEffects.ABSORPTION, MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.HERO_OF_THE_VILLAGE);
    private static final List<Holder<MobEffect>> NEGATIVE_EFFECTS = List.of(
        MobEffects.WEAKNESS, MobEffects.MINING_FATIGUE, MobEffects.SLOWNESS, MobEffects.NAUSEA,
        MobEffects.POISON, MobEffects.BLINDNESS, MobEffects.HUNGER);
    private static final List<EntityType<? extends Mob>> COMPANION_MOBS = List.of(
        EntityType.ALLAY, EntityType.AXOLOTL, EntityType.CAT, EntityType.FOX, EntityType.PARROT,
        EntityType.RABBIT, EntityType.SNIFFER, EntityType.WOLF, EntityType.HORSE, EntityType.MOOSHROOM);
    private static final List<EntityType<? extends Mob>> DANGEROUS_MOBS = List.of(
        EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.SKELETON, EntityType.STRAY,
        EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER);
    private static final List<Item> PREMIUM_REWARD_POOL = List.of(
        Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP, Items.GOLDEN_APPLE,
        Items.TOTEM_OF_UNDYING, Items.EXPERIENCE_BOTTLE);
    private static final List<Item> JUNK_REWARD_POOL = List.of(
        Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.DIRT);
    private static final List<TagKey<Structure>> STRUCTURE_TARGET_TAGS = List.of(
        StructureTags.VILLAGE, StructureTags.ON_WOODLAND_EXPLORER_MAPS, StructureTags.ON_JUNGLE_EXPLORER_MAPS,
        StructureTags.ON_TREASURE_MAPS, StructureTags.MINESHAFT, StructureTags.SHIPWRECK,
        StructureTags.RUINED_PORTAL, StructureTags.ON_TRIAL_CHAMBERS_MAPS);

    // Track processed coins to avoid double-granting (thread-safe for multi-world servers).
    private static final Set<ItemEntity> PROCESSED_COINS = ConcurrentHashMap.newKeySet();
    private static final Map<ServerLevel, Boolean> nearbyPlayersCache = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, Integer> nearbyPlayersCacheTicks = new ConcurrentHashMap<>();
    private static final int NEARBY_PLAYERS_CACHE_TICKS = 20;
    private static WishingWellState cachedWellState = null;
    private static int cachedWellStateTick = -1;

    // ── Event hooks ────────────────────────────────────────────────────────

    /** Instant detection: every item entity is checked once when it enters the level. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel world && event.getEntity() instanceof ItemEntity itemEntity) {
            checkCoinEntity(itemEntity, world);
        }
    }

    /** Per-tick fallback to catch coins the entity event missed (coins flicked in mid-flight, etc.). */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel world) {
            handleWorldTick(world);
        }
    }

    // ── Detection ──────────────────────────────────────────────────────────

    private static void checkCoinEntity(ItemEntity coinEntity, ServerLevel world) {
        if (world.isClientSide()) return;
        if (!coinEntity.getItem().is(ModItems.GOLD_COIN.get())) return;
        if (PROCESSED_COINS.contains(coinEntity)) return;

        WishingWellState state = WishingWellState.get(world.getServer());
        if (!state.isEnabled()) return;
        if (state.getDimensionKey().isPresent() && !world.dimension().equals(state.getDimensionKey().get())) return;

        AABB box = state.getWellBox().orElse(null);
        if (box == null) return;

        Vec3 coinPos = coinEntity.position();
        if (!box.contains(coinPos.x, coinPos.y, coinPos.z)) return;
        if (!hasNearbyPlayersCached(world, box)) return;

        PROCESSED_COINS.add(coinEntity);
        handleWish(world, coinEntity, box);
    }

    private static boolean hasNearbyPlayersCached(ServerLevel world, AABB box) {
        int currentTick = world.getServer().getTickCount();
        Integer cachedTick = nearbyPlayersCacheTicks.get(world);
        if (cachedTick != null && (currentTick - cachedTick) < NEARBY_PLAYERS_CACHE_TICKS) {
            Boolean cached = nearbyPlayersCache.get(world);
            if (cached != null) return cached;
        }
        Vec3 boxCenter = box.getCenter();
        boolean hasNearby = false;
        for (ServerPlayer player : world.players()) {
            if (player.position().distanceTo(boxCenter) < 10.0) {
                hasNearby = true;
                break;
            }
        }
        nearbyPlayersCache.put(world, hasNearby);
        nearbyPlayersCacheTicks.put(world, currentTick);
        return hasNearby;
    }

    private static void handleWorldTick(ServerLevel world) {
        int currentTick = world.getServer().getTickCount();
        if (world.players().isEmpty()) return;

        if (cachedWellState == null || (currentTick - cachedWellStateTick) >= 20) {
            cachedWellState = WishingWellState.get(world.getServer());
            cachedWellStateTick = currentTick;
        }
        WishingWellState state = cachedWellState;
        if (!state.isEnabled()) return;
        if (state.getDimensionKey().isPresent() && !world.dimension().equals(state.getDimensionKey().get())) return;

        AABB box = state.getWellBox().orElse(null);
        if (box == null) return;
        if (!hasNearbyPlayersCached(world, box)) return;

        var coins = world.getEntitiesOfClass(ItemEntity.class, box,
            e -> e.isAlive() && e.getItem().is(ModItems.GOLD_COIN.get()) && !PROCESSED_COINS.contains(e));
        for (ItemEntity coin : coins) {
            PROCESSED_COINS.add(coin);
            handleWish(world, coin, box);
        }
        PROCESSED_COINS.removeIf(e -> !e.isAlive() || e.isRemoved());
    }

    // ── Wish resolution ──────────────────────────────────────────────────────

    private static void handleWish(ServerLevel world, ItemEntity coinEntity, AABB wellBox) {
        ItemStack stack = coinEntity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            coinEntity.discard();
        } else {
            coinEntity.setItem(stack);
        }

        ServerPlayer player = resolveWishPlayer(world, coinEntity);
        Vec3 coinPos = coinEntity.position();
        playSparkleSound(world, coinPos);
        if (player == null) {
            dropItemReward(world, coinPos, LuckProfile.NEUTRAL);
            return;
        }

        LuckProfile luck = LuckProfile.from(player);
        double specialChance = clampChance(SPECIAL_EVENT_CHANCE + luck.specialChanceDelta);
        if (world.getRandom().nextDouble() < specialChance) {
            player.sendSystemMessage(luck.shimmerMessage);
            executeRandomOutcome(world, player, coinPos, wellBox, luck);
        } else {
            dropItemReward(world, coinPos, luck);
            player.sendSystemMessage(luck.rewardMessage);
        }
    }

    private static ServerPlayer resolveWishPlayer(ServerLevel world, ItemEntity coinEntity) {
        if (coinEntity.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }
        Player nearest = world.getNearestPlayer(coinEntity.getX(), coinEntity.getY(), coinEntity.getZ(), 6.0, false);
        return nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static ItemStack getRandomReward(RandomSource random) {
        for (int attempts = 0; attempts < MAX_REWARD_ROLLS; attempts++) {
            Optional<Holder.Reference<Item>> optional = BuiltInRegistries.ITEM.getRandom(random);
            if (optional.isEmpty()) continue;
            Item item = optional.get().value();
            if (item == Items.AIR || isBlacklisted(item)) continue;
            if (!passesWeightCheck(item, random)) continue;
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty() || stack.is(WISHING_WELL_BLACKLIST)) continue;
            int maxCount = Math.max(1, Math.min(maxStack(stack), 4));
            stack.setCount(random.nextInt(maxCount) + 1);
            return stack;
        }
        return new ItemStack(Items.EMERALD);
    }

    // ── Special events ───────────────────────────────────────────────────────

    private static void executeRandomOutcome(ServerLevel world, ServerPlayer player, Vec3 origin, AABB wellBox, LuckProfile luck) {
        double roll = Mth.clamp(world.getRandom().nextDouble() + luck.outcomeShift, 0.0D, 0.999D);
        if (roll < 0.5) {
            applyRandomStatusEffect(player, world.getRandom(), luck);
            return;
        }
        if (roll < 0.75) {
            boolean spawnFriendly = luck.rollFriendly(world.getRandom());
            if (spawnFriendly) {
                if (spawnFriendlyCompanions(world, wellBox, world.getRandom())) {
                    player.sendSystemMessage(Component.literal("§6[Wishing Well] §aCompanions emerge to aid you!"));
                } else if (spawnDangerousEncounters(world, wellBox, world.getRandom())) {
                    player.sendSystemMessage(Component.literal("§6[Wishing Well] §cHostile spirits crawl out of the water!"));
                }
            } else {
                if (spawnDangerousEncounters(world, wellBox, world.getRandom())) {
                    player.sendSystemMessage(Component.literal("§6[Wishing Well] §cHostile spirits crawl out of the water!"));
                } else if (spawnFriendlyCompanions(world, wellBox, world.getRandom())) {
                    player.sendSystemMessage(Component.literal("§6[Wishing Well] §aCompanions emerge to aid you!"));
                }
            }
            return;
        }
        if (roll < 0.95) {
            if (!teleportToRandomLocation(player, world, origin, world.getRandom())) {
                applyRandomStatusEffect(player, world.getRandom(), luck);
            }
            return;
        }
        if (!teleportToStructure(player, world, world.getRandom())) {
            if (spawnFriendlyCompanions(world, wellBox, world.getRandom())) {
                player.sendSystemMessage(Component.literal("§6[Wishing Well] §aThe waters could not find a structure, but friendly wildlife answers instead."));
            } else if (spawnDangerousEncounters(world, wellBox, world.getRandom())) {
                player.sendSystemMessage(Component.literal("§6[Wishing Well] §cThe waters churn angrily, unleashing hostile mobs instead!"));
            }
        }
    }

    private static void applyRandomStatusEffect(ServerPlayer player, RandomSource random, LuckProfile luck) {
        boolean positive = random.nextDouble() < luck.positiveEffectChance;
        List<Holder<MobEffect>> pool = positive ? POSITIVE_EFFECTS : NEGATIVE_EFFECTS;
        Holder<MobEffect> entry = pool.get(random.nextInt(pool.size()));
        int amplifier = positive ? 1 : 0;
        player.addEffect(new MobEffectInstance(entry, EFFECT_DURATION_TICKS, amplifier));
        Component effectName = Component.translatable(entry.value().getDescriptionId());
        if (positive) {
            player.sendSystemMessage(Component.literal("§6[Wishing Well] §aYou feel invigorated by ")
                .append(effectName).append(Component.literal("§a for 3 minutes!")));
        } else {
            player.sendSystemMessage(Component.literal("§6[Wishing Well] §cA lingering ")
                .append(effectName).append(Component.literal("§c clings to you for 3 minutes!")));
        }
    }

    private static boolean spawnFriendlyCompanions(ServerLevel world, AABB wellBox, RandomSource random) {
        return spawnFromPool(world, wellBox, random, COMPANION_MOBS);
    }

    private static boolean spawnDangerousEncounters(ServerLevel world, AABB wellBox, RandomSource random) {
        return spawnFromPool(world, wellBox, random, DANGEROUS_MOBS);
    }

    private static boolean spawnFromPool(ServerLevel world, AABB wellBox, RandomSource random, List<EntityType<? extends Mob>> pool) {
        int count = 1 + random.nextInt(2);
        BlockPos center = BlockPos.containing(wellBox.getCenter());
        boolean spawned = false;
        for (int i = 0; i < count; i++) {
            EntityType<? extends Mob> type = chooseMobType(pool, random);
            if (type == null) continue;
            BlockPos pos = findMobSpawnPosition(world, center, wellBox, random).orElse(center);
            if (type.spawn(world, pos, EntitySpawnReason.EVENT) != null) {
                spawned = true;
            }
        }
        return spawned;
    }

    private static EntityType<? extends Mob> chooseMobType(List<EntityType<? extends Mob>> pool, RandomSource random) {
        if (pool.isEmpty()) return null;
        for (int attempt = 0; attempt < pool.size() * 2; attempt++) {
            EntityType<? extends Mob> candidate = pool.get(random.nextInt(pool.size()));
            if (!isMobBlacklisted(candidate)) return candidate;
        }
        return null;
    }

    private static Optional<BlockPos> findMobSpawnPosition(ServerLevel world, BlockPos center, AABB wellBox, RandomSource random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int distance = 5 + random.nextInt(16);
            double angle = random.nextDouble() * Math.PI * 2;
            int offsetX = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int offsetZ = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            Optional<BlockPos> landing = findSafeLanding(world, offsetX, offsetZ);
            if (landing.isPresent() && !wellBox.contains(Vec3.atCenterOf(landing.get()))) {
                return landing;
            }
        }
        return Optional.empty();
    }

    private static boolean isMobBlacklisted(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && BLACKLISTED_ENTITY_IDS.contains(id);
    }

    // ── Teleports ────────────────────────────────────────────────────────────

    private static boolean teleportToRandomLocation(ServerPlayer player, ServerLevel world, Vec3 origin, RandomSource random) {
        BlockPos originPos = BlockPos.containing(origin);
        for (int attempt = 0; attempt < RANDOM_TELEPORT_ATTEMPTS; attempt++) {
            int distance = RANDOM_TELEPORT_MIN_RADIUS + random.nextInt(RANDOM_TELEPORT_MAX_RADIUS - RANDOM_TELEPORT_MIN_RADIUS + 1);
            double angle = random.nextDouble() * Math.PI * 2;
            int targetX = originPos.getX() + Mth.floor(Math.cos(angle) * distance);
            int targetZ = originPos.getZ() + Mth.floor(Math.sin(angle) * distance);
            Optional<BlockPos> landing = findSafeLanding(world, targetX, targetZ);
            if (landing.isPresent()) {
                teleportPlayer(player, world, landing.get());
                player.sendSystemMessage(Component.literal("§6[Wishing Well] §bYou are whisked away to a new location!"));
                return true;
            }
        }
        return false;
    }

    private static boolean teleportToStructure(ServerPlayer player, ServerLevel world, RandomSource random) {
        if (!world.dimension().equals(Level.OVERWORLD) || STRUCTURE_TARGET_TAGS.isEmpty()) return false;
        BlockPos center = player.blockPosition();
        for (int attempt = 0; attempt < 4; attempt++) {
            TagKey<Structure> tag = STRUCTURE_TARGET_TAGS.get(random.nextInt(STRUCTURE_TARGET_TAGS.size()));
            BlockPos structurePos = world.findNearestMapStructure(tag, center, 128, false);
            if (structurePos == null) continue;
            Optional<BlockPos> landing = findSafeLanding(world, structurePos.getX(), structurePos.getZ());
            if (landing.isEmpty()) landing = findNearbyLanding(world, structurePos, random);
            if (landing.isPresent()) {
                teleportPlayer(player, world, landing.get());
                player.sendSystemMessage(Component.literal("§6[Wishing Well] §dAncient power teleports you to a hidden structure!"));
                return true;
            }
        }
        return false;
    }

    private static Optional<BlockPos> findNearbyLanding(ServerLevel world, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = random.nextInt(17) - 8;
            int dz = random.nextInt(17) - 8;
            Optional<BlockPos> landing = findSafeLanding(world, center.getX() + dx, center.getZ() + dz);
            if (landing.isPresent()) return landing;
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findSafeLanding(ServerLevel world, int x, int z) {
        int topY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (topY <= world.getMinY()) return Optional.empty();
        BlockPos landing = new BlockPos(x, topY, z);
        if (!world.getBlockState(landing).isAir()) {
            landing = landing.above();
        }
        BlockPos below = landing.below();
        BlockState belowState = world.getBlockState(below);
        if (!belowState.isFaceSturdy(world, below, Direction.UP)) return Optional.empty();
        if (!world.getBlockState(landing).getFluidState().isEmpty()) return Optional.empty();
        return Optional.of(landing);
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel world, BlockPos target) {
        player.teleportTo(world, target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
            Set.of(), player.getYRot(), player.getXRot(), false);
    }

    // ── Rewards ──────────────────────────────────────────────────────────────

    private static void dropItemReward(ServerLevel world, Vec3 pos, LuckProfile luck) {
        RandomSource random = world.getRandom();
        spawnRewardEntity(world, pos, adjustRewardForLuck(getRandomReward(random), luck, random));
        if (random.nextDouble() < luck.bonusRewardChance) {
            spawnRewardEntity(world, pos, adjustRewardForLuck(getRandomReward(random), luck, random));
        }
    }

    private static ItemStack adjustRewardForLuck(ItemStack original, LuckProfile luck, RandomSource random) {
        ItemStack stack = original.copy();
        if (luck == LuckProfile.FORTUNATE) {
            if (random.nextFloat() < 0.35F) {
                return createStackFromPool(PREMIUM_REWARD_POOL, random);
            }
            int bonus = 1 + random.nextInt(2);
            stack.setCount(Math.min(maxStack(stack), stack.getCount() + bonus));
            return stack;
        }
        if (luck == LuckProfile.CURSED) {
            if (random.nextFloat() < 0.45F) {
                return createStackFromPool(JUNK_REWARD_POOL, random);
            }
            if (stack.getCount() > 1) {
                stack.shrink(1 + random.nextInt(Math.min(2, stack.getCount() - 1)));
            }
        }
        return stack;
    }

    private static ItemStack createStackFromPool(List<Item> pool, RandomSource random) {
        if (pool.isEmpty()) return new ItemStack(Items.EMERALD);
        Item item = pool.get(random.nextInt(pool.size()));
        ItemStack probe = new ItemStack(item);
        int maxCount = Math.max(1, Math.min(maxStack(probe), 4));
        return new ItemStack(item, 1 + random.nextInt(maxCount));
    }

    private static void spawnRewardEntity(ServerLevel world, Vec3 pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity rewardEntity = new ItemEntity(world, pos.x, pos.y, pos.z, stack.copy());
        rewardEntity.setDeltaMovement(
            (world.getRandom().nextDouble() - 0.5) * 0.2,
            0.7 + world.getRandom().nextDouble() * 0.3,
            (world.getRandom().nextDouble() - 0.5) * 0.2);
        world.addFreshEntity(rewardEntity);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static int maxStack(ItemStack stack) {
        return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 64);
    }

    private static double clampChance(double chance) {
        return Mth.clamp(chance, 0.02D, 0.5D);
    }

    private static void playSparkleSound(ServerLevel world, Vec3 pos) {
        float pitch = 1.0F + (world.getRandom().nextFloat() - 0.5F) * 0.3F;
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, pitch);
    }

    private static boolean isBlacklisted(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return false;
        if (BLACKLISTED_ITEMS.contains(id)) return true;
        // Mod items checked by identity at call time (their DeferredHolders may not be populated when
        // this class's static blacklist is built): don't hand out the well's own currency or lock tools.
        if (item == ModItems.GOLD_COIN.get() || item == ModItems.LOCK.get() || item == ModItems.KEY.get()) {
            return true;
        }
        if ("minecraft".equals(id.getNamespace())) {
            String path = id.getPath();
            return path.equals("wither_spawn_egg") || path.equals("ender_dragon_spawn_egg") || path.equals("shulker_spawn_egg");
        }
        return false;
    }

    private static boolean passesWeightCheck(Item item, RandomSource random) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return true;
        Double weight = ITEM_WEIGHT_OVERRIDES.get(id);
        return weight == null || random.nextDouble() < weight;
    }

    private static Set<Identifier> buildItemBlacklist() {
        Set<Identifier> items = new HashSet<>();
        for (Item item : new Item[]{
            // Admin / unobtainable
            Items.COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK,
            Items.COMMAND_BLOCK_MINECART, Items.STRUCTURE_BLOCK, Items.STRUCTURE_VOID, Items.JIGSAW,
            Items.BARRIER, Items.LIGHT, Items.BEDROCK, Items.DEBUG_STICK, Items.KNOWLEDGE_BOOK,
            Items.SPAWNER, Items.TRIAL_SPAWNER, Items.VAULT, Items.END_PORTAL_FRAME, Items.DRAGON_EGG,
            Items.BUDDING_AMETHYST, Items.REINFORCED_DEEPSLATE, Items.PETRIFIED_OAK_SLAB,
            // Too powerful to hand out for one coin
            Items.NETHERITE_BLOCK, Items.ELYTRA,
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
            Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
            // Dangerous spawn eggs
            Items.WITHER_SPAWN_EGG, Items.ENDER_DRAGON_SPAWN_EGG, Items.SHULKER_SPAWN_EGG,
        }) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) items.add(id);
        }
        return Set.copyOf(items);
    }

    private static Set<Identifier> buildEntityBlacklist() {
        Set<Identifier> ids = new HashSet<>();
        for (String value : new String[]{
            "minecraft:creeper", "minecraft:ender_dragon", "minecraft:wither",
            "minecraft:wither_skeleton", "minecraft:ghast"
        }) {
            Identifier parsed = Identifier.tryParse(value);
            if (parsed != null) ids.add(parsed);
        }
        return Set.copyOf(ids);
    }

    private static Map<Identifier, Double> buildItemWeightOverrides() {
        Map<Identifier, Double> weights = new HashMap<>();
        Identifier netheriteIngot = BuiltInRegistries.ITEM.getKey(Items.NETHERITE_INGOT);
        if (netheriteIngot != null) weights.put(netheriteIngot, 0.2D); // 20% chance to keep, else reroll
        return Map.copyOf(weights);
    }

    private enum LuckProfile {
        CURSED(-0.04D, 0.12D, 0.3D, 0.0D,
            "§6[Wishing Well] §7The waters grudgingly return a soggy trinket.",
            "§6[Wishing Well] §4The water darkens ominously as your coin sinks..."),
        NEUTRAL(0.0D, 0.0D, 0.5D, 0.1D,
            "§6[Wishing Well] §aThe well tosses treasure back to you.",
            "§6[Wishing Well] §bThe waters shimmer in response to your offering..."),
        FORTUNATE(0.12D, -0.12D, 0.75D, 0.45D,
            "§6[Wishing Well] §aFortune favors you! Extra treasure spills out.",
            "§6[Wishing Well] §bThe waters glow brightly as fortune answers your call!");

        private final double specialChanceDelta;
        private final double outcomeShift;
        private final double positiveEffectChance;
        private final double bonusRewardChance;
        private final Component rewardMessage;
        private final Component shimmerMessage;

        LuckProfile(double specialChanceDelta, double outcomeShift, double positiveEffectChance,
                    double bonusRewardChance, String rewardMessage, String shimmerMessage) {
            this.specialChanceDelta = specialChanceDelta;
            this.outcomeShift = outcomeShift;
            this.positiveEffectChance = positiveEffectChance;
            this.bonusRewardChance = bonusRewardChance;
            this.rewardMessage = Component.literal(rewardMessage);
            this.shimmerMessage = Component.literal(shimmerMessage);
        }

        private static LuckProfile from(ServerPlayer player) {
            if (player == null) return NEUTRAL;
            double luckValue = player.getAttributeValue(Attributes.LUCK);
            if (luckValue >= 1.0D) return FORTUNATE;
            if (luckValue <= -0.5D) return CURSED;
            return NEUTRAL;
        }

        private boolean rollFriendly(RandomSource random) {
            return random.nextDouble() < positiveEffectChance;
        }
    }
}
