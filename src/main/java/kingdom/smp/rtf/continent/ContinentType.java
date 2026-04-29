package kingdom.smp.rtf.continent;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.continent.advanced.AdvancedContinentGenerator;
import kingdom.smp.rtf.continent.fancy.FancyContinentGenerator;
import kingdom.smp.rtf.continent.infinite.InfiniteContinentGenerator;
import kingdom.smp.rtf.continent.simple.MultiContinentGenerator;
import kingdom.smp.rtf.continent.simple.SingleContinentGenerator;
import kingdom.smp.rtf.util.Seed;

public enum ContinentType implements StringRepresentable {
    MULTI {
        
    	@Override
        public MultiContinentGenerator create(Seed seed, GeneratorContext context) {
            return new MultiContinentGenerator(seed, context);
        }
    }, 
    SINGLE {
        
    	@Override
        public SingleContinentGenerator create(Seed seed, GeneratorContext context) {
            return new SingleContinentGenerator(seed, context);
        }
    }, 
    MULTI_IMPROVED {
        
    	@Override
        public AdvancedContinentGenerator create(Seed seed, GeneratorContext context) {
            return new AdvancedContinentGenerator(seed, context);
        }
    }, 
    EXPERIMENTAL {
        
    	@Override
        public FancyContinentGenerator create(Seed seed, GeneratorContext context) {
            return new FancyContinentGenerator(seed, context);
        }
    },
    INFINITE {
        
    	@Override
        public InfiniteContinentGenerator create(Seed seed, GeneratorContext context) {
            return new InfiniteContinentGenerator(context);
        }
    };
	
	public static final Codec<ContinentType> CODEC = StringRepresentable.fromEnum(ContinentType::values);
    
    public abstract Continent create(Seed seed, GeneratorContext context);

    @Override
	public String getSerializedName() {
		return this.name();
	}
}
