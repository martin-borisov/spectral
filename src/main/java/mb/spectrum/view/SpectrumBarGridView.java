package mb.spectrum.view;

import java.util.ArrayList;
import java.util.List;

import ddf.minim.analysis.FFT;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import mb.spectrum.Utils;

/**
 * This class is the initial implementation of a view and thus 
 * does not extend {@link AbstractView}, but it will be a good idea 
 * if it does at some point in time.
 */
public class SpectrumBarGridView extends MixedChannelView {
	
	private static final int INIT_SCENE_WIDTH = 800;
	private static final int INIT_SCENE_HEIGHT = 600;
	private static final int SCENE_MARGIN = 45;
	private static final int BAND_GAP = 1;
	private static final int FREQ_LINE_PER_BAR_COUNT = 5;
	private static final int DB_LINES_COUNT = 4;
	
	private List<Bar> bars;
	private List<Line> vLines, hLines;
	private List<Text> vLabels, hLabels;
	private int bufferSize, samplingRate, bandCount, maxBandHeight;

	public SpectrumBarGridView(int samplingRate, int bufferSize) {
		this.samplingRate = samplingRate;
		this.bufferSize = bufferSize;
		bars = new ArrayList<>();
		vLines = new ArrayList<>();
		hLines = new ArrayList<>();
		vLabels = new ArrayList<>();
		hLabels = new ArrayList<>();
		setupBands();
	}

	@Override
	public Scene getScene() {
		
		/*
		ImageView image = new ImageView(
				new Image(Spectrum2.class.getResourceAsStream("/vu.png")));
		image.setX(250);
		// image.setY(300);
		
		List<Node> nodes = getAllRectangles();
		nodes.add(image);
		image.setClip(nodes.remove(34));
		*/
		
        Scene scene = new Scene(new Group(collectAllShapes()), 
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
        return scene;
	}

	@Override
	public void dataAvailable(float[] data) {
		FFT fft = new FFT(bufferSize, samplingRate);
		fft.logAverages(22, 3);
		fft.forward(data);
		
		// Update band values (old impl)
		/*
		for (Band band : bands) {
			int value = (int) fft.getBand(band.getSpecIndex());
			band.setValue(value);
		}
		*/
		
		// Update band values
		for (int i = 0; i < bandCount; i++) {
//			bands.get(i).setValue((int) fft.getAvg(i));
			double bandDB = Utils.toDB(fft.getAvg(i), fft.timeSize());
			
			// NB: The value of -160 is empirical and it acts as a threshold in order to avoid
			// flicker of the bars caused by very low levels
			bars.get(i).setValue(Utils.map(bandDB, -165, 0, 0, maxBandHeight));
		}
	}

	@Override
	public void nextFrame() {
		for (Bar band : bars) {
			band.nextFrame();
		}
	}
	
	private void setupBands() {
		
		// Get number of bands and their properties
		FFT fft = new FFT(bufferSize, samplingRate);
		fft.logAverages(22, 3);
		
		bandCount = fft.avgSize();
		maxBandHeight = INIT_SCENE_HEIGHT - SCENE_MARGIN * 2;
		int barWidth = (INIT_SCENE_WIDTH - SCENE_MARGIN * 2) / bandCount - BAND_GAP;
		int x = SCENE_MARGIN;
		int y = INIT_SCENE_HEIGHT - SCENE_MARGIN;
		
		// Bands and freq. lines and labels
		for (int i = 0; i < bandCount; i++) {
			
			// Create band
			bars.add(new Bar(x, y, barWidth));
			
			// Create grid lines and labels
			if(i % FREQ_LINE_PER_BAR_COUNT == 0) {
				createLine(x, SCENE_MARGIN, x, INIT_SCENE_HEIGHT - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT, vLines);			
				createLabel(x, INIT_SCENE_HEIGHT - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT, 
						String.valueOf(Math.round(fft.getAverageCenterFrequency(i) - fft.getAverageBandWidth(i) / 2)) + "Hz", vLabels);
			} else if(i == bandCount - 1) {
				createLine(x + barWidth, SCENE_MARGIN, x + barWidth, 
						INIT_SCENE_HEIGHT - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT, vLines) ;
				createLabel(x + barWidth, INIT_SCENE_HEIGHT - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT, 
						String.valueOf(Math.round(fft.getAverageCenterFrequency(i) + fft.getAverageBandWidth(i) / 2)) + "Hz", vLabels);
			}
			
			x += barWidth + BAND_GAP;
		}
		
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
	
	private void onSceneWidthChange(Number oldValue, Number newValue) {
		
		// Recalculate bar and line properties
		double barWidth = (newValue.doubleValue() - SCENE_MARGIN * 2) / bandCount - BAND_GAP;
		double x = SCENE_MARGIN;
		
		int j = 0;
		for (int i = 0; i < bars.size(); i++) {
			
			// Update band
			Bar band = bars.get(i);
			band.setWidth(barWidth);
			band.setX(x);
			
			// Update grid line and label
			if(i % FREQ_LINE_PER_BAR_COUNT == 0) {
				Line line = vLines.get(j);
				line.setStartX(x);
				line.setEndX(x);
				
				Text label = vLabels.get(j++);
				label.setX(x - label.getLayoutBounds().getWidth() / 2);
			} else if(i == bandCount - 1) {
				Line line = vLines.get(j);
				line.setStartX(x + barWidth);
				line.setEndX(x + barWidth);
				
				Text label = vLabels.get(j++);
				label.setX(x + barWidth - label.getLayoutBounds().getWidth() / 2);
			}
			x += barWidth + BAND_GAP;
		}
		
		// Update DB lines
		for (Line line : hLines) {
			line.setEndX(newValue.doubleValue() - SCENE_MARGIN);
		}
	}
	
	private void onSceneHeightChange(Number oldValue, Number newValue) {
		
		// Bands
		for (Bar band : bars) {
			band.setY(newValue.intValue() - SCENE_MARGIN);
		}
		
		// Freq. lines and labels
		for (Line line : vLines) {
			line.setEndY(newValue.intValue() - SCENE_MARGIN + Bar.DEFAULT_BAR_HEIGHT);
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
		for (Bar band : bars) {
			shapes.add(band.getBar());
			shapes.add(band.getTrail());
		}
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

}
