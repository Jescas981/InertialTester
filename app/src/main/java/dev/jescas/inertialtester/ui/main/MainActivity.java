package dev.jescas.inertialtester.ui.main;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.ejml.data.FMatrix3;
import org.ejml.simple.SimpleMatrix;


import dev.jescas.inertialtester.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements IMainView {
    private ActivityMainBinding binding;
    LineChart orientationChart;
    private LineDataSet rollDataset;
    private LineDataSet pitchDataset;
    private LineDataSet yawDataset;
    private LineData orientationData;

    private static final int MAX_SAMPLES = 200; // Limit to 200 samples
    private int sampleCount = 0;
    private boolean onRecord = true;
    private IMainPresenter mainPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Presenter & Model Related
        mainPresenter = new MainPresenter(this);
        binding.btnRecord.setOnClickListener(this::OnRecord);
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometerSensor != null) {
            sensorManager.registerListener(mainPresenter, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscopeSensor != null) {
            sensorManager.registerListener(mainPresenter, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (magnetometerSensor != null) {
            sensorManager.registerListener(mainPresenter, magnetometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        // Chart related
        SetupCharts();
        ConfigureCharts();
    }

    public void OnRecord(View view){
        if(onRecord){
            binding.btnRecord.setText("Stop");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        }else{
            binding.btnRecord.setText("Record");
            Toast.makeText(this, "Recording finished", Toast.LENGTH_SHORT).show();
        }
        mainPresenter.EnableRecord(onRecord);
        onRecord = !onRecord;
    }

    public void OnRecordFinished(String filepath){
        Toast.makeText(this, "Saving file in " + filepath, Toast.LENGTH_SHORT).show();
    }

    public void SetupCharts() {
        orientationChart = binding.chartOrientation;

        // Initialize DataSet for roll, pitch, and yaw
        rollDataset = new LineDataSet(null, "Roll");
        pitchDataset = new LineDataSet(null, "Pitch"); // Initialize pitch dataset
        yawDataset = new LineDataSet(null, "Yaw"); // Initialize yaw dataset

        rollDataset.setDrawCircles(false);
        rollDataset.setColor(ColorTemplate.getHoloBlue());
        rollDataset.setLineWidth(2f);
        rollDataset.setDrawValues(false);

        pitchDataset.setDrawCircles(false);
        pitchDataset.setColor(ColorTemplate.COLORFUL_COLORS[1]); // Use a different color
        pitchDataset.setLineWidth(2f);
        pitchDataset.setDrawValues(false);

        yawDataset.setDrawCircles(false);
        yawDataset.setColor(ColorTemplate.COLORFUL_COLORS[2]); // Use a different color
        yawDataset.setLineWidth(2f);
        yawDataset.setDrawValues(false);

        // Combine datasets into LineData
        orientationData = new LineData(rollDataset, pitchDataset, yawDataset);
        orientationChart.setData(orientationData);
    }
    public void ConfigureCharts(){
        orientationChart.getLegend().setEnabled(true);
        orientationChart.getDescription().setEnabled(false);
        orientationChart.setDrawGridBackground(false);

        XAxis xAxis1 = orientationChart.getXAxis();
        xAxis1.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis1.setLabelCount(5, true);
        xAxis1.setValueFormatter(new IndexAxisValueFormatter());
        xAxis1.setDrawGridLines(false);

        // Customize the Y-Axis (Left)
        orientationChart.getAxisRight().setEnabled(false); // Disable right Y-axis
        orientationChart.getAxisLeft().setDrawGridLines(true);
        orientationChart.getAxisLeft().setLabelCount(9, true); // Set number of ticks/labels on the Y-axis
        orientationChart.getAxisLeft().setAxisMinimum((float) -180); // Set Y-axis minimum to -180
        orientationChart.getAxisLeft().setAxisMaximum((float) 180);  // Set Y-axis maximum to 180

        // Enable the legend and set it for the datasets
        orientationChart.getLegend().setEnabled(true);
        orientationChart.getLegend().setTextColor(ColorTemplate.getHoloBlue());
        orientationChart.getLegend().setTextSize(12f); // Adjust text size for the legend
    }

    public void AddEntriesChart(FMatrix3 orientation) {
        // Add new entry for roll, pitch, and yaw
        rollDataset.addEntry(new Entry(sampleCount, orientation.a1)); // Roll
        pitchDataset.addEntry(new Entry(sampleCount, orientation.a2)); // Pitch
        yawDataset.addEntry(new Entry(sampleCount, orientation.a3)); // Yaw

        // Limit dataset size to 200 samples
        if (rollDataset.getEntryCount() > MAX_SAMPLES) {
            rollDataset.removeFirst();
            for (Entry entry : rollDataset.getValues()) {
                entry.setX(entry.getX() - 1);
            }
            sampleCount = MAX_SAMPLES;
        }

        // Limit dataset size to 200 samples
        if (pitchDataset.getEntryCount() > MAX_SAMPLES) {
            pitchDataset.removeFirst();
            for (Entry entry : pitchDataset.getValues()) {
                entry.setX(entry.getX() - 1);
            }
            sampleCount = MAX_SAMPLES;
        }

        // Limit dataset size to 200 samples
        if (yawDataset.getEntryCount() > MAX_SAMPLES) {
            yawDataset.removeFirst();
            for (Entry entry : yawDataset.getValues()) {
                entry.setX(entry.getX() - 1);
            }
            sampleCount = MAX_SAMPLES;
        }


        // Refresh chart
        orientationData.notifyDataChanged();
        orientationChart.notifyDataSetChanged();
        orientationChart.setVisibleXRangeMaximum(MAX_SAMPLES);
        orientationChart.moveViewToX(sampleCount);
    }

    public void UpdateTextUI(FMatrix3 acc, double filtered){
        // Update UI
        // binding.tvAccx.setText(String.format("Ax: %.4f m/s²", acc.get(0)));
        // binding.tvAccy.setText(String.format("Ay: %.4f m/s²", acc.get(1)));
        // binding.tvAccz.setText(String.format("Az: %.4f m/s²", acc.get(2)));
        // binding.tvAcc.setText(String.format("Am: %.4f m/s^2", acc.normF()));
        //binding.tvAccBias.setText(String.format("Ab: %.4f m/s²", biasacc));
        // binding.tvAccdyn.setText(String.format("Ad: %.4f m/s²", filtered));
        //binding.tvAccdyn.setText(String.format("Steps: %d", stepCounter));
    }
}
