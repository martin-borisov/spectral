package mb.spectrum.view;

import static mb.spectrum.UiUtils.createConfigurableIntegerProperty;
import static mb.spectrum.Utils.map;
import static mb.spectrum.Utils.peakLevel;
import static mb.spectrum.Utils.rmsLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import mb.spectrum.Utils;

public class AnalogMeterView extends AbstractMixedChannelView {
	
	private static final int MIN_DB_VALUE = -66; // = MIN_DBU_VALUE - MAX_DBU_VALUE = -42dBu
	private static final int MIN_DBU_VALUE = -42; // dBu
	private static final int MAX_DBU_VALUE = 24; // dBu
	private static final double MIN_DB_ANGLE_RAD = 2.22; // Range is 3.14 to 0
	private static final double MAX_DB_ANGLE_RAD = 0.92; // Range is 3.14 to 0
	private static final double TOP_MARGIN_RATIO = 0.15;
	private static final double DIV_LENGTH_RATIO = 0.02;
	private static final double LINGER_STAY_FACTOR = 0.02;
	private static final double LINGER_ACCELARATION_FACTOR = 1.15;
	
	private SimpleObjectProperty<Integer> propDivCount;
	
	private DoubleProperty currentDbRmsProp, currentDbPeakProp, lingerLevelDbProp;
	
	private double currentDbRms, currentDbPeak;
	private double lingerLevelDb = MIN_DB_VALUE, lingerOpValDb = LINGER_STAY_FACTOR;

	@Override
	public String getName() {
		return "Analog Meter";
	}
	
	@Override
	protected void initProperties() {
		super.initProperties();
		
		// Configuration properties
		propDivCount = createConfigurableIntegerProperty(
				"analogMeterView.divisionsCount", "Divisions Count", 11);
		
		// Operational properties
		currentDbRmsProp = new SimpleDoubleProperty(MIN_DB_VALUE);
		currentDbPeakProp = new SimpleDoubleProperty(MIN_DB_VALUE);
		lingerLevelDbProp = new SimpleDoubleProperty(MIN_DB_VALUE);
	}

	@Override
	public List<ObjectProperty<? extends Object>> getProperties() {
		return Arrays.asList(propDivCount);
	}
	
	@Override
	protected List<Node> collectNodes() {
		List<Node> nodes = new ArrayList<>();
		
		// Grid
		for (int i = 0; i < propDivCount.get(); i++) {
			Line line = createDivisionLine(i);
		    nodes.add(line);
		    nodes.add(createLabel(i, line));
		}
		
		// Indicator
		Line line = new Line();
		line.startXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double angleRad = map(lingerLevelDbProp.get(), MIN_DB_VALUE, 0, 
							MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
					double r = getRoot().widthProperty().get() / 2;
	    			return r + Math.cos(angleRad) * r;
				}, lingerLevelDbProp, getRoot().widthProperty()));
	    line.startYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double angleRad = map(lingerLevelDbProp.get(), MIN_DB_VALUE, 0, 
							MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
	    			double r = getRoot().widthProperty().get() / 2;
	    	        double topMagin = getRoot().heightProperty().get() * TOP_MARGIN_RATIO;
	    	        return r + topMagin - Math.sin(angleRad) * r;
	    		}, lingerLevelDbProp, getRoot().widthProperty(), getRoot().heightProperty()));
		line.endXProperty().bind(getRoot().widthProperty().divide(2));
		line.endYProperty().bind(getRoot().heightProperty());
		
	    // TODO Property
	    line.setStroke(Color.WHITE);
	    
	    nodes.add(line);
	    
		return nodes;
	}
	
	@Override
	public void dataAvailable(float[] data) {
		currentDbRms = Utils.toDB(peakLevel(data));
		currentDbRms = currentDbRms < MIN_DB_VALUE ? MIN_DB_VALUE : currentDbRms;
		currentDbPeak = Utils.toDB(peakLevel(data));
		currentDbPeak = currentDbPeak < MIN_DB_VALUE ? MIN_DB_VALUE : currentDbPeak;
	}

	@Override
	public void nextFrame() {
		
		// Update operational properties from UI thread
		currentDbRmsProp.set(currentDbRms);
		currentDbPeakProp.set(currentDbPeak);
		
		// Update linger levels
		/*
		lingerLevelDb = lingerLevelDb - lingerOpValDb;
		lingerOpValDb = lingerOpValDb * LINGER_ACCELARATION_FACTOR;
		 */
		
		if(currentDbRms > lingerLevelDb) {
			
			/*
			lingerLevelDb = currentDbRms;
			*/
			
			// TODO Accelarate, don't increase constantly
			lingerLevelDb += 1.5;
			lingerOpValDb = LINGER_STAY_FACTOR;
		} else {
			lingerLevelDb = lingerLevelDb - lingerOpValDb;
			lingerOpValDb = lingerOpValDb * LINGER_ACCELARATION_FACTOR;
		}
		
		if(lingerLevelDb < MIN_DB_VALUE) {
			lingerLevelDb = MIN_DB_VALUE;
		}
		lingerLevelDbProp.set(lingerLevelDb);
	}
	
	private Line createDivisionLine(int idx) {
		
		double angleRad = map(idx, 0, propDivCount.get() - 1, MIN_DB_ANGLE_RAD, MAX_DB_ANGLE_RAD);
		
	    Line line = new Line();
	    line.startXProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double r = getRoot().widthProperty().get() / 2;
	    			return r + Math.cos(angleRad) * r;
	    		}, getRoot().widthProperty()));
	    line.startYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double r = getRoot().widthProperty().get() / 2;
	    	        double topMagin = getRoot().heightProperty().get() * TOP_MARGIN_RATIO;
	    	        return r + topMagin - Math.sin(angleRad) * r;
	    		}, getRoot().widthProperty(), getRoot().heightProperty()));
	    line.endXProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double r = getRoot().widthProperty().get() / 2;
	    			double len = getRoot().heightProperty().get() * DIV_LENGTH_RATIO;
	    	        return r + Math.cos(angleRad) * (r - len);
	    		}, getRoot().widthProperty()));
	    line.endYProperty().bind(Bindings.createDoubleBinding(
	    		() -> {
	    			double r = getRoot().widthProperty().get() / 2;
	    	        double topMagin = getRoot().heightProperty().get() * TOP_MARGIN_RATIO;
	    	        double len = getRoot().heightProperty().get() * DIV_LENGTH_RATIO;
	    	        return r + topMagin - Math.sin(angleRad) * (r - len);
	    		}, getRoot().widthProperty(), getRoot().heightProperty()));
	    
	    // TODO Property
	    line.setStroke(Color.WHITE);
	    
	    return line;
	}
	
	private Label createLabel(int idx, Line line) {
	    Label label = new Label(String.valueOf(Math.round(
	    		map(idx, 0, propDivCount.get() - 1, MIN_DBU_VALUE, MAX_DBU_VALUE))) + "dB");
	    label.layoutXProperty().bind(line.startXProperty().subtract(label.widthProperty().divide(2)));
	    label.layoutYProperty().bind(line.startYProperty().subtract(label.heightProperty().multiply(1.5)));
	    
	    // TODO Property
	    label.setTextFill(Color.WHITE);
	    
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", Bindings.createDoubleBinding(
						() -> (getRoot().widthProperty().get() * SCENE_MARGIN_RATIO) / 2.5,
						getRoot().widthProperty())));
	    return label;
	}

}
