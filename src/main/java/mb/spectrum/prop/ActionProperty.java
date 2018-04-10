package mb.spectrum.prop;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class ActionProperty extends ConfigurableProperty<Void> {
	
	private EventHandler<ActionEvent> handler;

	public ActionProperty(String name) {
		super(name, null, null, null, null);
	}

	@Override
	public Void increment() {
		return null;
	}

	@Override
	public Void decrement() {
		return null;
	}
	
	public void setOnAction(EventHandler<ActionEvent> handler) { 
		this.handler = handler;
	}
	
	public void trigger() {
		if(handler != null) {
			handler.handle(new ActionEvent());
		}
	}

}
