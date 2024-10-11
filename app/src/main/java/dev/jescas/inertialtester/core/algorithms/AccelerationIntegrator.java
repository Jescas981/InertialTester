package dev.jescas.inertialtester.core.algorithms;

import java.util.LinkedList;
import java.util.Queue;

public class AccelerationIntegrator {
    private final int windowSize;
    private final double std_threshold;
    private final Queue<Double> accelerationWindow;
    private double velocity;
    private double position;

    public AccelerationIntegrator(int windowSize, double std_threshold){
        this.windowSize = windowSize;
        this.std_threshold = std_threshold;
        this.accelerationWindow = new LinkedList<>();
        this.velocity = 0.0;
        this.position = 0.0;
    }

    // Add acceleration sample and perform integration
    public void Feed(double acceleration, double deltaTime) {
        accelerationWindow.add(acceleration);

        // Ensure the window size doesn't exceed the specified windowSize
        if (accelerationWindow.size() > windowSize) {
            accelerationWindow.poll();
        }
        if (!DetectStop()) {
            // Simple integration: v = v0 + a * dt
            velocity += acceleration * deltaTime;
            // x = x0 + v * dt
            position += velocity * deltaTime;
        }

    }

    // Detect if the standard deviation of acceleration is below the threshold, indicating a stop
    private boolean DetectStop() {
        if (accelerationWindow.size() < windowSize) {
            return false; // Not enough samples to make a decision
        }

        double mean = 0.0;
        for (double acc : accelerationWindow) {
            mean += acc;
        }
        mean /= accelerationWindow.size();

        double variance = 0.0;
        for (double acc : accelerationWindow) {
            variance += Math.pow(acc - mean, 2);
        }
        variance /= accelerationWindow.size();

        return Math.sqrt(variance) < std_threshold;
    }

    // Return the current velocity
    public double GetVelocity() {
        return velocity;
    }

    // Return the current position
    public double GetPosition() {
        return position;
    }
}
