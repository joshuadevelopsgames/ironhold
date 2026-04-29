package kingdom.smp.rtf.cell.noise;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.cell.CellField;
import kingdom.smp.rtf.noise.module.MappedNoise;
import kingdom.smp.rtf.noise.module.Noise;

public class CellSampler implements MappedNoise {
	public CellField field;
	@Nullable
	private Provider provider;
	
	public CellSampler(CellField field) {
		this.field = field;
	}
	
	@Override
	public float compute(float x, float z, int seed) {
		return this.field.read(this.provider.cacheCell);
	}

	@Override
	public float minValue() {
		return 0.0F;
	}

	@Override
	public float maxValue() {
		return 1.0F;
	}
	
	public static class Provider implements Visitor {
		@Nullable
		private Cell cacheCell;
		
		public void setCacheCell(Cell cell) {
			this.cacheCell = cell;
		}
		
		public Cell getCacheCell() {
			return this.cacheCell;
		}
		
		@Override
		public Noise apply(Noise input) {
			if(input instanceof CellSampler sampler) {
				CellSampler newSampler = new CellSampler(sampler.field);
				newSampler.provider = this;
				return newSampler;
			}
			if(input instanceof CellSampler.Marker marker) {
				return new CellSampler(marker.field());
			}
			return input;
		}
	}

	public record Marker(CellField field) implements MappedNoise.Marker {
		public static final MapCodec<Marker> CODEC = RecordCodecBuilder.<Marker>mapCodec(instance -> instance.group(
			CellField.CODEC.fieldOf("field").forGetter(Marker::field)
		).apply(instance, Marker::new));
		
		@Override
		public Noise mapAll(Visitor visitor) {
			return visitor.apply(new Marker(this.field));
		}

		@Override
		public MapCodec<Marker> codec() {
			return CODEC;
		}		
	}
}
