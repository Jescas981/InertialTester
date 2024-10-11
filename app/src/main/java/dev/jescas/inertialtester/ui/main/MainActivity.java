package dev.jescas.inertialtester.ui.main;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.ejml.data.FMatrix3;
import org.ejml.simple.SimpleMatrix;


import java.util.Collections;

import dev.jescas.inertialtester.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements IMainView {
    private ActivityMainBinding binding;
    private LineChart orientationChart;
    private LineChart stepDetectionChart;

    // Orientation Chart
    private LineDataSet[] orientationDataset;
    private LineData orientationData;

    // Step Detection Chart
    private LineDataSet peaksDataset;
    private LineDataSet thresholdDataset;
    private LineData stepDetectionData;

    // Trajectory Chart
    private CombinedChart trajectoryChart;
    private CombinedData trajectoryData;
    private ScatterDataSet stepsMarkerDataset;
    private LineDataSet trajectoryPathDataset;
    // Chart Margin
    float maxCurrentX = 0f;
    float minCurrentX = 0f;
    float maxCurrentY = 0f;
    float minCurrentY = 0f;
    float margin = 0.4f;

    private static final int MAX_SAMPLES = 200; // Limit to 200 samples
    private int sampleCount = 0;
    private boolean onRecord = true;
    private IMainPresenter mainPresenter;
    private float currentX = 0f;  // Starting X position
    private float currentY = 0f;  // Starting Y position
    private float stepLength = 0.8f;  // Average step length in meters
    private int lastStepCount = 0;    // To store the previous step count

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

    public void OnRecord(View view) {
        if (onRecord) {
            binding.btnRecord.setText("Stop");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } else {
            binding.btnRecord.setText("Record");
            Toast.makeText(this, "Recording finished", Toast.LENGTH_SHORT).show();
        }
        mainPresenter.EnableRecord(onRecord);
        onRecord = !onRecord;
    }

    public void OnRecordFinished(String filepath) {
        Toast.makeText(this, "Saving file in " + filepath, Toast.LENGTH_SHORT).show();
    }

    public void SetupCharts() {
        // Orientation Chart
        orientationChart = binding.chartOrientation;
        orientationData = new LineData();
        orientationDataset = new LineDataSet[]{
                new LineDataSet(null, "Roll"),
                new LineDataSet(null, "Pitch"),
                new LineDataSet(null, "Yaw")
        };
        for (int i = 0; i < orientationDataset.length; i++) {
            orientationDataset[i].setDrawCircles(false);
            orientationDataset[i].setLineWidth(2f);
            orientationDataset[i].setDrawValues(false);
            orientationDataset[i].setColor(ColorTemplate.COLORFUL_COLORS[i]);
            orientationData.addDataSet(orientationDataset[i]);
        }
        orientationChart.setData(orientationData);

        // Acceleration Chart
        stepDetectionChart = binding.chartPeak;
        peaksDataset = new LineDataSet(null, "Acceleration");
        thresholdDataset = new LineDataSet(null, "Threshold");
        peaksDataset.setDrawCircles(false);
        peaksDataset.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        peaksDataset.setLineWidth(2f);
        peaksDataset.setDrawValues(false);

        thresholdDataset.setDrawCircles(false);
        thresholdDataset.setColor(ColorTemplate.COLORFUL_COLORS[1]); // Use a different color
        thresholdDataset.setLineWidth(2f);
        thresholdDataset.setDrawValues(false);

        stepDetectionData = new LineData(peaksDataset, thresholdDataset);
        stepDetectionChart.setData(stepDetectionData);

        // Trajectory Chart
        trajectoryChart = binding.chartTrajectory;
        stepsMarkerDataset = new ScatterDataSet(null, "Markers");
        stepsMarkerDataset.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        stepsMarkerDataset.setColor(Color.RED);
        stepsMarkerDataset.setScatterShapeSize(10f);
        stepsMarkerDataset.setDrawValues(false);

        trajectoryPathDataset = new LineDataSet(null, "Trajectory");
        trajectoryPathDataset.setDrawCircles(false);
        trajectoryPathDataset.setColor(Color.RED); // Use a different color
        trajectoryPathDataset.setLineWidth(2f);
        trajectoryPathDataset.setDrawValues(false);

        trajectoryData = new CombinedData();
        trajectoryData.setData(new ScatterData(stepsMarkerDataset));
        //trajectoryData.setData(new LineData(trajectoryPathDataset));
        trajectoryChart.setData(trajectoryData);

    }

    public void ConfigureCharts() {
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
        stepDetectionChart.getLegend().setEnabled(true);
        stepDetectionChart.getLegend().setTextColor(ColorTemplate.getHoloBlue());
        stepDetectionChart.getLegend().setTextSize(12f); // Adjust text size for the legend

        stepDetectionChart.getLegend().setEnabled(true);
        stepDetectionChart.getDescription().setEnabled(false);
        stepDetectionChart.setDrawGridBackground(false);

        XAxis xAxis2 = stepDetectionChart.getXAxis();
        xAxis2.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis2.setLabelCount(5, true);
        xAxis2.setValueFormatter(new IndexAxisValueFormatter());
        xAxis2.setDrawGridLines(false);

        // Enable the legend and set it for the datasets
        stepDetectionChart.getLegend().setEnabled(true);
        stepDetectionChart.getLegend().setTextColor(ColorTemplate.getHoloBlue());
        stepDetectionChart.getLegend().setTextSize(12f); // Adjust text size for the legend

        // Chart
        XAxis xAxis3 = trajectoryChart.getXAxis();
        xAxis3.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis3.setDrawGridLines(true);
        xAxis3.setAxisMaximum(3f);
        xAxis3.setAxisMinimum(-3f);
        // Customize the Y-Axis (Left)
        trajectoryChart.setAutoScaleMinMaxEnabled(true);
        trajectoryChart.getAxisRight().setEnabled(false); // Disable right Y-axis
        trajectoryChart.getAxisLeft().setDrawGridLines(true);
        trajectoryChart.getAxisLeft().setAxisMaximum(3f);
        trajectoryChart.getAxisLeft().setAxisMinimum(-3f);
        trajectoryChart.getLegend().setEnabled(false);
        trajectoryChart.getDescription().setEnabled(false);
    }

    public void AddEntriesChart(FMatrix3 orientation, double raw) {
        // Update orientation Chart
        for (int i = 0; i < orientationDataset.length; i++) {
            orientationDataset[i].addEntry(new Entry(sampleCount, orientation.get(0, i)));
            LimitDatasetSize(orientationDataset[i]);
        }
        // Refresh chart
        orientationData.notifyDataChanged();
        orientationChart.notifyDataSetChanged();
        orientationChart.setVisibleXRangeMaximum(MAX_SAMPLES);
        orientationChart.moveViewToX(sampleCount);

        // Update Acceleration Chart
        peaksDataset.addEntry(new Entry(sampleCount, (float) raw));
        thresholdDataset.addEntry(new Entry(sampleCount, 0.4f));
        LimitDatasetSize(peaksDataset);
        LimitDatasetSize(thresholdDataset);
        stepDetectionData.notifyDataChanged();
        stepDetectionChart.notifyDataSetChanged();
        stepDetectionChart.setVisibleXRangeMaximum(MAX_SAMPLES);
        stepDetectionChart.moveViewToX(sampleCount);
    }

    private void LimitDatasetSize(LineDataSet dataset) {
        if (dataset.getEntryCount() > MAX_SAMPLES) {
            dataset.removeFirst();
            for (Entry entry : dataset.getValues()) {
                entry.setX(entry.getX() - 1);
            }
            sampleCount = MAX_SAMPLES;
        }
    }


    public void UpdateTextUI(double acc, int steps, double heading, double position, double velocity) {
        if (steps > lastStepCount) {
            // Convert heading from degrees to radians
            double headingRadians = Math.toRadians(heading);
            // Calculate new position using the step length and heading
            currentX += (float) (stepLength * Math.cos(headingRadians));
            currentY += (float) (stepLength * Math.sin(headingRadians));
            stepsMarkerDataset.addEntry(new Entry(currentX, currentY));
            trajectoryPathDataset.addEntry(new Entry(currentX, currentY));

            if (currentX > maxCurrentX) {
                maxCurrentX = currentX;
            }
            if (currentY > maxCurrentY) {
                maxCurrentY = currentY;
            }
            if (currentY < minCurrentY) {
                minCurrentY = currentY;
            }
            if (currentX < minCurrentX) {
                minCurrentX = currentX;
            }

            // Calculate margin for chart
            float xmargin = (maxCurrentX - minCurrentX) * margin;
            float ymargin = (maxCurrentY - minCurrentY) * margin;
            // Sort entries based on X values
            stepsMarkerDataset.getValues().sort(new EntryXComparator());
            //Collections.sort(trajectoryPathDataset.getValues(), new EntryXComparator());

            // Set the new axis limits with margin
            XAxis xAxis = trajectoryChart.getXAxis();
            xAxis.setAxisMinimum(minCurrentX - xmargin);
            xAxis.setAxisMaximum(maxCurrentX + xmargin);
            YAxis leftAxis = trajectoryChart.getAxisLeft();
            leftAxis.setAxisMinimum(minCurrentY - ymargin);
            leftAxis.setAxisMaximum(maxCurrentY + ymargin);

            trajectoryData.notifyDataChanged();
            trajectoryChart.notifyDataSetChanged();
            trajectoryChart.invalidate();
        }

        // Update last step count
        lastStepCount = steps;
    }
}
