package mb.spectrum.view;

import java.util.Arrays;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import mb.spectrum.UiUtils;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableProperty;

public class SoundWaveView extends AbstractView {
	
	private static final double WAVE_HEIGHT_FACTOR = 0.5;
	
	private ConfigurableColorProperty propLineColor, propBackgroundColor;
	
	private float[] bufferL, bufferR; 
	private GraphicsContext gc;
	
	public SoundWaveView(int bufferSize) {
		super(true);
		bufferL = new float[bufferSize];
		bufferR = new float[bufferSize];
		init();
	}

	@Override
	public String getName() {
		return "Sound Wave View";
	}

	@Override
	protected void initProperties() {
		super.initProperties();
		
		final String keyPrefix = "soundWaveView.";
		
		propLineColor = UiUtils.createConfigurableColorProperty(
				keyPrefix + "lineColor", "Line Color", Color.GREENYELLOW);
		propBackgroundColor = UiUtils.createConfigurableColorProperty(
				keyPrefix + "backgroundColor", "Background Color", Color.gray(0.2));
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(propLineColor, propBackgroundColor);
	}
	
	private Canvas createCanvas() {
		Canvas canvas = new Canvas();
		canvas.widthProperty().bind(getRoot().widthProperty());
		canvas.heightProperty().bind(getRoot().heightProperty());
		return canvas;
	}

	@Override
	protected List<Node> collectNodes() {
		Canvas canvas = createCanvas();
		gc = canvas.getGraphicsContext2D();
	    gc.setLineWidth(1);
		return Arrays.asList(canvas);
	}
	
	@Override
	public void dataAvailable(float[] left, float[] right) {
		System.arraycopy(left, 0, bufferL, 0, right.length);
		System.arraycopy(right, 0, bufferR, 0, right.length);
	}

	@Override
	public void nextFrame() {
		double rootWidth = getRoot().getWidth();
		double rootHeight = getRoot().getHeight();
		double rootHalfHeight = rootHeight / 2;
		gc.clearRect(0, 0, getRoot().getWidth(), rootHeight);
		gc.setStroke(propBackgroundColor.getProp().get());
		gc.strokeLine(0, rootHeight * 0.25, rootWidth, rootHeight * 0.25);
		gc.strokeLine(0, rootHeight * 0.75, rootWidth, rootHeight * 0.75);
		gc.setStroke(propLineColor.getProp().get());
		drawWaveForBuffer(bufferL, rootHeight * 0.25, rootHalfHeight * WAVE_HEIGHT_FACTOR);
		drawWaveForBuffer(bufferR, rootHeight * 0.75, rootHalfHeight * WAVE_HEIGHT_FACTOR);

	}
	
	private void drawWaveForBuffer(float[] buffer, double center, double maxSize) {
		double pointWidth = getRoot().getWidth() / buffer.length;
		for (int i = 0; i < buffer.length - 1; i++) {
			double y1 = center - buffer[i] * maxSize;
			double y2 = center - buffer[i + 1] * maxSize;
			gc.strokeLine(i * pointWidth, y1, (i + 1) * pointWidth, y2);
		}
	}
}
