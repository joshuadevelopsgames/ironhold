package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class MirrorRenderState extends EntityRenderState {
    public Direction direction = Direction.SOUTH;
    public int lightCoords;
    public int widthBlocks = 1;
    public int heightBlocks = 2;
    /** The reflection-buffer texture this mirror samples this frame, or null if it has no slot. */
    public @Nullable Identifier surfaceTexture;
}
