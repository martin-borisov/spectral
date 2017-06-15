package mb.spectrum.view;

import javafx.scene.CacheHint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Bar {
	
	public static final int DEFAULT_BAR_HEIGHT = 3;
	private static final int BAR_DROP_RATE = 6;
	private static final int TRAIL_DROP_RATE = 2;
	private static final int TRAIL_VISIBILITY_LIMIT = 6;
	private static final int TRAIL_PAUSE_FRAMES = 40;
	
	private double x, y, width, value, trailValue;
	private int trailPauseCounter;
	private Rectangle bar, trail;

	public Bar(double x, double y, double width) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.trailPauseCounter = -1;
		
		bar = new Rectangle(x, y, width, DEFAULT_BAR_HEIGHT);
		bar.setFill(Color.LAWNGREEN);
		
		// NB: This is too slow on the RPi
//		Reflection reflection = new Reflection();
//		reflection.setFraction(0.2);
//		bar.setEffect(reflection);
//		bar.setCache(true);
		
		trail = new Rectangle(x, y - DEFAULT_BAR_HEIGHT, width, DEFAULT_BAR_HEIGHT);
		trail.setFill(Color.RED);
		
		// Try to improve animation performance (worse on MacOS)
		bar.setCache(true);
		bar.setCacheHint(CacheHint.SCALE);
		trail.setCache(true);
		trail.setCacheHint(CacheHint.SPEED);
	}

	public double getValue() {
		return value;
	}

	public void setValue(double newValue) {
		if(newValue > value) {
			value = newValue;
		}
		if(value > trailValue) {
			trailValue = value;
			trailPauseCounter = 0;
		}
	}
	
	public void nextFrame() {
		if (trailPauseCounter == TRAIL_PAUSE_FRAMES || trailPauseCounter == -1) {
			if (trailValue > DEFAULT_BAR_HEIGHT) {
				trailValue -= TRAIL_DROP_RATE;
			}
			trailPauseCounter = -1;
		} else {
			trailPauseCounter++;
		}
		if(value > 0) {
			value -= BAR_DROP_RATE;
		}
		updateGraphics();
	}
	
	public void updateGraphics() {
		double val = value;
		if(val < DEFAULT_BAR_HEIGHT) {
			val = DEFAULT_BAR_HEIGHT;
		}
		
		bar.setHeight(val);
		bar.setTranslateY(val * -1);
		
		if(trailValue > TRAIL_VISIBILITY_LIMIT) {
			trail.setVisible(true);
			trail.setTranslateY(trailValue * -1);
		} else {
			trail.setVisible(false);
		}
	}
	
	public double getTrailValue() {
		return trailValue;
	}

	public void setTrailValue(double trailValue) {
		this.trailValue = trailValue;
		trail.setTranslateY(trailValue * -1);
	}

	public double getX() {
		return x;
	}
	
	public void setX(double x) {
		this.x = x;
		bar.setX(x);
		trail.setX(x);
	}

	public void setY(double y) {
		this.y = y;
		bar.setY(y);
		trail.setY(y - DEFAULT_BAR_HEIGHT);
	}

	public void setWidth(double width) {
		this.width = width;
		bar.setWidth(width);
		trail.setWidth(width);
	}

	public double getY() {
		return y;
	}

	public double getWidth() {
		return width;
	}

	public Rectangle getBar() {
		return bar;
	}

	public Rectangle getTrail() {
		return trail;
	}
}
