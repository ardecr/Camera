package com.example.camera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.CountDownTimer;

import java.util.concurrent.Semaphore;

public class Gyroscope {

    private  float ROTATIONGAP=3.0f;
    private int count=0;
    private float currentZPosition;
    private CameraSwitch cameraFunction;
    private Sensor gyroscopeSensor;
    private Semaphore semaphore;

    public Gyroscope(CameraSwitch cameraFunction, Context context){
        this.cameraFunction=cameraFunction;
        SensorManager sensorManager=(SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscopeSensor=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        semaphore=new Semaphore(1);
    }

    public  Sensor getGyroscopeSensor(){return  gyroscopeSensor;}

    public void menageRotation(float rx)  {
        if(currentZPosition==270.0f){
            currentZPosition=rx;
        }
        else {
            if (rx-currentZPosition>=ROTATIONGAP) {
                controlUnintentionalMovements();
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++;
                semaphore.release();
                currentZPosition = rx;
            }
            if(count>=1 && currentZPosition > rx  && currentZPosition - rx>=ROTATIONGAP/2) {
                cameraFunction.switchCamera();
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count=0;
                semaphore.release();
                currentZPosition = rx;
            }
        }
    }
    private void controlUnintentionalMovements (){
        long duration=1500;
        new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count=0;
                semaphore.release();
            }
        }.start();
    }


}
