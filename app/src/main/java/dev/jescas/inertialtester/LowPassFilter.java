package dev.jescas.inertialtester;

public class LowPassFilter {
    private double[] b = { 0.00139748, 0.0055899,  0.00838485, 0.0055899,  0.00139748};  // Numerator coefficients (filter feedforward)
    private double[] a = { 1.,        -2.85486627,  3.17616036, -1.61230573,  0.31337125};  // Denominator coefficients (filter feedback)

    private double[] inputHistory ;  // Buffer to store previous input samples
    private double[] outputHistory ; // Buffer to store previous output samples
    private int order;  // Filter order (based on b and a lengths)

    // Constructor to initialize the filter
    public LowPassFilter() {
        this.order = Math.max(b.length, a.length) - 1;

        // Initialize buffers with zeros
        inputHistory = new double[order + 1];
        outputHistory = new double[order + 1];
    }

    // Method to apply the filter on a single sample
    public double applyFilter(double newSample) {
        // Shift the history buffers (move the older samples back)
        System.arraycopy(inputHistory, 0, inputHistory, 1, order);
        System.arraycopy(outputHistory, 0, outputHistory, 1, order);

        // Set the latest input sample in the history
        inputHistory[0] = newSample;

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

        // Return the filtered output for this sample
        return output;
    }
}
