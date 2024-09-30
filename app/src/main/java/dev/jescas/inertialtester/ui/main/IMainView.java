package dev.jescas.inertialtester.ui.main;

import android.content.Context;

import org.ejml.simple.SimpleMatrix;

public interface IMainView {
     void OnRecordChunk();
     void OnRecordFinished(String filepath);
    Context getBaseContext();
    void AddEntriesChart(double raw, double filtered);
    void UpdateTextUI(SimpleMatrix acc, double filtered);
}
