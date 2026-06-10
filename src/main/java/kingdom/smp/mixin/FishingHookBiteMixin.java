package kingdom.smp.mixin;

import kingdom.smp.fishing.BaitRegistry;
import kingdom.smp.fishing.FishingMinigameManager;
import kingdom.smp.fishing.IFishingHookMinigame;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bridges vanilla's bite mechanic into the Ironhold fishing minigame.
 *
 * <ul>
 *   <li>Detects the moment vanilla flips {@code nibble} from 0 → positive
 *       (server-side, since {@code nibble} is server-authoritative) and
 *       asks {@link FishingMinigameManager} to start a session.</li>
 *   <li>While a session is active, pins {@code nibble} to a large value
 *       each tick so vanilla's 20-tick bite window can't expire mid-game
 *       and silently drop the catch.</li>
 * </ul>
 *
 * <p>On session resolve, the manager calls {@code retrieve()} (win) or
 * {@code discard()} (loss); the pin only keeps the option open.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookBiteMixin implements IFishingHookMinigame {

    @Shadow private int nibble;
    @Shadow private int timeUntilLured;
    @Shadow @Final private int luck;

    @Unique private int ironhold$prevNibble = 0;
    @Unique private boolean ironhold$minigameActive = false;

    @Override
    public void ironhold$setMinigameActive(boolean active) {
        this.ironhold$minigameActive = active;
    }

    @Override
    public boolean ironhold$isMinigameActive() {
        return this.ironhold$minigameActive;
    }

    @Override
    public int ironhold$getLuck() {
        return this.luck;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ironhold$pinBiteWindow(CallbackInfo ci) {
        if (ironhold$minigameActive) {
            // Large enough to survive any single-tick decrement, small enough not to
            // overflow / get treated weirdly by vanilla logic that checks nibble > N.
            this.nibble = 200;
            return;
        }
        // Faster bites: stronger bait shortens the wait before a bite by giving
        // vanilla's lure countdown extra progress each server tick, proportional to
        // the held bait's power (Terraria: higher bait power → quicker bites).
        FishingHook self = (FishingHook) (Object) this;
        if (!self.level().isClientSide() && this.timeUntilLured > 0
                && self.getPlayerOwner() instanceof ServerPlayer sp) {
            BaitRegistry.BestBait bait = BaitRegistry.findBest(sp);
            if (bait != null) {
                int extra = Math.max(1, bait.power() / 15); // ~+1..3 ticks/tick
                this.timeUntilLured = Math.max(1, this.timeUntilLured - extra);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ironhold$detectBiteStart(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.level().isClientSide()) {
            ironhold$prevNibble = nibble;
            return;
        }
        if (!ironhold$minigameActive && ironhold$prevNibble == 0 && nibble > 0) {
            Player owner = self.getPlayerOwner();
            if (owner instanceof ServerPlayer sp) {
                // Strict bait gate (Terraria-style "no bait, no bites"): without
                // bait the fish nibbles but never takes the hook. Zeroing nibble
                // makes vanilla re-roll a fresh lure delay next tick — so the line
                // keeps fishing but can never land a catch until bait is held.
                if (!BaitRegistry.hasBait(sp)) {
                    this.nibble = 0;
                } else {
                    FishingMinigameManager.tryStart(sp, self);
                }
            }
        }
        ironhold$prevNibble = nibble;
    }
}
