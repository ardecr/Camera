package com.example.camera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class Proximity {


    private Sensor proximitySensor;
    private static float DISTANCE=2.5f;

    public Proximity(Context context){

        SensorManager sensorManager=(SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor=sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    public Sensor getProximitySensor() {
        return proximitySensor;
    }

    public boolean isNear(float value){
        return value <= DISTANCE;
    }


}
