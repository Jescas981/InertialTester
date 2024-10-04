package dev.jescas.inertialtester.ui.main;

import org.ejml.data.FMatrix3;
import org.ejml.simple.SimpleMatrix;

import dev.jescas.inertialtester.core.algorithms.FactoredQuaternionAlgorithm;
import dev.jescas.inertialtester.core.filters.ButterWorthFilter;
import dev.jescas.inertialtester.core.filters.IPassFilter;
import dev.jescas.inertialtester.core.filters.KalmantFilter;

public class MainModel {
    public ButterWorthFilter lowPassFilter;
    public ButterWorthFilter highPassFilter;
    private KalmantFilter kalmantFilter;
    private FactoredQuaternionAlgorithm quaternionAlgorithm = new FactoredQuaternionAlgorithm();

    public MainModel(){
        kalmantFilter = new KalmantFilter();
        highPassFilter = new ButterWorthFilter(
                new double[]{1.0, -0.83791065},
                new double[]{ 0.91895532, -0.91895532}
        );

        lowPassFilter = new ButterWorthFilter(
                new double[]{1.        ,-2.64858448,  2.35624385, -0.70305812},
                new double[]{0.00057516,0.00172547, 0.00172547, 0.00057516}
        );
    }

    public FMatrix3 ProcessOrientation(FMatrix3 acc, FMatrix3 mag){
        quaternionAlgorithm.Feed(acc,mag);
        return quaternionAlgorithm.GetEulerAngles();
    }

    public double ProcessRawAcceleration(FMatrix3 acc_vector){
        return ButterWorth2DMethod(acc_vector);
    }

    public double KalmantFilter2DMethod(SimpleMatrix acc_vector){
        // High Pass Filter - Remove Gravity
        double acc_abs = acc_vector.normF();
        kalmantFilter.Predict();
        kalmantFilter.Update(acc_abs);
        double acc_dyn = kalmantFilter.GetDynamicAcceleration(); //highPassFilter.GetOutput();
        lowPassFilter.Update(acc_dyn);
        double acc_dyn_smooth = lowPassFilter.GetOutput();
        return acc_dyn_smooth;
    }

    public double ButterWorth2DMethod(FMatrix3 acc_vector){
        // High Pass Filter - Remove Gravity Effects
        double acc_abs = Math.sqrt(Math.pow(acc_vector.get(0,1), 2) + Math.pow(acc_vector.get(0,1), 2));
        highPassFilter.Update(acc_abs);
        double acc_dyn = highPassFilter.GetOutput();
        lowPassFilter.Update(acc_dyn);
        double acc_dyn_smooth = lowPassFilter.GetOutput();
        return acc_dyn_smooth;
    }
}
