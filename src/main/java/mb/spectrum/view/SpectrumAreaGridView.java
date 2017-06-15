package mb.spectrum.view;

import static mb.spectrum.Utils.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ddf.minim.analysis.FFT;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import mb.spectrum.Utils;

/**
 * TODO: Check if the DB lines show real values and refactor the grid drawing code to use coordX() and coordY().
 * TODO: This class should extend {@link AbstractView}
 */
public class SpectrumAreaGridView extends MixedChannelView {
	
	private static final int INIT_SCENE_WIDTH = 800;
	private static final int INIT_SCENE_HEIGHT = 600;
	private static final int SCENE_MARGIN = 45;
	private static final int FREQ_LINE_PER_BAR_COUNT = 5;
	private static final int DB_LINES_COUNT = 4;
	private static final int BAND_DROP_RATE_DB = 2;
	private static final int TRAIL_PAUSE_FRAMES = 40;
	private static final double TRAIL_DROP_RATE_DB = 0.5;
	
	// NB: The value of -165 is empirical and it acts as a threshold in order to avoid
	// flicker of the graph caused by very low audio levels
	private static final int MIN_DB_VALUE = -165;
	
	private GraphSceneWrapper sw;
	private Path curvePath, trailPath;
	private List<Line> vLines, hLines;
	private List<Text> vLabels, hLabels;
	private int bufferSize, samplingRate, bandCount;
	private double maxBandHeight;
	private double[] bandValuesDB, trailValuesDB;
	private int[] trailPauseCounters;

	public SpectrumAreaGridView(int samplingRate, int bufferSize) {
		this.samplingRate = samplingRate;
		this.bufferSize = bufferSize;
		vLines = new ArrayList<>();
		hLines = new ArrayList<>();
		vLabels = new ArrayList<>();
		hLabels = new ArrayList<>();
		createScene();
		createNodes();
		setupScene();
//		createTimer();
	}

	@Override
	public Scene getScene() {
        return sw.getScene();
	}

	@Override
	public void dataAvailable(float[] data) {
		
		// Perform forward FFT
		FFT fft = new FFT(bufferSize, samplingRate);
		fft.logAverages(22, 3);
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
			LineTo curveLine = (LineTo) curvePath.getElements().get(i + 1);
			curveLine.setY(coordY(map(bandValuesDB[i], MIN_DB_VALUE, 0, 0, sw.getSceneHeight())));
			
			LineTo trailLine = (LineTo) trailPath.getElements().get(i + 1);
			trailLine.setY(coordY(map(trailValuesDB[i], MIN_DB_VALUE, 0, 0, sw.getSceneHeight())));
			
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
	
	private void createTimer() {
		new Timer().schedule(new TimerTask() {
			public void run() {
				for (int i = 0; i < bandValuesDB.length; i++) {
					if(bandValuesDB[i] > MIN_DB_VALUE) {
						if (bandValuesDB[i] > MIN_DB_VALUE) {
							bandValuesDB[i] -= 2;
							bandValuesDB[i] = bandValuesDB[i] < MIN_DB_VALUE ? MIN_DB_VALUE : bandValuesDB[i];
						}
					}
				}
			}
		}, 0, 10);
	}
	
	private void createNodes() {
		
		// Get number of bands
		FFT fft = new FFT(bufferSize, samplingRate);
		fft.logAverages(22, 3);
		
		bandCount = fft.avgSize();
		bandValuesDB = new double[bandCount];
		Arrays.fill(bandValuesDB, MIN_DB_VALUE);
		trailValuesDB = new double[bandCount];
		Arrays.fill(trailValuesDB, MIN_DB_VALUE);
		trailPauseCounters = new int[bandCount];
		maxBandHeight = sw.getSceneHeight();
		double barWidth = sw.getSceneWidth() / bandCount;
		int x = 0;
		
		// Bands and freq. lines and labels
		curvePath = new Path();
		curvePath.setStroke(Color.LAWNGREEN);
		curvePath.setFill(Color.web("#7CFC00", 0.5));
		curvePath.getElements().add(new MoveTo(coordX(0), coordY(0)));
		trailPath = new Path();
		trailPath.setStroke(Color.DARKGREEN);
		trailPath.setStrokeWidth(2);
		trailPath.getElements().add(new MoveTo(coordX(0), coordY(0)));
		for (int i = 0; i < bandCount; i++) {
			
			// Create line segments
			LineTo curveLine = new LineTo();
			curveLine.setX(coordX(x + barWidth / 2));
			curveLine.setY(coordY(0));
			curvePath.getElements().add(curveLine);
			
			LineTo trailLine = new LineTo();
			trailLine.setX(coordX(x + barWidth / 2));
			trailLine.setY(coordY(0));
			trailPath.getElements().add(trailLine);
			
			// Create grid lines and labels
			if(i % FREQ_LINE_PER_BAR_COUNT == 0) {
				createLine(coordX(x), coordY(0 - Bar.DEFAULT_BAR_HEIGHT), coordX(x), coordY(sw.getSceneHeight()), vLines);			
				createLabel(coordX(x), sw.getSceneHeight() - Bar.DEFAULT_BAR_HEIGHT, 
						String.valueOf(Math.round(fft.getAverageCenterFrequency(i) - fft.getAverageBandWidth(i) / 2)) + "Hz", vLabels);
			} else if(i == bandCount - 1) {
				createLine(x + barWidth, SCENE_MARGIN, x + barWidth, 
						INIT_SCENE_HEIGHT - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT, vLines) ;
				createLabel(x + barWidth, INIT_SCENE_HEIGHT - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT, 
						String.valueOf(Math.round(fft.getAverageCenterFrequency(i) + fft.getAverageBandWidth(i) / 2)) + "Hz", vLabels);
			}
			
			x += barWidth;
		}
		LineTo curveLine = new LineTo();
		curveLine.setX(coordX(sw.getSceneWidth()));
		curveLine.setY(coordY(0));
		curvePath.getElements().add(curveLine);
		
		LineTo trailLine = new LineTo();
		trailLine.setX(coordX(sw.getSceneWidth()));
		trailLine.setY(coordY(0));
		trailPath.getElements().add(trailLine);
		
		// DB lines and labels
		for (int i = 0; i < DB_LINES_COUNT; i++) {
			double dbVal = Utils.map(i, 0, DB_LINES_COUNT, 0, -165);
			double yValue = Utils.map(dbVal, 0, -165, SCENE_MARGIN, maxBandHeight + SCENE_MARGIN);
			createLine(SCENE_MARGIN - Bar.DEFAULT_BAR_HEIGHT, yValue, 
					INIT_SCENE_WIDTH - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT, yValue, hLines);
			Text label = createLabel(SCENE_MARGIN - Bar.DEFAULT_BAR_HEIGHT, yValue, String.valueOf(Math.round(dbVal)), hLabels);
			label.setX(SCENE_MARGIN - Bar.DEFAULT_BAR_HEIGHT * 2 - label.getLayoutBounds().getWidth());
			label.setY(yValue + label.getLayoutBounds().getHeight() / 2);
		}
	}
	
	private void createScene() {
        Scene scene = new Scene(new Group(), 
        		INIT_SCENE_WIDTH, INIT_SCENE_HEIGHT, false, SceneAntialiasing.DISABLED);
        scene.setFill(Color.BLACK);
        scene.widthProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, 
					Number oldValue, Number newValue) {
				onSceneWidthChange(oldValue, newValue);
			}
		});
        scene.heightProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> observable, 
					Number oldValue, Number newValue) {
				onSceneHeightChange(oldValue, newValue);
			}
		});
        sw = new GraphSceneWrapper(scene, SCENE_MARGIN);
	}
	
	private void setupScene() {
		((Group) sw.getScene().getRoot()).getChildren().addAll(collectAllShapes());
	}
	
	private void onSceneWidthChange(Number oldValue, Number newValue) {
		
		// Recalculate bar and line properties
		double barWidth = sw.getSceneWidth() / bandCount;
		double x = 0;
		
		int j = 0;
		for (int i = 0; i < bandCount; i++) {
			
			// Update band
			LineTo curveLine = (LineTo) curvePath.getElements().get(i + 1);
			curveLine.setX(coordX(x + barWidth / 2));
			
			LineTo trailLine = (LineTo) trailPath.getElements().get(i + 1);
			trailLine.setX(coordX(x + barWidth / 2));
			
			// Update grid line and label
			if(i % FREQ_LINE_PER_BAR_COUNT == 0) {
				Line line = vLines.get(j);
				line.setStartX(coordX(x));
				line.setEndX(coordX(x));
				
				Text label = vLabels.get(j++);
				label.setX(coordX(x - label.getLayoutBounds().getWidth() / 2));
			} else if(i == bandCount - 1) {
				Line line = vLines.get(j);
				line.setStartX(coordX(x + barWidth));
				line.setEndX(coordX(x + barWidth));
				
				Text label = vLabels.get(j++);
				label.setX(coordX(x + barWidth - label.getLayoutBounds().getWidth() / 2));
			}
			x += barWidth;
		}
		LineTo curveLine = (LineTo) curvePath.getElements().get(curvePath.getElements().size() - 1);
		curveLine.setX(coordX(sw.getSceneWidth()));
		
		LineTo trailLine = (LineTo) trailPath.getElements().get(trailPath.getElements().size() - 1);
		trailLine.setX(coordX(sw.getSceneWidth()));
		
		// Update DB lines
		for (Line line : hLines) {
			line.setEndX(newValue.doubleValue() - SCENE_MARGIN);
		}
	}
	
	private void onSceneHeightChange(Number oldValue, Number newValue) {
		
		// Curve
		MoveTo curveMove = (MoveTo) curvePath.getElements().get(0);
		curveMove.setY(getScene().getHeight() - SCENE_MARGIN);
		for (int i = 1; i < curvePath.getElements().size() - 1; i++) {
			LineTo line = (LineTo) curvePath.getElements().get(i);
			line.setY(map(line.getY(), 0, oldValue.doubleValue(), 0, getScene().getHeight()));
		}
		LineTo curveLastLine = (LineTo) curvePath.getElements().get(curvePath.getElements().size() - 1);
		curveLastLine.setY(sw.coordY(0));
		
		// Trail
		MoveTo trailMove = (MoveTo) trailPath.getElements().get(0);
		trailMove.setY(getScene().getHeight() - SCENE_MARGIN);
		for (int i = 1; i < trailPath.getElements().size() - 1; i++) {
			LineTo line = (LineTo) trailPath.getElements().get(i);
			line.setY(map(line.getY(), 0, oldValue.doubleValue(), 0, getScene().getHeight()));
		}
		LineTo trailLastLine = (LineTo) trailPath.getElements().get(trailPath.getElements().size() - 1);
		trailLastLine.setY(sw.coordY(0));
		
		// Freq. lines and labels
		for (Line line : vLines) {
			line.setStartY(sw.coordY(0));
			line.setEndY(sw.coordY(sw.getSceneHeight()));
		}
		for (Text label : vLabels) {
			label.setY(newValue.intValue() - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT + label.getLayoutBounds().getHeight());
		}
		maxBandHeight = newValue.intValue() - SCENE_MARGIN * 2;
		
		// DB lines and labels
		for (int i = 0; i < hLines.size(); i++) {
			double dbVal = Utils.map(i, 0, hLines.size(), 0, -165);
			double yValue = Utils.map(dbVal, 0, -165, SCENE_MARGIN, maxBandHeight + SCENE_MARGIN);
			
			Line line = hLines.get(i);
			line.setStartY(yValue);
			line.setEndY(yValue);
			
			Text label = hLabels.get(i);
			label.setY(yValue + label.getLayoutBounds().getHeight() / 3);
		}
	}
	
	private List<Node> collectAllShapes() {
		List<Node> shapes = new ArrayList<>();
		shapes.addAll(vLines);
		shapes.addAll(vLabels);
		shapes.addAll(hLines);
		shapes.addAll(hLabels);
		shapes.add(curvePath);
		shapes.add(trailPath);
		return shapes;
	}
	
	private Line createLine(double startX, double startY, double endX, double endY, List<Line> list) {
		Line line = new Line(startX, startY, endX, endY);
		line.setStroke(Color.web("#fd4a11"));
		line.getStrokeDashArray().addAll(2d);
		line.setCache(true);
		list.add(line);
		return line;
	}
	
	private Text createLabel(double x, double y, String text, List<Text> list) {
		Text label = new Text(x, y, text);
		label.setStroke(Color.web("#fd4a11"));
		label.setCache(true);
		list.add(label);
		return label;
	}
	
	protected double coordX(double x) {
		return sw.coordX(x);
	}

	protected double coordY(double y) {
		return sw.coordY(y);
	}
}
