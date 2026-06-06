package kingdom.smp.client.emote;

/**
 * Duck interface mixed into vanilla {@code AvatarRenderState} so the player
 * model can read a per-player "pointing" blend amount during setupAnim. The
 * value is 0 when not pointing and eases up to 1 at the held pose.
 */
public interface PointableRenderState {
    void ironhold$setPoint(float amount);
    float ironhold$point();
}
