package kingdom.smp.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.HashMap;
import java.util.Map;

/**
 * Render state for {@link ThrownPitchforkRenderer} that satisfies both
 * {@link EntityRenderState} (required by {@code GeoEntityRenderer}) and
 * {@link GeoRenderState} (required by {@code RenderPassInfo}).
 */
public class ThrownPitchforkRenderState extends EntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> dataMap = new HashMap<>();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return dataMap;
    }
}
