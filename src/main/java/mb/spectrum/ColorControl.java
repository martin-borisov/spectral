package mb.spectrum;

import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Currently does not support bidirectional binding
 */
public class ColorControl extends GridPane {
	
	private static final int PARENT_TO_GAP_RATIO = 20;
	private static final int PARENT_TO_COLOR_RECT_SIZE_RATIO = 4;
	
	private ObjectProperty<Color> colorProperty;
	private Slider hue, saturation, brightness, opacity;
	private Region transparentRect, colorRect;
	
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
	
	public boolean hasMoreSlidersToTheRight(Slider slider) {
		List<Slider> sliders = Arrays.asList(hue, saturation, brightness, opacity);
		int sliderIdx = sliders.indexOf(slider);
		return (sliderIdx > -1) && (sliderIdx < sliders.size() - 1);
	}
	
	public boolean hasMoreSlidersToTheLeft(Slider slider) {
		List<Slider> sliders = Arrays.asList(hue, saturation, brightness, opacity);
		int sliderIdx = sliders.indexOf(slider);
		return (sliderIdx > 0);
	}

	private void createControls() {
		hue = createSlider(0, 360, colorProperty.get().getHue());
		hue.setBlockIncrement(10);
		
		saturation = createSlider(0, 1, colorProperty.get().getSaturation());
		brightness = createSlider(0, 1, colorProperty.get().getBrightness());
		opacity = createSlider(0, 1, colorProperty.get().getOpacity());
		
		transparentRect = new Region();
		transparentRect.prefWidthProperty().bind(widthProperty().divide(PARENT_TO_COLOR_RECT_SIZE_RATIO));
		transparentRect.prefHeightProperty().bind(transparentRect.prefWidthProperty());
		transparentRect.minWidthProperty().bind(transparentRect.prefWidthProperty());
		transparentRect.minHeightProperty().bind(transparentRect.maxWidthProperty());
		transparentRect.maxWidthProperty().bind(transparentRect.prefWidthProperty());
		transparentRect.maxHeightProperty().bind(transparentRect.prefWidthProperty());
		transparentRect.setStyle(
				"-fx-background-image: url(\"pattern-transparent.png\");" + 
				"-fx-background-repeat: repeat;" + 
				"-fx-background-size: auto;");
		
        colorRect = new Region();
		colorRect.prefWidthProperty().bind(widthProperty().divide(PARENT_TO_COLOR_RECT_SIZE_RATIO));
		colorRect.prefHeightProperty().bind(colorRect.prefWidthProperty());
		colorRect.minWidthProperty().bind(colorRect.prefWidthProperty());
		colorRect.minHeightProperty().bind(colorRect.prefWidthProperty());
		colorRect.maxWidthProperty().bind(colorRect.prefWidthProperty());
		colorRect.maxHeightProperty().bind(colorRect.prefWidthProperty());
        colorRect.setStyle(
				"-fx-stroke: black;" + 
				"-fx-stroke-width: 0.4;" + 
				"-fx-border-color: gray;");
        colorRect.backgroundProperty().bind(Bindings.createObjectBinding(
        		() -> {
        			return new Background(new BackgroundFill(colorProperty.get(), CornerRadii.EMPTY, Insets.EMPTY));
        		}, colorProperty));
        
        // Bind all controls to the main color property
        colorProperty.bind(Bindings.createObjectBinding(
        		() -> {
        			return Color.hsb(hue.valueProperty().get(), saturation.valueProperty().get(), 
        					brightness.valueProperty().get(), opacity.valueProperty().get());
        		}, hue.valueProperty(), saturation.valueProperty(), brightness.valueProperty(), opacity.valueProperty()));
	}
	
	private void layoutControls() {
		add(hue, 0, 0);
		add(new Label("H"), 0, 1);
		add(saturation, 1, 0);
		add(new Label("S"), 1, 1);
		add(brightness, 2, 0);
		add(new Label("B"), 2, 1);
		add(opacity, 3, 0);
		add(new Label("O"), 3, 1);
		add(transparentRect, 4, 0, 1, 2);
		add(colorRect, 4, 0, 1, 2);
	}
	
	private Slider createSlider(double min, double max, double value) {
		Slider slider = new Slider(min, max, value);
		slider.setOrientation(Orientation.VERTICAL);
		slider.setBlockIncrement(0.05);
		return slider;
	}
}
