package mb.spectrum.view;

import static mb.spectrum.Utils.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ddf.minim.analysis.FFT;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import mb.spectrum.ConfigService;
import mb.spectrum.UiUtils;
import mb.spectrum.Utils;

public abstract class AbstractSpectrumView extends AbstractMixedChannelView {
	
	protected static final int MIN_DB_VALUE = -165;
	private static final int FREQ_LINE_PER_BAR_COUNT = 5;
	private static final int DB_LINES_COUNT = 4;
	private static final double GRID_LABELS_MARGIN_RATIO = 0.1;
	private static final int BAND_DROP_RATE_DB = 2;
	private static final int TRAIL_PAUSE_FRAMES = 40;
	private static final double TRAIL_DROP_RATE_DB = 0.5;
	
	// Configurable properties
	private static final int SAMPLING_RATE = Integer.valueOf(
			ConfigService.getInstance().getProperty("mb.spectrum.sampling-rate"));
	private static final int BUFFER_SIZE = Integer.valueOf(
			ConfigService.getInstance().getProperty("mb.spectrum.buffer-size"));
	private SimpleObjectProperty<Color> propGridColor;
	
	private List<Line> vLines, hLines;
	private List<Label> vLabels, hLabels;
	
	private FFT fft;
	
	// Operational properties
	protected List<SimpleDoubleProperty> bandValues;
	protected List<SimpleDoubleProperty> trailValues;
	
	protected int bandCount;
	private double[] bandValuesDB, trailValuesDB;
	private int[] trailPauseCounters;

	@Override
	public List<ObjectProperty<? extends Object>> getProperties() {
		return Arrays.asList(propGridColor);
	}
	
	@Override
	protected void initProperties() {
		propGridColor = new SimpleObjectProperty<>(null, "Grid Color", Color.web("#fd4a11"));
		bandValues = new ArrayList<>();
		trailValues = new ArrayList<>();
	}

	@Override
	protected List<Node> collectNodes() {
		
		vLines = new ArrayList<>();
		hLines = new ArrayList<>();
		vLabels = new ArrayList<>();
		hLabels = new ArrayList<>();
		
		// Get number of bands
		fft = new FFT(BUFFER_SIZE, SAMPLING_RATE);
		fft.logAverages(22, 3);
				
		bandCount = fft.avgSize();
				
		bandValuesDB = new double[bandCount];
		Arrays.fill(bandValuesDB, MIN_DB_VALUE);
		for (int i = 0; i < bandCount; i++) {
			bandValues.add(new SimpleDoubleProperty(MIN_DB_VALUE));
		}
				
		trailValuesDB = new double[bandCount];
		Arrays.fill(trailValuesDB, MIN_DB_VALUE);
		for (int i = 0; i < bandCount; i++) {
			trailValues.add(new SimpleDoubleProperty(MIN_DB_VALUE));
		}
				
		trailPauseCounters = new int[bandCount];
		
		for (int i = 0; i < bandCount; i++) {
			
			// Create grid lines and labels
			if(i % FREQ_LINE_PER_BAR_COUNT == 0) {
				createHzLineAndLabel(i, Math.round(fft.getAverageCenterFrequency(i) - fft.getAverageBandWidth(i) / 2));
			} else if(i == bandCount - 1) {
				createHzLineAndLabel(i + 1, Math.round(fft.getAverageCenterFrequency(i + 1) - fft.getAverageBandWidth(i + 1) / 2));
			}
		}
		
		// DB lines and labels (horizontal)
		for (int i = 0; i <= DB_LINES_COUNT; i++) {
			createDbGridLineAndLabel(i);
		}
		
		List<Node> shapes = new ArrayList<>();
		shapes.addAll(vLines);
		shapes.addAll(vLabels);
		shapes.addAll(hLines);
		shapes.addAll(hLabels);
		return shapes;
	}
	
	@Override
	public void dataAvailable(float[] data) {
		
		// Perform forward FFT
		fft.forward(data);
		
		// Update band values
		for (int i = 0; i < bandCount; i++) {
			double bandDB = Utils.toDB(fft.getAvg(i), fft.timeSize());
			
			bandDB = bandDB < MIN_DB_VALUE ? MIN_DB_VALUE : bandDB;
			if(bandDB > bandValuesDB[i]) {
				bandValuesDB[i] = bandDB;
			}
			if(bandDB > trailValuesDB[i]) {
				trailValuesDB[i] = bandDB;
				trailPauseCounters[i] = 0;
			}
		}
	}
	
	@Override
	public void nextFrame() {
		for (int i = 0; i < bandValuesDB.length; i++) {

			bandValues.get(i).set(bandValuesDB[i]);
			trailValues.get(i).set(trailValuesDB[i]);
			
			// Curve drop
			if(bandValuesDB[i] > MIN_DB_VALUE) {
				bandValuesDB[i] -= BAND_DROP_RATE_DB;
				bandValuesDB[i] = bandValuesDB[i] < MIN_DB_VALUE ? MIN_DB_VALUE : bandValuesDB[i];
			}
			
			// Trail drop
			if (trailPauseCounters[i] == TRAIL_PAUSE_FRAMES || trailPauseCounters[i] == -1) {
				if (trailValuesDB[i] > MIN_DB_VALUE) {
					trailValuesDB[i] -= TRAIL_DROP_RATE_DB;
				}
				trailPauseCounters[i] = -1;
			} else {
				trailPauseCounters[i]++;
			}
		}
	}
	
	private void createHzLineAndLabel(int barIdx, int hz) {
		
		// Create line
		Line line = new Line();
		line.startXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double parentWidth = getRoot().widthProperty().get();
					double barW = (parentWidth - parentWidth * SCENE_MARGIN_RATIO * 2) / bandCount;
					return parentWidth * SCENE_MARGIN_RATIO + barIdx * barW;
				}, 
				getRoot().widthProperty()));
		line.endXProperty().bind(line.startXProperty());
		line.startYProperty().bind(getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO));
		line.endYProperty().bind(getRoot().heightProperty().subtract(
				getRoot().heightProperty().multiply(SCENE_MARGIN_RATIO)));
		line.strokeProperty().bind(propGridColor);
		line.getStrokeDashArray().addAll(2d);
		line.setCache(true);
		vLines.add(line);
		
		// Create label
		Label label = UiUtils.createLabel(hz + "Hz", vLabels);
		label.layoutXProperty().bind(
				line.startXProperty().subtract(
						label.widthProperty().divide(2)));
		label.layoutYProperty().bind(line.endYProperty().add(label.heightProperty().multiply(GRID_LABELS_MARGIN_RATIO)));
		label.textFillProperty().bind(propGridColor);
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 4,
						getRoot().widthProperty())));
	}
	
	private void createDbGridLineAndLabel(int idx) {
		
		double dBVal = map(idx, 0, DB_LINES_COUNT, MIN_DB_VALUE, 0);
		
		// Create line
		Line line = new Line();
		line.startXProperty().bind(getRoot().widthProperty().multiply(SCENE_MARGIN_RATIO));
		line.endXProperty().bind(getRoot().widthProperty().subtract(
				getRoot().widthProperty().multiply(SCENE_MARGIN_RATIO)));
		line.startYProperty().bind(
				Bindings.createDoubleBinding(() -> {
					double parentHeigth = getRoot().heightProperty().get();
					return map(dBVal, MIN_DB_VALUE, 0,  
							parentHeigth - parentHeigth * SCENE_MARGIN_RATIO,
							parentHeigth * SCENE_MARGIN_RATIO);
				}, 
				getRoot().heightProperty()));
		line.endYProperty().bind(line.startYProperty());
		line.strokeProperty().bind(propGridColor);
		line.getStrokeDashArray().addAll(2d);
		line.setCache(true);
		hLines.add(line);
		
		// Create label
		Label label = UiUtils.createLabel(Math.round(dBVal) + "dB", hLabels);
		label.layoutXProperty().bind(
				line.startXProperty().subtract(
						label.widthProperty().add(
								label.widthProperty().multiply(GRID_LABELS_MARGIN_RATIO))));
		label.layoutYProperty().bind(line.startYProperty().subtract(label.heightProperty().divide(2)));
		label.textFillProperty().bind(propGridColor);
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 4,
						getRoot().widthProperty())));
	}
	
	@Override
	public void onShow() {
	}

	@Override
	public void onHide() {
	}
}
