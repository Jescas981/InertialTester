package dev.jescas.inertialtester;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;


import dev.jescas.inertialtester.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private boolean recording = false;
    private StepDetector stepDetector = new StepDetector(0.4);
    LineChart lineChartRaw, lineChartKF;
    private LineDataSet accMagnitudeDataSet, accKFDataset;
    private LineData accLineDataRaw, accLineDataKF;
    private LowPassFilter lpf = new LowPassFilter();
    private int sampleCount = 0;
    private int stepCounter = 0;
    private static final int MAX_SAMPLES = 200; // Limit to 200 samples

    private ByteArrayOutputStream accByteStream = new ByteArrayOutputStream();
    private KalmanFilter kalmantFilter = new KalmanFilter();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);

        binding.btnRecord.setOnClickListener(this::OnRecord);

        // Initialize chart
        lineChartRaw = binding.chartraw;
        lineChartKF = binding.chartkf;
        setupLineChart();

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

    private void setupLineChart() {
        lineChartRaw.getDescription().setEnabled(false);
        lineChartRaw.setDrawGridBackground(false);

        XAxis xAxis = lineChartRaw.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(5, true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter());
        xAxis.setDrawGridLines(false);

        // Customize the Y-Axis (Left)
        lineChartRaw.getAxisRight().setEnabled(false); // Disable right Y axis
        lineChartRaw.getAxisLeft().setDrawGridLines(true);
        lineChartRaw.getAxisLeft().setAxisMinimum(8f); // Set the minimum value of the Y axis
        lineChartRaw.getAxisLeft().setAxisMaximum(12f); // Set the maximum value of the Y axis
        lineChartRaw.getAxisLeft().setLabelCount(10, true); // Set number of ticks/labels on the Y axis

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

    protected void OnRecord(View view) {
        if (recording) {
            // Stop recording
            recording = false;
            binding.btnRecord.setText("Record");
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            saveSensorData();
        } else {
            // Start recording
            recording = true;
            binding.btnRecord.setText("Stop");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSensorData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());

        // Reset the ByteArrayOutputStream after saving
        accByteStream.reset();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long timestamp = System.currentTimeMillis();
        ByteBuffer byteBuffer = ByteBuffer.allocate(20);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.putLong(timestamp);

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = event.values;

            // Update UI
            binding.tvAccx.setText(String.format("Ax: %.4f m/s²", values[0]));
            binding.tvAccy.setText(String.format("Ay: %.4f m/s²", values[1]));
            binding.tvAccz.setText(String.format("Az: %.4f m/s²", values[2]));
            double magnitude = Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2));
            binding.tvAcc.setText(String.format("Am: %.4f m/s^2", magnitude));

            kalmantFilter.predict();
            kalmantFilter.update(magnitude);
            double dynacc = kalmantFilter.getDynamicAcceleration();
            double biasacc = kalmantFilter.getBias();
            binding.tvAccBias.setText(String.format("Ab: %.4f m/s²", biasacc));
            binding.tvAccdyn.setText(String.format("Ad: %.4f m/s²", dynacc));
            binding.tvAccdyn.setText(String.format("Steps: %d", stepCounter));
            // Add magnitude to the chart
            dynacc = lpf.applyFilter(dynacc);
            addEntryToChart((float) magnitude, (float) dynacc);
            if(stepDetector.detectPeak(dynacc)){
                stepDetector.getPeakInfo();
                stepCounter++;
            }
            // Store data for the chart
            if (recording) {
                for (float value : values) byteBuffer.putFloat(value);
                writeToStream(accByteStream, byteBuffer);

            }
        }
    }

    private void addEntryToChart(float magnitude, float dynacc) {
        // Add new entry
        accMagnitudeDataSet.addEntry(new Entry(sampleCount++, magnitude));

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
        accKFDataset.addEntry(new Entry(sampleCount++, dynacc));

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

    private void writeToStream(ByteArrayOutputStream stream, ByteBuffer buffer) {
        try {
            stream.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // No implementation needed
    }
}
