package mb.spectrum;

import static mb.spectrum.Utils.map;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Section;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import javafx.util.Duration;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableChoiceProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.prop.IncrementalActionProperty;

public class UiUtils {
    
    private static final int SHUTDOWN_COUNTDOWN_TIME_S = 10;
	
	public static Line createGridLine(double startX, double startY, double endX, double endY, Color color, List<Line> list) {
		Line line = new Line(startX, startY, endX, endY);
		line.setStroke(color);
		line.getStrokeDashArray().addAll(2d);
		line.setCache(true);
		list.add(line);
		return line;
	}
	
	public static Label createLabel(String text, List<Label> list) {
		Label label = new Label(text);
		label.setCache(true);
		list.add(label);
		return label;
	}
	
	public static Transition createFadeInOutTransition(Node node, double fadeInMs, 
			double lingerMs, double fadeOutMs, EventHandler<ActionEvent> handler) {
		
		FadeTransition fadeIn = new FadeTransition(Duration.millis(fadeInMs), node);
		fadeIn.setFromValue(0.0f);
		fadeIn.setToValue(1.0f);
		fadeIn.setCycleCount(1);
		fadeIn.setAutoReverse(false);
		
		FadeTransition fadeOut = new FadeTransition(Duration.millis(fadeOutMs), node);
		fadeOut.setFromValue(1.0f);
		fadeOut.setToValue(0.0f);
		fadeOut.setCycleCount(1);
		fadeOut.setAutoReverse(false);
		fadeOut.setDelay(Duration.millis(lingerMs));
		
		SequentialTransition trans = new SequentialTransition(fadeIn, fadeOut);
		trans.setOnFinished(handler);
		return trans;
	}
	
	public static Transition createFadeInTransition(Node node, double fadeInMs, 
			EventHandler<ActionEvent> handler) {
		FadeTransition fadeIn = new FadeTransition(Duration.millis(fadeInMs), node);
		fadeIn.setFromValue(0.0f);
		fadeIn.setToValue(1.0f);
		fadeIn.setCycleCount(1);
		fadeIn.setAutoReverse(false);
		fadeIn.setOnFinished(handler);
		return fadeIn;
	}
	
	public static Transition createFadeOutTransition(Node node, double fadeInMs, 
			EventHandler<ActionEvent> handler) {
		return createFadeOutTransition(node, fadeInMs, 0.0, handler);
	}
	
	public static Transition createFadeOutTransition(Node node, double fadeInMs, double toValue,
			EventHandler<ActionEvent> handler) {
		FadeTransition fadeOut = new FadeTransition(Duration.millis(fadeInMs), node);
		fadeOut.setFromValue(node.getOpacity());
		fadeOut.setToValue(toValue);
		fadeOut.setCycleCount(1);
		fadeOut.setAutoReverse(false);
		fadeOut.setOnFinished(handler);
		return fadeOut;
	}
	
	public static ColorPicker createColorPropertyColorPicker(Color color, Pane parent) {
		ColorPicker picker = new ColorPicker(color);
		picker.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(40), ";"));
		return picker;
	}
	
	public static Button createActionPropertyButton(String label) {
		Button button = new Button(label);
		return button;
	}
	
    @SuppressWarnings("unchecked")
    public static Gauge createIncrementalActionPropertyGauge(IncrementalActionProperty prop) {
        Gauge gauge = GaugeBuilder.create()
                .minValue(prop.getMinValue())
                .maxValue(prop.getMaxValue())
                .skinType(SkinType.SIMPLE)
                .valueVisible(false)
                .borderPaint(Color.TRANSPARENT) // Border
                .tickLabelColor(Color.TRANSPARENT) // Hide min and max value labels
                .sections(Arrays.asList(
                        new Section(0, 2, "On", Color.rgb(255, 0, 0), Color.WHITE), 
                        new Section(2, 4, Color.rgb(200, 0, 0)), 
                        new Section(4, 6, Color.rgb(150, 0, 0)),
                        new Section(6, 8, "Off", Color.BLACK, Color.WHITE)))
                .sectionTextVisible(true)
                .animated(true)
                .animationDuration(100)
                .build();
        gauge.valueProperty().bind(prop.getProp());
        return gauge;
    }
	
	public static CheckBox createBooleanPropertyCheckBox(Boolean value, String label, Pane parent) {
		CheckBox box = new CheckBox(label);
		box.setSelected(value);
		box.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(40), ";"));
		return box;
	}
	
	/*
	public static ToggleSwitch createBooleanPropertyToggleSwitch(Boolean value, String label, Pane parent) {
	    ToggleSwitch toggle = new ToggleSwitch(label);
	    toggle.setSelected(value);
	    toggle.styleProperty().bind(Bindings.concat(
	                "-fx-font-size: ", parent.widthProperty().divide(50), ";"));
	    toggle.setMouseTransparent(true);
	    toggle.setFocusTraversable(false);
	    return toggle;
	}
	*/
	
	@SuppressWarnings("unchecked")
    public static Region createBooleanPropertyGauge(ConfigurableBooleanProperty prop) {
	    Gauge gauge = GaugeBuilder.create()
	            .minValue(0)
	            .maxValue(4)
	            .skinType(SkinType.SIMPLE)
	            .valueVisible(false)
	            .borderPaint(Color.TRANSPARENT) // Border
	            .tickLabelColor(Color.TRANSPARENT) // Hide min and max value labels
	            .sections(Arrays.asList(
	                    new Section(0, 2, "No", Color.GRAY), 
	                    new Section(2, 4, "Yes", Color.LIME)))
	            .sectionTextVisible(true)
	            .animated(true)
	            .animationDuration(300)
	            .build();
	    gauge.valueProperty().bind(Bindings.createDoubleBinding(() -> {
	        return prop.get() ? 3d : 1d;
        }, prop.getProp()));
	    return gauge;
	}
	
	public static Label createNumberPropertyLabel(String initValue, Pane parent) {
		Label label = new Label(initValue);
		label.setAlignment(Pos.CENTER);
		
		// TODO Play a bit with the values below to find the best fit
		label.styleProperty().bind(Bindings.concat(
				"-fx-font-size: ", parent.widthProperty().divide(20), ";"));
		label.maxWidthProperty().bind(parent.widthProperty());
		return label;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
    public static Gauge createNumberPropertyGauge(ConfigurableProperty prop) {
	    
	    if(!(prop instanceof ConfigurableIntegerProperty || 
	            prop instanceof ConfigurableDoubleProperty)) {
	        throw new IllegalArgumentException("Wrong property type");
	    }
	    
	    Gauge gauge = GaugeBuilder.create()
	        .minValue(prop instanceof ConfigurableIntegerProperty ? 
	                (Integer) prop.getMinValue() : (Double) prop.getMinValue())
	        .maxValue(prop instanceof ConfigurableIntegerProperty ? 
                    (Integer) prop.getMaxValue() : (Double) prop.getMaxValue())
	        .startFromZero(false)
	        .skinType(SkinType.DASHBOARD)
	        .barColor(Color.CYAN)
	        .majorTickMarksVisible(true)
	        .decimals(prop instanceof ConfigurableIntegerProperty ? 0 : 1)
	        .build();
	    gauge.valueProperty().bind(prop.getProp());

	    
	    if(prop.getUnit() != null) {
	        gauge.setUnit(prop.getUnit());
	    }
	    
	    return gauge;
	}
	
	public static ListView<String> createChoicePropertyListView(ConfigurableChoiceProperty prop, Pane parent) {
	    
	    // Reverse the items in the list, as the property value increase/decrease 
	    // with arrow keys is done in reverse order
	    ArrayList<String> items = new ArrayList<>(prop.getAllValues());
	    Collections.reverse(items);
	    
	    ListView<String> list = new ListView<>(
	            FXCollections.<String>observableArrayList(items));
	    list.setMouseTransparent(true);
	    list.setFocusTraversable(false);
	    list.getSelectionModel().select(prop.get());
	    list.styleProperty().bind(Bindings.concat(
                "-fx-font-size: ", parent.widthProperty().divide(40), ";"));
	    return list;
	}
	
	public static Line createThickRoundedLine(Color color) {
		Line line = new Line();
		line.setStroke(color);
		line.setStrokeWidth(4);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		return line;
	}
	
	public static String colorToWeb(Color color) {
		return MessageFormat.format("rgba({0,number,#}, {1,number,#}, {2,number,#}, {3})", 
				map(color.getRed(), 0, 1, 0, 255),
				map(color.getGreen(), 0, 1, 0, 255),
				map(color.getBlue(), 0, 1, 0, 255),
				color.getOpacity());
	}
	
	public static ConfigurableColorProperty createConfigurableColorProperty(String key, String name, Color defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableColorProperty prop = 
				new ConfigurableColorProperty(name, null, null, 
						Color.web(cs.getOrCreateProperty(key, colorToWeb(defaultValue))), null);
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, colorToWeb(newVal));
		});
		return prop;
	}
	
	public static ConfigurableDoubleProperty createConfigurableDoubleProperty(DoubleProperty property, String key, String name, 
			Double minValue, Double maxValue, Double defaultValue, Double increment) {
		ConfigurableDoubleProperty confProp = createConfigurableDoubleProperty(key, name, minValue, maxValue, defaultValue, increment);
		property.bind(confProp.getProp());
		return confProp;
	}
	
	public static ConfigurableDoubleProperty createConfigurableDoubleProperty(String key, String name, 
			Double minValue, Double maxValue, Double defaultValue, Double increment) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableDoubleProperty prop = 
				new ConfigurableDoubleProperty(name, minValue, maxValue, 
						Double.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))), increment);
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static ConfigurableBooleanProperty createConfigurableBooleanProperty(String key, String name, Boolean defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableBooleanProperty prop = 
				new ConfigurableBooleanProperty(name, null, null, 
						Boolean.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))), null);
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static ConfigurableIntegerProperty createConfigurableIntegerProperty(String key, String name, 
			Integer minValue, Integer maxValue, Integer defaultValue, Integer increment, String unit) {
	    ConfigurableIntegerProperty prop = createConfigurableIntegerProperty(
	            key, name, minValue, maxValue, defaultValue, increment);
	    prop.setUnit(unit);
		return prop;
	}
	
	   public static ConfigurableIntegerProperty createConfigurableIntegerProperty(String key, String name, 
	            Integer minValue, Integer maxValue, Integer defaultValue, Integer increment) {
	        ConfigService cs = ConfigService.getInstance();
	        ConfigurableIntegerProperty prop = 
	                new ConfigurableIntegerProperty(name, minValue, maxValue, 
	                        Integer.valueOf(cs.getOrCreateProperty(key, String.valueOf(defaultValue))), increment);
	        prop.getProp().addListener((obs, oldVal, newVal) -> {
	            cs.setProperty(key, String.valueOf(newVal));
	        });
	        return prop;
	    }
	
	public static <T extends Enum<T>> ConfigurableChoiceProperty createConfigurableChoiceProperty(
			String key, String name, Class<T> enumType) {
		return UiUtils.createConfigurableChoiceProperty(key, name, 
				Stream.of(enumType.getEnumConstants()).map(String::valueOf).collect(Collectors.toList()), 
				enumType.getEnumConstants()[0].toString());
	}
	
	public static ConfigurableChoiceProperty createConfigurableChoiceProperty(String key, String name, 
			List<String> values, String defaultValue) {
		ConfigService cs = ConfigService.getInstance();
		ConfigurableChoiceProperty prop = 
				new ConfigurableChoiceProperty(
						name, values, cs.getOrCreateProperty(key, defaultValue));
		prop.getProp().addListener((obs, oldVal, newVal) -> {
			cs.setProperty(key, String.valueOf(newVal));
		});
		return prop;
	}
	
	public static boolean isDouble(String string) {
		return isCreatable(string) && string.contains(".");
	}
	
	public static void runAfter(long seconds, Runnable runnable) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				Platform.runLater(runnable);
			}
		}, seconds * 1000);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> ConfigurableProperty<T> createConfigurableProperty(Property<T> property, String key, String name, Object...values) {
		ConfigurableProperty<T> confProp;
		if(property instanceof DoubleProperty) {
			confProp = (ConfigurableProperty<T>) createConfigurableDoubleProperty(key, name, 
					(double) values[0], (double) values[1], (double) values[2], (double) values[3]);
			property.bind(confProp.getProp());
		} else if(property instanceof ObjectProperty && 
				((ObjectProperty<?>) property).get() instanceof Color) {
			confProp = (ConfigurableProperty<T>) createConfigurableColorProperty(
					key, name, (Color) values[0]);
			property.bind(confProp.getProp());
			
			// TODO Add other supported configurable property types
			
		} else {
			throw new IllegalArgumentException("Unsupported property type: "
					+ property.getClass().getName());
		}
		return confProp;
	}
	
	public static BorderPane createUtilityPane(Pane parent, double widthRatio, double heightRatio, double opacity) {
        BorderPane pane = new BorderPane();
        
        // Automatically resize pane based on the scene size
        pane.minWidthProperty().bind(parent.widthProperty().divide(widthRatio));
        pane.minHeightProperty().bind(parent.heightProperty().divide(heightRatio));
        pane.maxWidthProperty().bind(pane.minWidthProperty());
        pane.maxHeightProperty().bind(pane.minHeightProperty());
        pane.setBackground(new Background(
                new BackgroundFill(Color.rgb(140, 140, 140, opacity), new CornerRadii(5), Insets.EMPTY)));
        pane.setBorder(new Border(new BorderStroke(Color.DARKGREY, 
                BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(2))));
        
        return pane;
    }
	
	public static Region createSVGRegion(String content, ReadOnlyDoubleProperty reference, int factor, Paint color) {
        SVGPath svg = new SVGPath();
        svg.setContent(content);
        
        Region region = new Region();
        region.setShape(svg);
        region.maxWidthProperty().bind(reference.divide(factor));
        region.maxHeightProperty().bind(region.maxWidthProperty());
        region.minWidthProperty().bind(region.maxWidthProperty());
        region.minHeightProperty().bind(region.maxHeightProperty());
        region.setBackground(new Background(new BackgroundFill(
        		color, CornerRadii.EMPTY, Insets.EMPTY)));
        
        return region;
	}
	
	public static void createAndShowShutdownPrompt(Stage stage, boolean shutdownForReal) {
	    Pane parent = (Pane)stage.getScene().getRoot();
	    
	    Label label = createNumberPropertyLabel(
	            String.valueOf(SHUTDOWN_COUNTDOWN_TIME_S), parent);
	    
	    BorderPane pane = createUtilityPane(parent, 1.5, 2, 1);
	    pane.setTop(createNumberPropertyLabel("Power off in", parent));
	    pane.setCenter(label);
	    parent.getChildren().add(pane);
	    
	    
	    KeyFrame frame = new KeyFrame(Duration.seconds(1), (event) -> {
	        int value = Integer.valueOf(label.getText()) - 1;
	        if(value > 0) {
	            label.setText(String.valueOf(value));
	        } else {
	            parent.getChildren().remove(pane);
	            if(shutdownForReal) {
	                SystemUtils.shutdown();
	            } else {
	                System.out.println("Fake shutdown");
	            }
	        }
	    });
	    
        Timeline tl = new Timeline(frame);
        tl.setCycleCount(SHUTDOWN_COUNTDOWN_TIME_S);
        tl.play();
	}
}
