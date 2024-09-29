package dev.jescas.inertialtester;
import java.util.LinkedList;

import java.util.LinkedList;

public class StepDetector {
    private LinkedList<Double> peakVector; // to store peak values
    private boolean onThreshold; // flag to check if threshold is crossed
    private double threshold; // the threshold for peak detection
    private Double peak; // the detected peak
    private Double start; // start value of the peak
    private Double end; // end value of the peak
    private int peakIdxOffset; // offset for peak index
    private LinkedList<Double> startVector; // for tracking start of peak
    private LinkedList<Double> endVector; // for tracking end of peak
    private boolean onStart; // flag for start detection
    private boolean onEnd; // flag for end detection
    private int peakIdx; // index of the detected peak
    private int startIdx; // index of the start

    // Constructor
    public StepDetector(double threshold) {
        this.peakVector = new LinkedList<>();
        this.startVector = new LinkedList<>();
        this.endVector = new LinkedList<>();
        this.threshold = threshold;
        this.peak = null;
        this.start = null;
        this.end = null;
        this.onStart = false;
        this.onEnd = false;
        this.peakIdx = 0;
        this.startIdx = 0;
    }

    // Method to get peak information
    public Object[] getPeakInfo() {
        return new Object[]{peak, start, end};
    }

    // Method to get peak index
    public int[] getPeakIdx() {
        return new int[]{peakIdx, startIdx + 1};
    }

    // Method to detect peak
    public boolean detectPeak(double raw) {
        // Reset
        if (!onEnd) {
            peak = null;
            end = null;
        }

        // Start Vector - Get 3 Values
        if (startVector.size() > 2) {
            startVector.removeFirst();
        }
        startVector.add(raw);

        // End Vector - Get 2 Values
        if (endVector.size() > 1) {
            endVector.removeFirst();
        }
        endVector.add(raw);

        startIdx++;

        // Mark Start
        if (!onStart && startVector.size() > 2 && startVector.get(2) > startVector.get(1) && startVector.get(1) > startVector.get(0)) {
            start = raw;
            onStart = true;
            startIdx = 0;
        } else if (onStart && startVector.size() > 2 && startVector.get(2) < startVector.get(1) && startVector.get(1) < startVector.get(0)) {
            onStart = false;
        }

        // Mark End
        if (onEnd && endVector.size() > 1 && endVector.get(1) > endVector.get(0)) {
            end = raw;
            peakIdx = startIdx - peakIdx;
            onEnd = false;
            return true;
        }

        // Detect peaks above the threshold
        if (!onThreshold && raw >= threshold) {
            onThreshold = true;
            peakIdxOffset = startIdx;
        } else if (onThreshold) {
            if (raw < threshold) {
                onThreshold = false;
                if (!peakVector.isEmpty()) {
                    int argmax = getIndexOfMax(peakVector); // Find the max value in the buffer
                    peak = peakVector.get(argmax);
                    peakIdx = peakIdxOffset + argmax; // Adjust the peak index correctly
                    peakVector.clear();
                    onEnd = true;
                }
            }
            if (raw >= threshold) {
                peakVector.add(raw);
            }
        }
        return false;
    }

    // Helper method to find the index of the maximum value in a LinkedList
    private int getIndexOfMax(LinkedList<Double> list) {
        int maxIndex = 0;
        double maxValue = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i) > maxValue) {
                maxValue = list.get(i);
                maxIndex = i;
            }
        }
        return maxIndex;
    }
}
