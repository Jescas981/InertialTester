package dev.jescas.inertialtester;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateValueFormatter extends ValueFormatter {
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // Set your desired time format

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return sdf.format(new Date((long) value)); // Format the timestamp to display as a human-readable time
    }
}
