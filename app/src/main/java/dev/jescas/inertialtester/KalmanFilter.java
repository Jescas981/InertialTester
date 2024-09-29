package dev.jescas.inertialtester;

import org.ejml.simple.ConstMatrix;
import org.ejml.simple.SimpleMatrix;

public class KalmanFilter {
    private SimpleMatrix F;  // State transition matrix
    private SimpleMatrix Q;  // Process noise covariance matrix
    private SimpleMatrix H;  // Observation matrix
    private SimpleMatrix R;  // Sensor noise covariance matrix
    private SimpleMatrix X;  // State vector (dynamic acceleration + bias)
    private SimpleMatrix P;  // Covariance matrix

    // Constructor
    public KalmanFilter() {
        // Initialize the state vector X = [dynamic acceleration, bias]
        X = new SimpleMatrix(2, 1);
        X.set(0, 0, 0);   // Initial dynamic acceleration (starting at 0)
        X.set(1, 0, 9.81); // Initial bias (gravity)

        // Covariance matrix P (2x2 identity matrix for initial uncertainty)
        P = SimpleMatrix.identity(2);

        // State transition matrix F (identity matrix in this case, for simplicity)
        F = SimpleMatrix.identity(2);

        // Process noise covariance Q (adjust based on system dynamics)
        Q = new SimpleMatrix(2, 2);
        Q.set(0, 0, 1e-4);  // Process noise in acceleration
        Q.set(1, 1, 1e-6);  // Process noise in bias

        // Observation matrix H (relates state to measurements)
        H = new SimpleMatrix(1, 2);
        H.set(0, 0, 1); // Mapping dynamic acceleration
        H.set(0, 1, 1); // Mapping bias

        // Sensor noise covariance matrix R (measurement noise)
        R = new SimpleMatrix(1, 1);
        R.set(0, 0, 1e-2);  // Sensor noise for accelerometer
    }

    // Prediction step
    public void predict() {
        // State prediction X = F * X
        X = F.mult(X);

        // Covariance prediction P = F * P * F' + Q
        P = F.mult(P).mult(F.transpose()).plus(Q);
    }

    // Update step (measurement update)
    public void update(double measurement) {
        // Measurement residual y = z - H * X
        SimpleMatrix z = new SimpleMatrix(1, 1);  // Measurement (acceleration)
        z.set(0, 0, measurement);
        SimpleMatrix y = z.minus(H.mult(X));

        // Innovation covariance S = H * P * H' + R
        SimpleMatrix S = H.mult(P).mult(H.transpose()).plus(R);

        // Kalman gain K = P * H' * inv(S)
        SimpleMatrix K = P.mult(H.transpose()).scale(1.0 / S.get(0, 0));

        // State update X = X + K * y
        X = X.plus(K.mult(y));

        // Covariance update P = (I - K * H) * P
        SimpleMatrix I = SimpleMatrix.identity(2);
        P = I.minus(K.mult(H)).mult(P);
    }

    // Get dynamic acceleration (filtered result)
    public double getDynamicAcceleration() {
        return X.get(0, 0); // Return the first state variable (dynamic acceleration)
    }

    // Get bias (gravity component)
    public double getBias() {
        return X.get(1, 0); // Return the second state variable (bias)
    }
}
