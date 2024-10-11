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

import dev.jescas.inertialtester.core.algorithms.StepDetection;
import dev.jescas.inertialtester.core.math.Quaternion;
import dev.jescas.inertialtester.core.persistance.WriteFileStream;


public class MainPresenter implements IMainPresenter{
    private final IMainView view;
    private final MainModel model;
    private boolean onRecording = false;
    private WriteFileStream accFileStream, magFileStream, gyroFileStream;

    // Timestamps for synchronization
    private long currTimestamp = 0;
    private long prevTimestamp = 0;

    // Store the last measurements
    private float[] lastAccValues = new float[3];
    private float[] lastGyroValues = new float[3];
    private float[] lastMagValues = new float[3];

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
        currTimestamp = sensorEvent.timestamp;

        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // Update last values and timestamp
                lastAccValues = sensorEvent.values.clone();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // Update last values and timestamp
                lastGyroValues = sensorEvent.values.clone();
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                // Update last values and timestamp
                lastMagValues = sensorEvent.values.clone();
                Process();
                prevTimestamp = currTimestamp;
                break;

        }
    }

    private void Process(){
        // Calculate delta time in seconds
        double deltaTime = (currTimestamp - prevTimestamp) * 1e-9; // Convert nanoseconds to seconds
        if(deltaTime > 1.0){
            return;
        }
        // Create FMatrix3 objects for processing
        FMatrix3 accVector = new FMatrix3(lastAccValues[0],lastAccValues[1],lastAccValues[2]);
        FMatrix3 magVector = new FMatrix3(lastMagValues[0],lastMagValues[1],lastMagValues[2]);
        FMatrix3 gyroVector = new FMatrix3(lastGyroValues[0],lastGyroValues[1],lastGyroValues[2]);

        // Process Data & Show in UI
        Quaternion orientation = model.ProcessQuaternion(accVector, magVector, gyroVector, deltaTime);
        FMatrix3 angles = orientation.getEulerAngles();
        FMatrix3 accRot = orientation.rotateVector(accVector);
        double acc_ft = model.ProcessRawAcceleration(accRot);
        int steps = model.CountStepsAcceleration(acc_ft, currTimestamp);
        double heading = angles.a3;

        Double[] integral = model.IntegrateAcceleration(acc_ft, deltaTime);

        view.AddEntriesChart(angles, acc_ft);
        view.UpdateTextUI(acc_ft, steps, heading, integral[0], integral[1]);

        // Save on record only
        if (onRecording) {
            accFileStream.AppendData(lastAccValues);
            gyroFileStream.AppendData(lastGyroValues);
            magFileStream.AppendData(lastMagValues);
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
