package mb.spectrum.gpio;

import java.text.MessageFormat;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import mb.spectrum.gpio.RotaryEncoderHandler.Direction;
import mb.spectrum.gpio.RotaryEncoderHandler.RotationListener;

public class RotaryEncoderDebugPane extends Application {
    
    private RotaryEncoderHandler handler;
    private ObservableList<String> events;
    private ListView<String> listView;
    private Circle circlePinA, circlePinB;
    private long lastEventTs;

    @Override
    public void init() throws Exception {
        events = FXCollections.observableArrayList();
    }

    @Override
    public void start(Stage stage) throws Exception {
        
        // GPIO
        handler = new RotaryEncoderHandler(RaspiPin.GPIO_25, RaspiPin.GPIO_27, new RotationListener() {
            public void rotated(Direction direction) {
            }
        });
        handler.setPinListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                handlePinEvent(event);
            }
        });
        
        // UI
        listView = new ListView<String>(events);
        listView.setPrefSize(200, 250);
        listView.setEditable(true);
        
        circlePinA = new Circle(30);
        circlePinA.setFill(Color.GRAY);
        circlePinB = new Circle(30);
        circlePinB.setFill(Color.GRAY);
        HBox circleBox = new HBox(new Label("A ->"), circlePinA, new Label("B ->"), circlePinB);
        circleBox.setSpacing(10);
        
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(listView);
        BorderPane.setAlignment(circleBox, Pos.CENTER_RIGHT);
        borderPane.setBottom(circleBox);
        
        stage.setScene(new Scene(borderPane));
        stage.show();
        stage.setMaximized(true);
    }

    @Override
    public void stop() throws Exception {
    }
    
    private void handlePinEvent(GpioPinDigitalStateChangeEvent event) {
        Platform.runLater(new Runnable() {
            public void run() {
                
                // Update circles
                Circle circle = null;
                switch (event.getPin().getName()) {
                case RotaryEncoderHandler.PIN_A:
                    circle = circlePinA;
                    break;
                    
                case RotaryEncoderHandler.PIN_B:
                    circle = circlePinB;
                    break;
                }
                circle.setFill(PinState.HIGH.equals(event.getState()) ? Color.RED : Color.GRAY);
                
                // Update event list
                int idx = events.size();
                
                long ts = 0;
                if(!events.isEmpty()) {
                    long currentTs = System.currentTimeMillis();
                    ts = currentTs - lastEventTs;
                    lastEventTs = currentTs;
                }
                
                events.add(MessageFormat.format("{0} / {1} / {2} / {3}", event.getPin().getName(), event.getState(), event.getEdge(), ts));
                listView.scrollTo(idx);
            }
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
