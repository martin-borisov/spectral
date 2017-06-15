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
}
