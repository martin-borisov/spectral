package mb.spectrum;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import mb.spectrum.prop.ConfigurableProperty;

public class PropertyPane extends BorderPane {
    
    private static final int PROPS_BEFORE_AND_AFTER = 4;
    private static final String ARROW_UP_SVG = "M11 7l-4 6h8"; // Dashicons "arrow-up";
    private static final String ARROW_DOWN_SVG = "M15 8l-4.03 6L7 8h8z"; // Dashicons "arrow-down"
    private static final int FONT_WIDTH_FACTOR = 30;
    private static final int ARROW_WIDTH_FACTOR = 50;
    
    public PropertyPane(Pane parent, double widthRatio, double heightRatio, double opacity, 
            Region control, List<ConfigurableProperty<? extends Object>> currentPropertyList, 
            int currentPropIdx) {
        setup(parent, widthRatio, heightRatio, opacity);
        createPropertyList(currentPropertyList, currentPropIdx);
        addPropertyControl(control);
    }
    
    private void setup(Pane parent, double widthRatio, double heightRatio, double opacity) {
        prefWidthProperty().bind(parent.widthProperty().divide(widthRatio));
        prefHeightProperty().bind(parent.heightProperty().divide(heightRatio));
        layoutXProperty().bind(parent.widthProperty().subtract(widthProperty()).divide(2));
        layoutYProperty().bind(parent.heightProperty().subtract(heightProperty()).divide(2));
        setBackground(new Background(new BackgroundFill(
                Color.rgb(140, 140, 140, opacity), new CornerRadii(5), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(Color.DARKGREY, 
                BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(2))));
    }
    
    private void createPropertyList(
            List<ConfigurableProperty<? extends Object>> currentPropertyList, int currentPropIdx) {
        
        // Show a few properties before and after the current one
        VBox box = new VBox();
        box.setStyle("-fx-padding: 10;");
        box.setBackground(new Background(new BackgroundFill(
                Color.rgb(130, 130, 130), new CornerRadii(5), Insets.EMPTY)));
        
        // Make the vbox wider than the potentially longest string in the list to avoid resizing the pane
        box.prefWidthProperty().bind(widthProperty().divide(2.5));
        
        int startIdx = Math.min(currentPropIdx - PROPS_BEFORE_AND_AFTER, 
                currentPropertyList.size() - 1 - PROPS_BEFORE_AND_AFTER * 2);
        startIdx = startIdx < 0 ? 0 : startIdx;
        
        int endIdx = Math.max(currentPropIdx + PROPS_BEFORE_AND_AFTER, PROPS_BEFORE_AND_AFTER * 2);
        endIdx = endIdx > currentPropertyList.size() - 1 ? currentPropertyList.size() - 1 : endIdx;
        
        for (int i = startIdx; i < endIdx + 1; i++) {
            Text text = new Text(currentPropertyList.get(i).getName());
            
            if(i == currentPropIdx) {
                text.setFill(Color.BLACK);
                text.setUnderline(true);
            } else {
                text.setFill(Color.DIMGRAY);
            }
            
            text.fontProperty().bind(Bindings.createObjectBinding(
                    () -> {
                        return Font.font(widthProperty().get() / FONT_WIDTH_FACTOR);
                    }, widthProperty()));

            box.getChildren().add(text);
        }
        
        // Show arrows if there are more properties above/below
        Region iconUp = UiUtils.createSVGRegion(ARROW_UP_SVG, widthProperty(), ARROW_WIDTH_FACTOR, Color.BLACK);
        iconUp.setVisible(startIdx > 0);
        box.getChildren().add(0, iconUp);
        
        Region iconDown = UiUtils.createSVGRegion(ARROW_DOWN_SVG, widthProperty(), ARROW_WIDTH_FACTOR, Color.BLACK);
        iconDown.setVisible(endIdx < currentPropertyList.size() - 1);
        box.getChildren().add(iconDown);
        
        // Align and add property list to pane
        box.setAlignment(Pos.CENTER_LEFT);
        setLeft(box);
        
    }
    
    private void addPropertyControl(Region control) {
        setCenter(control);
        BorderPane.setAlignment(control, Pos.CENTER);
        
        // TODO Revisit: Automatically resize the contained property control based on the pane size
        control.prefWidthProperty().bind(widthProperty().divide(2));
        control.prefHeightProperty().bind(heightProperty().divide(4));
    }

}
