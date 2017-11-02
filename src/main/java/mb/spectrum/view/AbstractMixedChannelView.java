package mb.spectrum.view;

public abstract class AbstractMixedChannelView extends AbstractView {

	@Override
	public final void dataAvailable(float[] left, float[] right) {
		float[] samples = new float[left.length];
		for (int i = 0; i < left.length; i++) {
			samples[i] = ((left[i] + right[i]) / 2.0F);
		}
		dataAvailable(samples);
	}
	
	public abstract void dataAvailable(float[] data);
	
	@Override
	public void onShow() {
	}

	@Override
	public void onHide() {
	}
	
}
