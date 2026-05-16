package kingdom.smp.rtf;

public class WorldGenFlags {
	private static final ThreadLocal<Boolean> FAST_LOOKUP = ThreadLocal.withInitial(() -> true);
	/**
	 * Depth counter for {@link net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator#iterateNoiseColumn}
	 * (used by {@code getBaseHeight} / {@code getBaseColumn}). Those paths construct a 1-wide {@code NoiseChunk}
	 * for structure height probes; we must not kick off tile pre-generation from there or we queue hundreds of
	 * cold tiles and blow the heap.
	 */
	private static final ThreadLocal<Integer> AMBIENT_NOISE_COLUMN_DEPTH = ThreadLocal.withInitial(() -> 0);
	private static boolean CULL_NOISE_SECTIONS = true;
	
	public static void setFastCellLookups(boolean fastLookups) {
		FAST_LOOKUP.set(fastLookups);
	}
	
	public static boolean fastLookups() {
		return FAST_LOOKUP.get();
	}
	
	public static void setCullNoiseSections(boolean cullNoiseSections) {
		CULL_NOISE_SECTIONS = cullNoiseSections;
	}
	
	public static boolean cullNoiseSections() {
		return CULL_NOISE_SECTIONS;
	}

	public static void pushAmbientNoiseColumnSampling() {
		AMBIENT_NOISE_COLUMN_DEPTH.set(AMBIENT_NOISE_COLUMN_DEPTH.get() + 1);
	}

	public static void popAmbientNoiseColumnSampling() {
		int d = AMBIENT_NOISE_COLUMN_DEPTH.get() - 1;
		AMBIENT_NOISE_COLUMN_DEPTH.set(Math.max(0, d));
	}

	/** {@code true} while inside iterateNoiseColumn from height/column probes (see NoiseBasedChunkGeneratorRTFMixin). */
	public static boolean isAmbientNoiseColumnSampling() {
		return AMBIENT_NOISE_COLUMN_DEPTH.get() > 0;
	}
}
