package dev.jescas.inertialtester.ui.main;

import android.content.Context;

import org.ejml.data.FMatrix3;
import org.ejml.simple.SimpleMatrix;

public interface IMainView {
     void OnRecordFinished(String filepath);
    Context getBaseContext();
    void AddEntriesChart(FMatrix3 orientation, double raw);
    void UpdateTextUI(double acc, int steps, double heading, double position, double velocity);
}
