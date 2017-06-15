package mb.spectrum.view;

import javafx.scene.Scene;

public class GraphSceneWrapper {
	
	private Scene scene;
	private int margin;

	public GraphSceneWrapper(Scene scene, int margin) {
		this.scene = scene;
		this.margin = margin;
	}
	
	public double coordX(double x) {
		return margin + x;
	}
	
	public double coordY(double y) {
		return scene.getHeight() - margin - y;
	}

	public Scene getScene() {
		return scene;
	}
	
	public double getSceneWidth() {
		return scene.getWidth() - margin * 2;
	}
	
	public double getSceneHeight() {
		return scene.getHeight() - margin * 2;
	}
}
