package com.ar.motionauthprototype;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ResultScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_screen);
        if(getIntent().getExtras().getBoolean("auth") == true){
            setTitle("User Authenticated");
            TextView tv=(TextView)findViewById(R.id.message);
            tv.setText("Welcome!");
        }else{
            finish();
        }
    }
}
