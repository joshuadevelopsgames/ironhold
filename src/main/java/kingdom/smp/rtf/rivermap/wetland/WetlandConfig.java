package kingdom.smp.rtf.rivermap.wetland;

import kingdom.smp.rtf.data.preset.settings.RiverSettings;
import kingdom.smp.rtf.noise.NoiseUtil;
import kingdom.smp.rtf.util.Variance;

public class WetlandConfig {
    public int skipSize;
    public Variance length;
    public Variance width;
    
    public WetlandConfig(RiverSettings.Wetland settings) {
        this.skipSize = Math.max(1, NoiseUtil.round((1.0F - settings.chance) * 10.0F));
        this.length = Variance.of(settings.sizeMin, settings.sizeMax);
        this.width = Variance.of(50.0F, 150.0F);
    }
}
