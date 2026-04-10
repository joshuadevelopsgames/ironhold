package kingdom.smp.client;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.MagicMinecartEntity;
import kingdom.smp.item.AnkhShieldItem;
import kingdom.smp.net.CloudDoubleJumpPayload;
import kingdom.smp.net.MagicMinecartInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import kingdom.smp.client.hud.CarryWeightHud;
import kingdom.smp.client.hud.ClassInfoHud;

/** Client-only NeoForge game-bus listeners (registered from {@link kingdom.smp.IronholdClient}). */
public final class ClientNeoForgeEvents {
    private ClientNeoForgeEvents() {}

    /** Rising edge for jump (tap-again mid-air). */
    private static boolean cloudJumpPrevJumpDown;
    /** Ticks since leaving ground; used to ignore the same physical press as the ground jump. */
    private static int cloudAirGraceTicks;
    /** One Breeze boost per airtime (server also enforces via {@link kingdom.smp.game.CloudJumpState}). */
    private static boolean cloudBoostSentThisAirtime;

    /** Any {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen}: weight beside panel. */
    @SubscribeEvent
    public static void onScreenPost(ScreenEvent.Render.Post event) {
        InventoryWeightOverlay.renderAfterScreen(event);
    }

    @SubscribeEvent
    public static void onHudPost(RenderGuiEvent.Post event) {
        ClassInfoHud.render(event.getGuiGraphics(), event.getPartialTick());
        CarryWeightHud.render(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        boolean jumpDown = mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !cloudJumpPrevJumpDown;
        cloudJumpPrevJumpDown = jumpDown;

        var local = mc.player;
        if (local == null) {
            cloudJumpPrevJumpDown = false;
            return;
        }

        if (local.getAbilities().flying || local.isFallFlying() || local.getVehicle() != null) {
            cloudAirGraceTicks = 0;
            cloudBoostSentThisAirtime = false;
            return;
        }

        if (local.onGround()) {
            cloudAirGraceTicks = 0;
            cloudBoostSentThisAirtime = false;
            return;
        }

        cloudAirGraceTicks++;

        if (cloudBoostSentThisAirtime) {
            return;
        }

        // Holding jump from the ground jump never produced a rising edge in air — allow after a short delay.
        boolean holdBoost = jumpDown && cloudAirGraceTicks >= 5;
        // Tap jump again after leaving ground (rising edge once airborne long enough).
        boolean tapBoost = jumpPressed && cloudAirGraceTicks >= 2;
        if (holdBoost || tapBoost) {
            cloudBoostSentThisAirtime = true;
            ClientPayloads.sendToServer(new CloudDoubleJumpPayload());
        }
    }

    private static final ResourceKey<Biome> EBONWOOD_HOLLOW = ResourceKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath("ironhold", "ebonwood_hollow"));

    // ── Ebonwood ambient sound ─────────────────────────────────────────────
    // Sound: pale-garden.ogg by CreativeMD / AmbientSounds mod (LGPL-3.0)
    // https://github.com/CreativeMD/AmbientSounds
    private static FadingAmbientSound ebonwoodAmbientSound = null;

    // ── Smooth fog/sky transition ────────────────────────────────────────────
    // Blend factor 0.0 = normal biome, 1.0 = fully in ebonwood
    private static float ebonwoodBlend = 0.0f;
    private static final float BLEND_SPEED = 0.02f; // ~2.5 seconds for full transition

    private static boolean isInEbonwoodHollow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return false;
        BlockPos pos = mc.player.blockPosition();
        Holder<Biome> biome = mc.level.getBiome(pos);
        return biome.is(EBONWOOD_HOLLOW);
    }

    /** Called each tick to update the blend factor smoothly and manage ambient sound. */
    private static void tickEbonwoodBlend() {
        boolean inBiome = isInEbonwoodHollow();
        if (inBiome && ebonwoodBlend < 1.0f) {
            ebonwoodBlend = Math.min(1.0f, ebonwoodBlend + BLEND_SPEED);
        } else if (!inBiome && ebonwoodBlend > 0.0f) {
            ebonwoodBlend = Math.max(0.0f, ebonwoodBlend - BLEND_SPEED);
        }

        // Fade ambient sound in/out when entering/leaving ebonwood
        Minecraft mc = Minecraft.getInstance();
        if (inBiome) {
            // Start a new fading sound if we don't have one (or the old one was fading out)
            if (ebonwoodAmbientSound == null || ebonwoodAmbientSound.isStopped() || ebonwoodAmbientSound.isFadingOut()) {
                ebonwoodAmbientSound = new FadingAmbientSound(
                    Identifier.fromNamespaceAndPath(Ironhold.MODID, "ambient.ebonwood_hollow"),
                    0.8F, 0.02F); // fade in over ~2 seconds (0.02 * 40 ticks = 0.8)
                mc.getSoundManager().play(ebonwoodAmbientSound);
            }
        } else {
            // Fade out when leaving
            if (ebonwoodAmbientSound != null && !ebonwoodAmbientSound.isFadingOut()) {
                ebonwoodAmbientSound.fadeOut();
            }
            if (ebonwoodAmbientSound != null && ebonwoodAmbientSound.isStopped()) {
                ebonwoodAmbientSound = null;
            }
        }
    }

    // Ebonwood target fog color: #1A0525
    private static final float EBON_FOG_R = 0x1A / 255f;
    private static final float EBON_FOG_G = 0x05 / 255f;
    private static final float EBON_FOG_B = 0x25 / 255f;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        tickEbonwoodBlend();
        if (ebonwoodBlend <= 0.0f) return;
        float t = ebonwoodBlend;
        event.setRed(lerp(event.getRed(), EBON_FOG_R, t));
        event.setGreen(lerp(event.getGreen(), EBON_FOG_G, t));
        event.setBlue(lerp(event.getBlue(), EBON_FOG_B, t));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (ebonwoodBlend <= 0.0f) return;
        float t = ebonwoodBlend;
        // Terrain fog: dense and close for atmosphere
        float defaultNear = event.getNearPlaneDistance();
        float defaultFar = event.getFarPlaneDistance();
        event.setNearPlaneDistance(lerp(defaultNear, 5f, t));
        event.setFarPlaneDistance(lerp(defaultFar, 40f, t));
        // Sky fog matches terrain fog so it extends all the way up
        event.getFogData().skyEnd = lerp(event.getFogData().skyEnd, 40f, t);
        event.getFogData().cloudEnd = lerp(event.getFogData().cloudEnd, 40f, t);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** Max distance (squared) at which ankh shield particles are visible to the local player. */
    private static final double ANKH_PARTICLE_RANGE_SQ = 30.0 * 30.0;

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        VillagerDialogueCache.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            long gt = mc.level.getGameTime();
            // Only render every 2 ticks to halve particle cost
            if (gt % 2 == 0) {
                for (Player p : mc.level.players()) {
                    if (!AnkhShieldItem.isBlockingWithAnkh(p)) {
                        continue;
                    }
                    if (mc.player.distanceToSqr(p) > ANKH_PARTICLE_RANGE_SQ) {
                        continue;
                    }
                    double cx = p.getX();
                    double cz = p.getZ();
                    double r = 1.42;
                    int n = 26;
                    for (int ring = 0; ring < 2; ring++) {
                        double cy = p.getY() + 0.28 + ring * 0.82 + Math.sin((gt + ring * 7) * 0.07) * 0.06;
                        double phase = gt * 0.042 + ring * 0.6;
                        for (int i = 0; i < n; i++) {
                            double ang = (Math.PI * 2 * i) / n + phase;
                            double x = cx + Math.cos(ang) * r;
                            double z = cz + Math.sin(ang) * r;
                            mc.level.addParticle(ParticleTypes.ENCHANT, x, cy, z, 0.0, 0.02, 0.0);
                            if ((i + ring) % 3 == 0) {
                                mc.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, cy, z, 0.0, 0.0, 0.0);
                            }
                        }
                    }
                }
            }
        }

        if (mc.player == null || !mc.player.isPassenger()) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof MagicMinecartEntity)) {
            return;
        }
        var opts = mc.options;
        ClientPayloads.sendToServer(
            new MagicMinecartInputPayload(
                opts.keyUp.isDown(),
                opts.keyDown.isDown(),
                opts.keyLeft.isDown(),
                opts.keyRight.isDown(),
                opts.keyJump.isDown(),
                opts.keySprint.isDown()
            )
        );
    }
}
