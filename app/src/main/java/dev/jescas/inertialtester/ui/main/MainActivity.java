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

import org.ejml.simple.SimpleMatrix;


import dev.jescas.inertialtester.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements IMainView {
    private ActivityMainBinding binding;
    private LineDataSet accMagnitudeDataSet, accKFDataset;
    private LineData accLineDataRaw, accLineDataKF;
    LineChart lineChartRaw, lineChartKF;
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
        if (accelerometerSensor != null) {
            sensorManager.registerListener(mainPresenter, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
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

    public void OnRecordChunk(){
        Toast.makeText(this, "Saving chunk for memory safety", Toast.LENGTH_SHORT).show();
    }

    public void OnRecordFinished(String filepath){
        Toast.makeText(this, "Saving file in " + filepath, Toast.LENGTH_SHORT).show();
    }


    public void SetupCharts() {
        lineChartRaw = binding.chartraw;
        lineChartKF = binding.chartkf;
        // Initialize DataSet for accelerometer magnitude
        accMagnitudeDataSet = new LineDataSet(null, "Accel Magnitude (m/s²)");
        accMagnitudeDataSet.setDrawCircles(false);
        accMagnitudeDataSet.setColor(ColorTemplate.getHoloBlue());
        accMagnitudeDataSet.setLineWidth(2f);
        accMagnitudeDataSet.setDrawValues(false);


        // Initialize LineData and add the DataSet to it
        accLineDataRaw = new LineData(accMagnitudeDataSet);
        lineChartRaw.setData(accLineDataRaw);

        // Initialize DataSet for accelerometer magnitude
        accKFDataset = new LineDataSet(null, "Accel KF (m/s²)");
        accKFDataset.setDrawCircles(false);
        accKFDataset.setColor(ColorTemplate.getHoloBlue());
        accKFDataset.setLineWidth(2f);
        accKFDataset.setDrawValues(false);


        // Initialize LineData and add the DataSet to it
        accLineDataKF = new LineData(accKFDataset);
        lineChartKF.setData(accLineDataKF);
    }

    public void ConfigureCharts(){
        lineChartRaw.getDescription().setEnabled(false);
        lineChartRaw.setDrawGridBackground(false);

        XAxis xAxis = lineChartRaw.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(5, true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter());
        xAxis.setDrawGridLines(false);

        // Customize the Y-Axis (Left)
        // lineChartRaw.getAxisRight().setEnabled(false); // Disable right Y axis
        // lineChartRaw.getAxisLeft().setDrawGridLines(true);
        // lineChartRaw.getAxisLeft().setAxisMinimum(8f); // Set the minimum value of the Y axis
        // lineChartRaw.getAxisLeft().setAxisMaximum(12f); // Set the maximum value of the Y axis
        // lineChartRaw.getAxisLeft().setLabelCount(10, true); // Set number of ticks/labels on the Y axis

        lineChartRaw.getLegend().setEnabled(false);

        lineChartRaw.getDescription().setEnabled(false);
        lineChartRaw.setDrawGridBackground(false);

        XAxis xAxis1 = lineChartKF.getXAxis();
        xAxis1.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis1.setLabelCount(5, true);
        xAxis1.setValueFormatter(new IndexAxisValueFormatter());
        xAxis1.setDrawGridLines(false);

        // Customize the Y-Axis (Left)
        lineChartKF.getAxisRight().setEnabled(false); // Disable right Y axis
        lineChartKF.getAxisLeft().setDrawGridLines(true);
        lineChartKF.getAxisLeft().setLabelCount(10, true); // Set number of ticks/labels on the Y axis

        lineChartKF.getLegend().setEnabled(false);
    }

    public void AddEntriesChart(double raw, double filtered) {
        // Add new entry
        accMagnitudeDataSet.addEntry(new Entry(sampleCount++, (float)raw));

        // Limit dataset size to 200 samples
        if (accMagnitudeDataSet.getEntryCount() > MAX_SAMPLES) {
            accMagnitudeDataSet.removeFirst();
            for (Entry entry : accMagnitudeDataSet.getValues()) {
                entry.setX(entry.getX() - 1);
            }
            sampleCount = MAX_SAMPLES;
        }

        // Refresh chart
        accLineDataRaw.notifyDataChanged();
        lineChartRaw.notifyDataSetChanged();
        lineChartRaw.setVisibleXRangeMaximum(MAX_SAMPLES);
        lineChartRaw.moveViewToX(sampleCount);

        // Add new entry
        accKFDataset.addEntry(new Entry(sampleCount++, (float)filtered));

        // Limit dataset size to 200 samples
        if (accKFDataset.getEntryCount() > MAX_SAMPLES) {
            accKFDataset.removeFirst();
            for (Entry entry : accKFDataset.getValues()) {
                entry.setX(entry.getX() - 1);
            }
            sampleCount = MAX_SAMPLES;
        }

        // Refresh chart
        accLineDataKF.notifyDataChanged();
        lineChartKF.notifyDataSetChanged();
        lineChartKF.setVisibleXRangeMaximum(MAX_SAMPLES);
        lineChartKF.moveViewToX(sampleCount);
    }

    public void UpdateTextUI(SimpleMatrix acc, double filtered){
        // Update UI
        binding.tvAccx.setText(String.format("Ax: %.4f m/s²", acc.get(0)));
        binding.tvAccy.setText(String.format("Ay: %.4f m/s²", acc.get(1)));
        binding.tvAccz.setText(String.format("Az: %.4f m/s²", acc.get(2)));
        binding.tvAcc.setText(String.format("Am: %.4f m/s^2", acc.normF()));
        //binding.tvAccBias.setText(String.format("Ab: %.4f m/s²", biasacc));
        binding.tvAccdyn.setText(String.format("Ad: %.4f m/s²", filtered));
        //binding.tvAccdyn.setText(String.format("Steps: %d", stepCounter));
    }
}
