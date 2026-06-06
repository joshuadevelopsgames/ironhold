package kingdom.smp.portal;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A one-way visual link from a {@link PortalSurface} in {@code sourceDim} to a destination anchor in
 * {@code destDim}.
 *
 * <p>Ironhold's portals are <em>non-paired</em> teleports — there is no matching portal frame on the
 * far side to "see through" into. So the see-through view is generated from an explicit destination
 * anchor: the virtual capture camera maps the player's eye (expressed relative to the surface) onto
 * the {@code destAnchor} frame, with the surface normal mapped to {@code destYaw}. The anchor is
 * chosen to match where the teleport actually lands, so looking through the portal previews where
 * you will arrive.
 */
public record PortalLink(PortalSurface surface,
                         ResourceKey<Level> sourceDim,
                         ResourceKey<Level> destDim,
                         Vec3 destAnchor,
                         float destYaw) {

    /** A stable signature for change-detection / logging (independent of float jitter). */
    public String signature() {
        Vec3 c = surface.center();
        return String.format("%s->%s@(%.0f,%.0f,%.0f)|dest(%.0f,%.0f,%.0f)",
            sourceDim.identifier(), destDim.identifier(), c.x, c.y, c.z,
            destAnchor.x, destAnchor.y, destAnchor.z);
    }
}
