package dev.jescas.inertialtester.ui.main;

import org.ejml.data.FMatrix3;
import org.ejml.dense.fixed.NormOps_FDF3;

import dev.jescas.inertialtester.core.algorithms.AccelerationIntegrator;
import dev.jescas.inertialtester.core.algorithms.MadgwickFilter;
import dev.jescas.inertialtester.core.algorithms.StepDetection;
import dev.jescas.inertialtester.core.filters.ButterWorthFilter;
import dev.jescas.inertialtester.core.filters.KalmantFilter;
import dev.jescas.inertialtester.core.math.Quaternion;

public class MainModel {
    public ButterWorthFilter lowPassFilter;
    public ButterWorthFilter highPassFilter;
    private KalmantFilter kalmantFilter;
    private MadgwickFilter madgwickFilter = new MadgwickFilter();
    private AccelerationIntegrator accelerationIntegrator = new AccelerationIntegrator(10, 0.1);
    private StepDetection stepDetection = new StepDetection(0.5);

    public MainModel() {
        kalmantFilter = new KalmantFilter();
        highPassFilter = new ButterWorthFilter(
                new double[]{1., -1.64745998, 0.70089678},
                new double[]{0.83708919, -1.67417838, 0.83708919}
        );

        lowPassFilter = new ButterWorthFilter(
                new double[]{1., -2.64858448, 2.35624385, -0.70305812},
                new double[]{0.00057516, 0.00172547, 0.00172547, 0.00057516}
        );

    }

    public FMatrix3 ProcessOrientation(FMatrix3 acc, FMatrix3 mag, FMatrix3 gyro, double dt) {
        madgwickFilter.Feed(acc, mag, gyro, dt);
        return madgwickFilter.GetEulerAngles();
    }

    public Quaternion ProcessQuaternion(FMatrix3 acc, FMatrix3 mag, FMatrix3 gyro, double dt) {
        madgwickFilter.Feed(acc, mag, gyro, dt);
        return madgwickFilter.GetQuaternion();
    }

    public double ProcessRawAcceleration(FMatrix3 acc_vector) {
        return KalmanFilter2DMethod(acc_vector);
    }

    public int CountStepsAcceleration(double acc, long timestamp) {
        return stepDetection.GetSteps(acc, timestamp);
    }

    // FIXME: Dramatically Error
    public Double[] IntegrateAcceleration(double acc, double delta) {
        accelerationIntegrator.Feed(acc, delta);
        return new Double[]{accelerationIntegrator.GetPosition(), accelerationIntegrator.GetVelocity()};
    }

    public double KalmanFilter2DMethod(FMatrix3 acc_vector) {
        // High Pass Filter - Remove Gravity
        double acc_abs = NormOps_FDF3.normF(acc_vector);
        kalmantFilter.Predict();
        kalmantFilter.Update(acc_abs);
        double acc_dyn = kalmantFilter.GetDynamicAcceleration();
        lowPassFilter.Update(acc_dyn);
        return lowPassFilter.GetOutput();
    }

    public double ButterWorth2DMethod(FMatrix3 acc_vector) {
        // High Pass Filter - Remove Gravity Effects
        double acc_abs = Math.sqrt(acc_vector.a1 * acc_vector.a1 + acc_vector.a2 * acc_vector.a2);
        highPassFilter.Update(acc_abs);
        double acc_dyn = highPassFilter.GetOutput();
        lowPassFilter.Update(acc_dyn);
        return lowPassFilter.GetOutput();
    }
}
