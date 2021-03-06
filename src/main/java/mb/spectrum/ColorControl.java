package mb.spectrum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 * Currently does not support bidirectional binding
 */
public class ColorControl extends GridPane {
	
	private static final int PARENT_TO_GAP_RATIO = 60;
	private static final int PARENT_TO_COLOR_RECT_SIZE_RATIO = 4;
	
	private ObjectProperty<Color> colorProperty;
	private Gauge hueGauge, satGauge, brGauge, opGauge;
	
	private int currentGaugeIdx;
	private List<Gauge> gauges;
	
	public ColorControl(Color color) {
		colorProperty = new SimpleObjectProperty<>(color != null ? color : Color.WHITE);
		createControls();
		layoutControls();
		
		hgapProperty().bind(widthProperty().divide(PARENT_TO_GAP_RATIO));
		vgapProperty().bind(hgapProperty());
		paddingProperty().bind(Bindings.createObjectBinding(
				() -> new Insets(hgapProperty().get()), hgapProperty()));
		
		setAlignment(Pos.CENTER);
	}
	
	public ObjectProperty<Color> colorProperty() {
		return colorProperty;
	}
	
    public void selectNextGauge() {
        unmarkCurrentGauge();
        currentGaugeIdx++;
        if (currentGaugeIdx == gauges.size()) {
            currentGaugeIdx = 0;
        }
        markCurrentGaugeAsSelected();
    }
	
    public void selectPrevGauge() {
        unmarkCurrentGauge();
        currentGaugeIdx--;
        if (currentGaugeIdx < 0) {
            currentGaugeIdx = gauges.size() - 1;
        }
        markCurrentGaugeAsSelected();
    }
    
    public boolean isFirstSelected() {
        return currentGaugeIdx == 0;
    }
    
    public boolean isLastSelected() {
        return currentGaugeIdx == gauges.size() - 1;
    }
    
    public void incrementCurrent() {
        Gauge gauge = gauges.get(currentGaugeIdx);
        final double step = (gauge.getMaxValue() - gauge.getMinValue()) / 20;
        final double value = gauge.getValue() + step;
        gauge.setValue(value > gauge.getMaxValue() ? gauge.getMaxValue() : value);
    }
    
    public void decrementCurrent() {
        Gauge gauge = gauges.get(currentGaugeIdx);
        final double step = (gauge.getMaxValue() - gauge.getMinValue()) / 20;
        final double value = gauge.getValue() - step;
        gauge.setValue(value < gauge.getMinValue() ? gauge.getMinValue() : value);
    }

	private void createControls() {
		hueGauge = createGauge(0, 360, colorProperty.get().getHue(), "Hue", 0);
		satGauge = createGauge(0, 1, colorProperty.get().getSaturation(), "Saturation", 2);
		brGauge = createGauge(0, 1, colorProperty.get().getBrightness(), "Brightness", 2);
		opGauge = createGauge(0, 1, colorProperty.get().getOpacity(), "Opacity", 2);
		gauges = new ArrayList<>(Arrays.asList(hueGauge, brGauge, opGauge, satGauge));
		markCurrentGaugeAsSelected();
        
        // Bind all controls to the main color property
        colorProperty.bind(Bindings.createObjectBinding(
        		() -> {
        			return Color.hsb(hueGauge.valueProperty().get(), satGauge.valueProperty().get(), 
        					brGauge.valueProperty().get(), opGauge.valueProperty().get());
        		}, hueGauge.valueProperty(), satGauge.valueProperty(), brGauge.valueProperty(), opGauge.valueProperty()));
	}
	
	private void layoutControls() {
		add(hueGauge, 0, 0);
		add(satGauge, 0, 1);
		add(brGauge, 1, 0);
		add(opGauge, 1, 1);
	}
	
	private Gauge createGauge(double min, double max, double value, String label, int decimals) {
	    Gauge gauge = GaugeBuilder.create()
	            .minValue(min)
	            .maxValue(max)
	            .value(value)
	            .skinType(SkinType.SPACE_X)
	            .decimals(decimals)
	            .title(label)
	            .barBackgroundColor(Color.gray(0.1))
	            .thresholdVisible(false)
	            .threshold(max)
	            .build();
	    
	    gauge.setBackground(new Background(new BackgroundFill(
                Color.TRANSPARENT, new CornerRadii(20), Insets.EMPTY)));
	    gauge.barColorProperty().bind(colorProperty);
	    return gauge;
	}
	
    private void unmarkCurrentGauge() {
        Gauge gauge = gauges.get(currentGaugeIdx);
        gauge.setBackgroundPaint(Color.TRANSPARENT);
    }
	
	private void markCurrentGaugeAsSelected() {
	    Gauge gauge = gauges.get(currentGaugeIdx);
	    gauge.setBackgroundPaint(Color.gray(0.4));
	}
}
