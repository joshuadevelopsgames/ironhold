package kingdom.smp.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Thrown pitchfork projectile — behaves like a trident.
 * Flies tip-first, sticks in the ground/entities, and can be picked back up.
 */
public class ThrownPitchforkEntity extends AbstractArrow implements GeoAnimatable {

    private static final String TAG_STACK = "PitchforkStack";
    private static final float BASE_DAMAGE = 8.0F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Stored so we can return the exact stack (with durability) on pickup. */
    private ItemStack storedStack = Ironhold.PITCHFORK.get().getDefaultInstance();

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Deserialization / entity type constructor. */
    public ThrownPitchforkEntity(EntityType<? extends ThrownPitchforkEntity> type, Level level) {
        super(type, level);
        this.setBaseDamage(BASE_DAMAGE);
    }

    /** Called when a player throws the pitchfork. */
    public ThrownPitchforkEntity(LivingEntity thrower, Level level, ItemStack stack) {
        super(Ironhold.THROWN_PITCHFORK.get(), thrower, level, stack, stack);
        this.storedStack = stack.copy();
        this.setBaseDamage(BASE_DAMAGE);
    }

    // ── Pickup ────────────────────────────────────────────────────────────────

    @Override
    protected ItemStack getDefaultPickupItem() {
        return storedStack.copy();
    }

    // ── Hit behaviour ─────────────────────────────────────────────────────────

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        // AbstractArrow handles the damage via baseDamage; no extra logic needed
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.read(TAG_STACK, ItemStack.CODEC).isPresent()) {
            storedStack = input.read(TAG_STACK, ItemStack.CODEC).get();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store(TAG_STACK, ItemStack.CODEC, storedStack);
    }

    // ── GeckoLib ─────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations — static model flying through the air
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
