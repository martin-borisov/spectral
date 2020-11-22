package mb.spectrum;

import java.util.ArrayList;

import mb.spectrum.view.AnalogMeterView;
import mb.spectrum.view.AnalogMeterView.Orientation;
import mb.spectrum.view.GaugeView;
import mb.spectrum.view.SoundWaveView;
import mb.spectrum.view.SpectrumAreaView;
import mb.spectrum.view.SpectrumBarView;
import mb.spectrum.view.StereoGaugeView;
import mb.spectrum.view.StereoLevelsLedView;
import mb.spectrum.view.StereoLevelsLedView3D;
import mb.spectrum.view.StereoLevelsView;
import mb.spectrum.view.View;

/**
 * Lazily loads views on demand
 */
public class ViewLazyList extends ArrayList<View> {
    private static final long serialVersionUID = 1L;
    
    private int bufferSize;
    
    public ViewLazyList(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public View get(int index) {
        View view = null;
        
        switch (index) {
        case 0:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new AnalogMeterView("Analog Meter", "analogMeterView", "Peak", Orientation.HORIZONTAL));
            }
            break;
            
        case 1:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new StereoLevelsLedView());
            }
            break;
            
        case 2:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new SpectrumBarView());
            }
            break;
            
        case 3:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new SoundWaveView(bufferSize));
            }
            break;
            
        case 4:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new StereoLevelsLedView3D());
            }
            break;
            
        case 5:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new SpectrumAreaView());
            }
            break;
            
        case 6:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new StereoLevelsView());
            }
            break;
            
        case 7:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new StereoGaugeView());
            }
            break;
            
        case 8:
            try {
                view = super.get(index);
            } catch (IndexOutOfBoundsException e) {
                add(index, view = new GaugeView("Analog Meter", "gaugeView", false));
            }
            break;

        default:
            view = super.get(index);
            break;
        }
        
        return view;
    }

    @Override
    public int size() {
        return Math.max(9, super.size());
    }
}
