package dev.jescas.inertialtester.core.filters;

import org.ejml.simple.SimpleMatrix;

public class ButterWorthFilter implements IPassFilter {
    private final double[] a;
    private final double[] b;
    private final double[] inputHistory;
    private final double[] outputHistory;
    private final int order;

    public ButterWorthFilter(double[] a, double[] b){
        this.a = a;
        this.b = b;
        this.order = Math.max(a.length, b.length) - 1; // Ensure order is set correctly
        inputHistory = new double[order+1];
        outputHistory = new double[order+1];
    }

    @Override
    public void Update(double raw) {
        // Shift the history buffers (move the older samples back)
        System.arraycopy(inputHistory, 0, inputHistory, 1, order); // Shift input history
        System.arraycopy(outputHistory, 0, outputHistory, 1, order); // Shift output history

        // Set the latest input sample in the history
        inputHistory[0] = raw;

        // Compute the new filtered output
        double output = 0.0;

        // Apply the b coefficients (feedforward part)
        for (int i = 0; i < b.length; i++) {
            output += b[i] * inputHistory[i];
        }

        // Apply the a coefficients (feedback part), excluding a[0] which is always 1
        for (int j = 1; j < a.length; j++) {
            output -= a[j] * outputHistory[j];
        }

        // Normalize by a[0] (if it's not 1, though for Butterworth it typically is)
        output /= a[0];

        // Store the new output in the history
        outputHistory[0] = output;
    }
    @Override
    public double GetOutput(){
        return outputHistory[0];
    }
}
