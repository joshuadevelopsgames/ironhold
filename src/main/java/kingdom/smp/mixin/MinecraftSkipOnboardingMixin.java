package kingdom.smp.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skips the first-launch "Accessibility Onboarding" / narrator selection screen.
 *
 * <p>Vanilla {@code Minecraft.addInitialScreens} gates the screen on
 * {@code this.options.onboardAccessibility || SharedConstants.DEBUG_FORCE_ONBOARDING_SCREEN}.
 * We zero the flag at HEAD so vanilla's check fails naturally and the screen is never queued —
 * keeping the rest of the startup-screen flow (ban notices, profile-action banners, etc.)
 * intact. The flag persists once Options writes options.txt back (next save), so this mixin
 * doesn't need to fire on every startup either.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftSkipOnboardingMixin {

    @Shadow public Options options;

    @Inject(method = "addInitialScreens", at = @At("HEAD"))
    private void ironhold$disableOnboarding(java.util.List<?> screens, CallbackInfoReturnable<Boolean> cir) {
        if (this.options != null && this.options.onboardAccessibility) {
            this.options.onboardAccessibility = false;
            // First launch: force the narrator (the "announcer") off so the player doesn't have to
            // dig through Options to silence it, and disable its Ctrl+B hotkey so it can't be toggled
            // on by accident. Players can still re-enable both from Accessibility settings later.
            this.options.narrator().set(NarratorStatus.OFF);
            this.options.narratorHotkey().set(false);
            this.options.save();
        }
    }
}
