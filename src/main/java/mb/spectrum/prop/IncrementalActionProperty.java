package mb.spectrum.prop;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class IncrementalActionProperty extends ConfigurableIntegerProperty {
    
    private EventHandler<ActionEvent> handler;

    public IncrementalActionProperty(String name, Integer maxValue) {
        super(name, 0, maxValue, 0, 1);
        setupTrigger();
    }
    
    public void setOnAction(EventHandler<ActionEvent> handler) { 
        this.handler = handler;
    }
    
    private void setupTrigger() {
        prop.addListener((obs, oldVal, newVal) -> {
            if(newVal.intValue() == maxValue) {
                trigger();
            }
        });
    }
    
    private void trigger() {
        if(handler != null) {
            handler.handle(new ActionEvent());
        }
    }

}
