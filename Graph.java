package com.ar.motionauthprototype;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Graph extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    final float alpha = 0.8f;
    float[] gravity=new float[3];
    float[] linear_acceleration=new float[3];
    private ArrayList<Pair<Long, float[]>> sensorLog;
    private Button btnHold;
    private TextView[] acc_tv =new TextView[3];
    private float[] tmpacc=new float[]{0f,0f,0f};
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeriesx,mSeriesy,mSeriesz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        GraphView graph = (GraphView) findViewById(R.id.graph);
        sensorManager.registerListener
                (Graph.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
//        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
//                new DataPoint(0, 1),
//                new DataPoint(1, 5),
//                new DataPoint(2, 3),
//                new DataPoint(3, 2),
//                new DataPoint(4, 6)
//        });
//        graph.addSeries(series);
        initGraph(graph);
        mTimer = new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 0.05d;
                mSeriesx.appendData(new DataPoint(graphLastXValue, tmpacc[0]), true, 80);
                mSeriesy.appendData(new DataPoint(graphLastXValue, tmpacc[1]), true, 80);
                mSeriesz.appendData(new DataPoint(graphLastXValue, tmpacc[2]), true, 80);
                mHandler.postDelayed(mTimer, 50);
            }
        };
        mHandler.postDelayed(mTimer, 100);

    }

    public void initGraph(GraphView graph) {
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(4);

        graph.getViewport().setMinY(-10);
        graph.getViewport().setMaxY(10);
        graph.getGridLabelRenderer().setLabelVerticalWidth(100);

        // x
        mSeriesx = new LineGraphSeries<>();
//        mSeriesx.setDrawBackground(true);
        mSeriesx.setTitle("x");
        //y
        mSeriesy = new LineGraphSeries<>();
        mSeriesy.setColor(Color.argb(255, 255, 60, 60));
//        mSeriesy.setBackgroundColor(Color.argb(50, 255, 60, 60));
//        mSeriesy.setDrawBackground(true);
        mSeriesy.setTitle("y");
        //y
        mSeriesz = new LineGraphSeries<>();
        mSeriesz.setColor(Color.argb(255, 150, 120, 60));
//        mSeriesz.setBackgroundColor(Color.argb(50, 255, 60, 60));
//        mSeriesz.setDrawBackground(true);
        mSeriesz.setTitle("z");


        graph.addSeries(mSeriesx);
        graph.addSeries(mSeriesy);
        graph.addSeries(mSeriesz);

        // legend
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];
            Log.d("acc log","filtered Sensor data : " + Arrays.toString(linear_acceleration));
            int i=0;

            for (float val:linear_acceleration) {
                tmpacc[i++]=val;
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }




    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(Graph.this);
        mHandler.removeCallbacks(mTimer);
    }

    double mLastRandom = 2;
    Random mRand = new Random();
    private double getRandom() {
        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
    }
}
