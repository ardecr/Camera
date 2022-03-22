package com.example.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.jetbrains.annotations.Nullable;


public class OptionActivity extends AppCompatActivity {

    private Button bokeh;
    private Button video;
    private int firstCreation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);
        firstCreation=1;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
            getWindow().setNavigationBarColor(Color.BLACK);
        bokeh=findViewById(R.id.ritratto);
        video= findViewById(R.id.videoButton);
        bokeh.setOnClickListener(view -> bokehMode());
        video.setOnClickListener( view->videoMode());

    }

    private void bokehMode(){
        Intent intent= new Intent(this,BokehActivity.class);
        startActivity(intent);
    }
    private void videoMode(){
        Intent intent= new Intent(this,VideoModeActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(firstCreation==1)
            firstCreation++;
        else{ finish();}
    }
}