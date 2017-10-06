package mb.spectrum;

public class Utils {

	public static double map(double s, double a1, double a2, double b1, double b2) {
		return b1 + (s - a1) * (b2 - b1) / (a2 - a1);
	}
	
	public static double toDB(double value, int bufferSize) {
		double valueDb = 0;
		if(value > 0) {
			valueDb = 20 * Math.log( 2 * value / bufferSize);
		}
		return valueDb;
	}
	
	public static double toDB(double value) {
		return 20 * ( (float) Math.log10(value) );
	}
	
	public static float rmsLevel(float[] samples) {
		float level = 0.0F;
		for (int i = 0; i < samples.length; i++) {
			level += samples[i] * samples[i];
		}
		level /= samples.length;
		level = (float) Math.sqrt(level);
		return level;
	}
	
	public static final float peakLevel(float[] samples) {
		float max = 0;
		for (int i = 0; i < samples.length; i++) {
			float sample = Math.abs(samples[i]);
			if(sample > max) {
				max = sample;
			}
		}
		return max;
	}
}
