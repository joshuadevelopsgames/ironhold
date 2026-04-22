package kingdom.smp.entity;

import kingdom.smp.ModAttachments;
import kingdom.smp.net.OpenVillagerScreenPayload;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Warden Halric — onboarding NPC at Wayfarer's Hollow gate.
 * Gives a class-appropriate starter kit on first interaction per player,
 * then delivers rotating flavour dialogue on return visits.
 */
public class WardenHalricEntity extends PathfinderMob {

    private static final int COOLDOWN_TICKS = 300; // 15 seconds between interactions

    // First-meeting dialogue — Halric sizes up the newcomer and sends them off with a kit
    private static final String FIRST_DIALOGUE =
        "Stand easy, traveler. I am Warden Halric — keeper of this gate and recorder of " +
        "those bold enough to seek their fortune here. The lands beyond are contested, and " +
        "the Kingdom needs every able soul it can muster. I've had your kit prepared. Take it. " +
        "It won't carry you forever — but it'll carry you through the first hard days. " +
        "From here, the path is yours.";

    // Return-visit lines — cycle through on each interaction after the first
    private static final String[] RETURN_DIALOGUES = {
        "Still standing. Good. Not everyone who passes this gate makes it back.",
        "The roads have been rougher of late. Whatever's moving out there, it's getting bolder. Watch your flanks.",
        "I've seen newer recruits than you give up before their first season was out. " +
            "You've outlasted them. Don't squander it.",
        "The Kingdom watches those who do quiet work. Your name has come up. Keep it that way.",
        "Another hard day behind you. There'll be more ahead. That's the job.",
        "Every veteran you see walking these grounds once stood where you're standing now. Remember that."
    };

    // Per-player kit tracking (persisted via NBT)
    private final Set<UUID> kitReceivers = new HashSet<>();

    // Per-villager rotation index so consecutive visits don't repeat the same line
    private int returnDialogueIndex = 0;

    // Global interaction cooldown
    private long lastInteractTick = 0;

    public WardenHalricEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getCustomName() == null) {
            this.setCustomName(Component.literal("Warden Halric"));
            this.setCustomNameVisible(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.SCALE, 1.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (super.isInvulnerableTo(level, source)) return true;
        return !source.is(DamageTypes.GENERIC_KILL)
            && !source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override
    public void push(double x, double y, double z) {}

    @Override
    public boolean isPushable() { return false; }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;

        long now = level().getGameTime();
        if (now - lastInteractTick < COOLDOWN_TICKS) return InteractionResult.SUCCESS;
        lastInteractTick = now;

        getLookControl().setLookAt(player, 30.0F, 30.0F);

        UUID playerId = sp.getUUID();
        boolean firstMeeting = !kitReceivers.contains(playerId);

        String dialogue;
        if (firstMeeting) {
            kitReceivers.add(playerId);
            giveStarterKit(sp);
            dialogue = FIRST_DIALOGUE;
        } else {
            dialogue = RETURN_DIALOGUES[returnDialogueIndex % RETURN_DIALOGUES.length];
            returnDialogueIndex++;
        }

        PacketDistributor.sendToPlayer(sp,
            new OpenVillagerScreenPayload(
                "Warden Halric",
                "warden",
                dialogue,
                OpenVillagerScreenPayload.encodeMood(0.1f),
                getId()));

        return InteractionResult.SUCCESS;
    }

    // ── Starter kit ───────────────────────────────────────────────────────────

    private void giveStarterKit(ServerPlayer player) {
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        String role = rpg.playerClass().role();

        // ── Base kit (everyone) ───────────────────────────────────────────────
        give(player, new ItemStack(Items.BREAD, 16));
        give(player, new ItemStack(Items.TORCH, 32));
        give(player, new ItemStack(Items.IRON_PICKAXE));
        give(player, new ItemStack(Items.IRON_SHOVEL));

        // ── Role-specific extras ──────────────────────────────────────────────
        switch (role) {
            case "Tank" -> {
                give(player, new ItemStack(Items.IRON_SWORD));
                give(player, new ItemStack(Items.IRON_CHESTPLATE));
                give(player, new ItemStack(Items.SHIELD));
            }
            case "Mage" -> {
                give(player, new ItemStack(Items.ENCHANTED_BOOK));
                give(player, new ItemStack(Items.ENDER_PEARL, 4));
                give(player, new ItemStack(Items.AMETHYST_SHARD, 8));
            }
            case "Ranger" -> {
                give(player, new ItemStack(Items.BOW));
                give(player, new ItemStack(Items.ARROW, 32));
                give(player, new ItemStack(Items.LEATHER_BOOTS));
                give(player, new ItemStack(Items.LEATHER_LEGGINGS));
            }
            case "Support" -> {
                give(player, new ItemStack(Items.GOLDEN_APPLE, 2));
                give(player, new ItemStack(Items.GLISTERING_MELON_SLICE, 8));
                give(player, new ItemStack(Items.IRON_SWORD));
            }
            case "Hybrid" -> {
                give(player, new ItemStack(Items.IRON_SWORD));
                give(player, new ItemStack(Items.BOW));
                give(player, new ItemStack(Items.ARROW, 16));
            }
            default -> {
                // PEASANT or unknown
                give(player, new ItemStack(Items.IRON_SWORD));
                give(player, new ItemStack(Items.LEATHER_HELMET));
                give(player, new ItemStack(Items.LEATHER_CHESTPLATE));
                give(player, new ItemStack(Items.LEATHER_LEGGINGS));
                give(player, new ItemStack(Items.LEATHER_BOOTS));
            }
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("ReturnIndex", returnDialogueIndex);
        if (!kitReceivers.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (UUID id : kitReceivers) {
                if (sb.length() > 0) sb.append(',');
                sb.append(id.toString());
            }
            out.putString("KitReceivers", sb.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        returnDialogueIndex = in.getIntOr("ReturnIndex", 0);
        String raw = in.getStringOr("KitReceivers", "");
        if (!raw.isEmpty()) {
            Arrays.stream(raw.split(",")).forEach(s -> {
                try { kitReceivers.add(UUID.fromString(s.trim())); }
                catch (IllegalArgumentException ignored) {}
            });
        }
    }
}
