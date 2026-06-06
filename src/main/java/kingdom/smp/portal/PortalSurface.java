package kingdom.smp.portal;

import net.minecraft.world.phys.Vec3;

/**
 * An oriented rectangular portal surface in some source dimension. The rectangle is centered at
 * {@code center} with outward unit {@code normal}; {@code right} and {@code up} are unit vectors
 * spanning the plane. The rectangle extends {@code halfWidth} along {@code right} and
 * {@code halfHeight} along {@code up}.
 *
 * <p>This is the surface the see-through view is rendered onto, and the shape the off-axis capture
 * frustum is bound to (mirroring {@code MirrorReflection}'s corner-bounded projection). It is
 * deliberately dimension- and block-shape-agnostic so that both block-plane portals (the Moon's
 * {@code MoonPortalBlock}) and region portals (the Lobby box) reduce to the same primitive.
 */
public record PortalSurface(Vec3 center, Vec3 normal, Vec3 right, Vec3 up,
                            double halfWidth, double halfHeight) {

    /** The four world-space corners: BL, BR, TL, TR (matching the order MirrorReflection bounds). */
    public Vec3[] corners() {
        Vec3 rw = right.scale(halfWidth);
        Vec3 uh = up.scale(halfHeight);
        return new Vec3[] {
            center.subtract(rw).subtract(uh),
            center.add(rw).subtract(uh),
            center.subtract(rw).add(uh),
            center.add(rw).add(uh),
        };
    }
}
