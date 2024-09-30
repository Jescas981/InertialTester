package dev.jescas.inertialtester.ui.main;

import android.content.Context;
import android.hardware.SensorEventListener;

import java.util.EventListener;

public interface IMainPresenter extends SensorEventListener {
     void EnableRecord(boolean enable);
}
