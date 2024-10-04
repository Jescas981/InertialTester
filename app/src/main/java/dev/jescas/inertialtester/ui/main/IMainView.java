package dev.jescas.inertialtester.ui.main;

import android.content.Context;

import org.ejml.data.FMatrix3;
import org.ejml.simple.SimpleMatrix;

public interface IMainView {
     void OnRecordFinished(String filepath);
    Context getBaseContext();
    void AddEntriesChart(FMatrix3 orientation);
    void UpdateTextUI(FMatrix3 acc, double filtered);
}
