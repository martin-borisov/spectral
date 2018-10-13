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
	
	private ConfigurableColorProperty propLineColor;
	
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
		propLineColor.getProp().addListener((obs, oldValue, newValue) -> {
			gc.setStroke(newValue);
		});
	}

	@Override
	public List<ConfigurableProperty<? extends Object>> getProperties() {
		return Arrays.asList(propLineColor);
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
		gc.setStroke(propLineColor.getProp().get());
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
		gc.clearRect(0, 0, getRoot().getWidth(), getRoot().getHeight());
		drawWaveForBuffer(bufferL, getRoot().getHeight() * 0.25, getRoot().getHeight() / 2);
		drawWaveForBuffer(bufferR, getRoot().getHeight() * 0.75, getRoot().getHeight() / 2);

	}
	
	private void drawWaveForBuffer(float[] buffer, double center, double maxSize) {
		for (int i = 0; i < buffer.length - 1; i++) {
			double y = center - buffer[i] * maxSize;
			double y1 = center - buffer[i + 1] * maxSize;
			gc.strokeLine(i, y, i + 1, y1);
		}
	}
}
