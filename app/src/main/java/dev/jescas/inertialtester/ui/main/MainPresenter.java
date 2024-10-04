package dev.jescas.inertialtester.ui.main;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.ejml.data.FMatrix3;
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
    private WriteFileStream accFileStream, magFileStream, gyroFileStream;
    private FMatrix3 accVector = new FMatrix3();
    private FMatrix3 magVector = new FMatrix3();


    public MainPresenter(IMainView view){
        this.view = view;
        this.model = new MainModel();
    }

    public void EnableRecord(boolean enable){
        if(enable) {
            accFileStream =  new WriteFileStream(view.getBaseContext().getExternalFilesDir(null), "accel_data");
            magFileStream =  new WriteFileStream(view.getBaseContext().getExternalFilesDir(null), "mag_data");
            gyroFileStream =  new WriteFileStream(view.getBaseContext().getExternalFilesDir(null), "gyro_data");
        }else{
            view.OnRecordFinished(accFileStream.GetFilePath());
            view.OnRecordFinished(gyroFileStream.GetFilePath());
            view.OnRecordFinished(magFileStream.GetFilePath());
            accFileStream.CloseStream();
            gyroFileStream.CloseStream();
            magFileStream.CloseStream();
        }
        onRecording = enable;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Check Sensor Type
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            // Turn this into a Vector3f
            float[] values = sensorEvent.values;
            // Create Acceleration Vector
            accVector = new FMatrix3(values[0],values[1],values[2]);
            // Process Data & Show in UI
            FMatrix3 orientation = model.ProcessOrientation(accVector,magVector);
            view.AddEntriesChart(orientation);
            // Save on record only
            if(onRecording){
                accFileStream.AppendData(values);
            }
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            // Turn this into a Vector3f
            float[] values = sensorEvent.values;
            // Save on record only
            if(onRecording){
                gyroFileStream.AppendData(values);
            }
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            // Turn this into a Vector3f
            float[] values = sensorEvent.values;
            magVector = new FMatrix3(values[0],values[1],values[2]);
            // Save on record only
            if(onRecording){
                magFileStream.AppendData(values);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
