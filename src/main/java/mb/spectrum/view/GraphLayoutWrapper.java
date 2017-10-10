package mb.spectrum.view;

import javafx.scene.layout.Pane;

public class GraphLayoutWrapper {
	
	private Pane pane;
	private int margin;

	public GraphLayoutWrapper(int margin) {
		this.margin = margin;
		createPane();
	}
	
	public double coordX(double x) {
		return margin + x;
	}
	
	public double coordY(double y) {
		return pane.getHeight() - margin - y;
	}

	public Pane getPane() {
		return pane;
	}
	
	public double getLayoutWidth() {
		return pane.getWidth() - margin * 2;
	}
	
	public double getLayoutHeight() {
		return pane.getHeight() - margin * 2;
	}
	
	private void createPane() {
		pane = new Pane();
	}
}
