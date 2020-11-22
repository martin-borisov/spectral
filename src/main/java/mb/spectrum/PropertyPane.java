package mb.spectrum;

import java.util.Collections;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import mb.spectrum.prop.ActionProperty;
import mb.spectrum.prop.ConfigurableBooleanProperty;
import mb.spectrum.prop.ConfigurableChoiceProperty;
import mb.spectrum.prop.ConfigurableColorProperty;
import mb.spectrum.prop.ConfigurableDoubleProperty;
import mb.spectrum.prop.ConfigurableIntegerProperty;
import mb.spectrum.prop.ConfigurableProperty;
import mb.spectrum.prop.IncrementalActionProperty;

public class PropertyPane extends BorderPane {
    
    private static final int PROPS_BEFORE_AND_AFTER = 4;
    private static final String ARROW_UP_SVG = "M11 7l-4 6h8"; // Dashicons "arrow-up";
    private static final String ARROW_DOWN_SVG = "M15 8l-4.03 6L7 8h8z"; // Dashicons "arrow-down"
    private static final int FONT_WIDTH_FACTOR = 28;
    private static final int ARROW_WIDTH_FACTOR = 60;
    
    private static final Color HIGHLIGHTED_BG_COLOR = Color.DARKGRAY;
    private static final Color HIGHLIGHTED_BORDER_COLOR = Color.RED;
    
    private static final Background HIGHLIGHTED_LABEL_BG = 
            new Background(new BackgroundFill(HIGHLIGHTED_BG_COLOR, new CornerRadii(5, 0, 0, 5, false), Insets.EMPTY));
    private static final Background HIGHLIGHTED_CONTROL_BG = 
            new Background(new BackgroundFill(HIGHLIGHTED_BG_COLOR, new CornerRadii(5, 5, 5, 5, false), Insets.EMPTY));
    private static final BorderWidths HIGHLIGHTED_BORDER_WIDTHS = BorderStroke.MEDIUM;
    
    // Insets added to prevent the border from increasing the size of the component
    private static final Border SELECTED_LABEL_BORDER = new Border(
            new BorderStroke(HIGHLIGHTED_BORDER_COLOR, Color.DARKGRAY, HIGHLIGHTED_BORDER_COLOR, HIGHLIGHTED_BORDER_COLOR, 
                    BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, 
                    new CornerRadii(5, 0, 0, 5, false), HIGHLIGHTED_BORDER_WIDTHS, new Insets(HIGHLIGHTED_BORDER_WIDTHS.getTop() * -1)));
    private static final Border SELECTED_CONTROL_BORDER = new Border(
            new BorderStroke(HIGHLIGHTED_BORDER_COLOR, BorderStrokeStyle.SOLID, 
                    new CornerRadii(5, 5, 5, 5, false), HIGHLIGHTED_BORDER_WIDTHS, new Insets(HIGHLIGHTED_BORDER_WIDTHS.getTop() * -1)));
    
    private SimpleBooleanProperty selectedProperty;
    private SimpleDoubleProperty widthRatioProperty, heightRatioProperty;
    
    private List<ConfigurableProperty<? extends Object>> properties;
    private int currPropIdx;
    
    private VBox propListBox;
    private Region iconUp, iconDown;
    private VBox controlBox;
    
    public PropertyPane() {
        widthRatioProperty = new SimpleDoubleProperty();
        heightRatioProperty = new SimpleDoubleProperty();
        selectedProperty = new SimpleBooleanProperty(false);
        properties = Collections.emptyList();
        currPropIdx = 0;
        
        setupVisuals();
        createListeners();
    }
    
    public Node getControl() {
        Node node = null;
        if(!controlBox.getChildren().isEmpty()) {
            node = controlBox.getChildren().get(0);
        }
        return node;
    }
    
    public SimpleBooleanProperty selectedProperty() {
        return selectedProperty;
    }
    
    public SimpleDoubleProperty widthRatioProperty() {
        return widthRatioProperty;
    }
    
    public SimpleDoubleProperty heightRatioProperty() {
        return heightRatioProperty;
    }

    public void setProperties(List<ConfigurableProperty<? extends Object>> properties) {
        this.properties = properties;
        this.currPropIdx = 0;
        updatePropertyList();
    }
    
    public void setCurrPropIdx(int currPropIdx) {
        this.currPropIdx = currPropIdx;
        updatePropertyList();
        createCurrentPropertyControl();
    }
    
    public int getCurrPropIdx() {
        return currPropIdx;
    }

    public int nextProperty() {
        currPropIdx++;
        if(currPropIdx > properties.size() - 1) {
            currPropIdx = properties.size() - 1;
        }
        updatePropertyList();
        createCurrentPropertyControl();
        return currPropIdx;
    }
    
    public int prevProperty() {
        currPropIdx--;
        if(currPropIdx < 0) {
            currPropIdx = 0;
        }
        updatePropertyList();
        createCurrentPropertyControl();
        return currPropIdx;
    }
    
    public ConfigurableProperty<? extends Object> getCurrProperty() {
        ConfigurableProperty<? extends Object> prop = null;
        if(!properties.isEmpty()) {
            prop = properties.get(currPropIdx);
        }
        return prop;
    }
    
    private void createListeners() {    
        parentProperty().addListener((obs, oldVal, newVal) -> {
            updateSize();
        });
    }
    
    private void setupVisuals() {
        setBackground(new Background(new BackgroundFill(
                Color.rgb(120, 120, 120, opacityProperty().get()), new CornerRadii(5), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(Color.DARKGREY, 
                BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(2))));
        setPadding(new Insets(20));
        
        // Icons
        iconUp = UiUtils.createSVGRegion(ARROW_UP_SVG, widthProperty(), ARROW_WIDTH_FACTOR, Color.BLACK);
        iconDown = UiUtils.createSVGRegion(ARROW_DOWN_SVG, widthProperty(), ARROW_WIDTH_FACTOR, Color.BLACK);
        
        // Control pane
        controlBox = new VBox();
        controlBox.setPadding(new Insets(10));
        controlBox.setBackground(HIGHLIGHTED_CONTROL_BG);
        controlBox.borderProperty().bind(Bindings.createObjectBinding(() -> {
            return selectedProperty.get() ? 
                   SELECTED_CONTROL_BORDER : Border.EMPTY;
        }, selectedProperty));
        setCenter(controlBox);
        
        // Property list
        propListBox = new VBox();
        propListBox.prefWidthProperty().bind(widthProperty().divide(2));
        propListBox.setAlignment(Pos.CENTER_LEFT);
        setLeft(propListBox);
    }
    
    private void updateSize() {
        
        // We know it's a stack pane, so it's safe to cast
        StackPane parent = (StackPane) getParent();
        minWidthProperty().bind(parent.widthProperty().divide(widthRatioProperty));
        minHeightProperty().bind(parent.heightProperty().divide(heightRatioProperty));
        maxWidthProperty().bind(minWidthProperty());
        maxHeightProperty().bind(minHeightProperty());
    }
    
    private void updatePropertyList() {
        
        // Calculate which items to show
        int startIdx = Math.min(currPropIdx - PROPS_BEFORE_AND_AFTER, 
                properties.size() - 1 - PROPS_BEFORE_AND_AFTER * 2);
        startIdx = startIdx < 0 ? 0 : startIdx;
        
        int endIdx = Math.max(currPropIdx + PROPS_BEFORE_AND_AFTER, PROPS_BEFORE_AND_AFTER * 2);
        endIdx = endIdx > properties.size() - 1 ? properties.size() - 1 : endIdx;
        
        // Unbind and clear previous items
        for (Node node : propListBox.getChildren()) {
            if(node instanceof Text) {
                ((Label)node).prefWidthProperty().unbind();
                ((Label)node).fontProperty().unbind();
                ((Label)node).backgroundProperty().unbind();
            }
        }
        propListBox.getChildren().clear();
        
        // Create new items
        for (int i = startIdx; i < endIdx + 1; i++) {
            Label label = new Label(properties.get(i).getName());
            label.setTextFill(Color.rgb(90, 90, 90));
            label.prefWidthProperty().bind(propListBox.widthProperty());
            label.fontProperty().bind(Bindings.createObjectBinding(
                    () -> {
                        return Font.font(widthProperty().get() / FONT_WIDTH_FACTOR);
                    }, widthProperty()));

            propListBox.getChildren().add(label);
        }
        
        // Highlight current
        Label curr = (Label)propListBox.getChildren().get(currPropIdx - startIdx);
        curr.setTextFill(Color.BLACK);
        curr.setBackground(HIGHLIGHTED_LABEL_BG);
        curr.borderProperty().bind(Bindings.createObjectBinding(() -> {
            return selectedProperty.get() ? 
                   SELECTED_LABEL_BORDER : Border.EMPTY;
        }, selectedProperty));
        
        // Show arrows if there are more properties above/below
        iconUp.setVisible(startIdx > 0);
        propListBox.getChildren().add(0, iconUp);
        iconDown.setVisible(endIdx < properties.size() - 1);
        propListBox.getChildren().add(iconDown);
    }
    
    @SuppressWarnings("unchecked")
    private void createCurrentPropertyControl() {
        
        // Cleanup previous control and remove it
        if(!controlBox.getChildren().isEmpty()) {
            Region control = (Region) controlBox.getChildren().get(0);
            control.prefWidthProperty().unbind();
            control.prefHeightProperty().unbind();
            controlBox.getChildren().remove(control);
        }
        
        // Create new control
        ConfigurableProperty<? extends Object> prop = 
                properties.get(currPropIdx);
        Region control = null;
        if(prop instanceof ConfigurableColorProperty) {
            
            ObjectProperty<Color> p = (ObjectProperty<Color>) prop.getProp();
            ColorControl picker = new ColorControl(p.getValue());
            p.bind(picker.colorProperty());
            control = picker;
        } else if(prop instanceof IncrementalActionProperty) {
            
            // Reset property every time it's shown
            ((IncrementalActionProperty) prop).getProp().setValue(0);
            control = UiUtils.createIncrementalActionPropertyGauge((IncrementalActionProperty) prop);
            
        } else if(prop instanceof ConfigurableIntegerProperty || 
                prop instanceof ConfigurableDoubleProperty) {
            
            control = UiUtils.createNumberPropertyGauge(prop);
            
        } else if(prop instanceof ConfigurableChoiceProperty) {
            
            ConfigurableChoiceProperty choiceProp = (ConfigurableChoiceProperty) prop;
            ListView<String> list = UiUtils.createChoicePropertyListView(
                    choiceProp, this);
            choiceProp.removeListener("listViewListener");
            choiceProp.addListener((obs, oldValue, newValue) -> {
                list.getSelectionModel().clearSelection();
                list.getSelectionModel().select((String) newValue);
            }, "listViewListener");
            
            control = list;
            
        } else if(prop instanceof ConfigurableBooleanProperty) {
            /*
            ObjectProperty<Boolean> p = (ObjectProperty<Boolean>) prop.getProp();
            ToggleSwitch toggle = UiUtils.createBooleanPropertyToggleSwitch(
                    p.getValue(), null, this);
            toggle.selectedProperty().bind(p);
            */
            control = UiUtils.createBooleanPropertyGauge((ConfigurableBooleanProperty) prop);
            
        } else if(prop instanceof ActionProperty) {
            
            Button button = UiUtils.createActionPropertyButton(prop.getName());
            button.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    ((ActionProperty) prop).trigger();
                }
            });
            control = button;
        }
        
        // Add to pane
        control.prefWidthProperty().bind(controlBox.widthProperty().divide(1.1));
        control.prefHeightProperty().bind(controlBox.heightProperty().divide(1.1));
        controlBox.getChildren().add(control);
    }
}
