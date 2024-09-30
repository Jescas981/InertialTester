package dev.jescas.inertialtester.ui.main;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.ejml.simple.SimpleMatrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

import dev.jescas.inertialtester.core.persistance.WriteFileStream;


public class MainPresenter implements IMainPresenter{
    private final IMainView view;
    private final MainModel model;
    private boolean onRecording = false;
    private final int MAX_BUFFER_SIZE = 100 * 1024 * 1024;  // 100 MB
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    private String filename;
    private WriteFileStream accFileStream;

    public MainPresenter(IMainView view){
        this.view = view;
        this.model = new MainModel();
    }

    public void EnableRecord(boolean enable){
        onRecording = enable;
        if(enable) {
            accFileStream =  new WriteFileStream(view.getBaseContext().getExternalFilesDir(null), "accel_data");
        }else{
            view.OnRecordFinished(accFileStream.GetFilePath());
            accFileStream.CloseStream();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Check Sensor Type
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            // Turn this into a Vector3f
            float[] values = sensorEvent.values;
            // Create Acceleration Vector
            SimpleMatrix acc_vector = new SimpleMatrix(3,1,true,
                    new double[]{values[0],values[1],values[2]});
            // Process Data & Show in UI
            double acc_abs = Math.sqrt(Math.pow(acc_vector.get(0), 2) + Math.pow(acc_vector.get(1), 2));
            double acc_ft = model.ProcessRawAcceleration(acc_vector);
            view.AddEntriesChart(acc_abs, acc_ft);
            view.UpdateTextUI(acc_vector, acc_ft);
            // Save on record only
            if(onRecording){
                accFileStream.AppendData(values);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
