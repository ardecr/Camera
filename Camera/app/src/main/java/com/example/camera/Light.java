package com.example.camera;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;

public class Light {

    private Context context;
    private Sensor lightSensor;

    public Sensor getLightSensor(){return lightSensor;}

    public Light(Context context){
        this.context =context;
        SensorManager sensorManager=(SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor=sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public void menageLight(float changedVale ){
        if (changedVale < 2) {
            setBrightness(80);
        }
        if (changedVale >= 2 && changedVale < 80) {
            setBrightness(120);
        }
        if (changedVale >= 80) {
            setBrightness(180);
        }
    }

    private void setBrightness(int brightness){
        if(brightness<0)
            brightness=0;
        if(brightness>255)
            brightness=255;
        ContentResolver contentResolver= context.getApplicationContext().getContentResolver();
        Settings.System.putInt(contentResolver,Settings.System.SCREEN_BRIGHTNESS,brightness);
    }
}
