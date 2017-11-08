package com.ar.motionauthprototype;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dtw.FastDTW;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.util.DistanceFunction;
import com.util.DistanceFunctionFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class LockScreen extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    final double alpha = 0.8f;
    double[] gravity=new double[3];
    double[] linear_acceleration=new double[3];
    private ArrayList<Pair<Long, double[]>> sensorData;
    private Button btnHold;
    private TextView[] acc_tv =new TextView[3];
    private double[] tmpacc=new double[]{0f,0f,0f};
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeriesx,mSeriesy,mSeriesz;
    Vibrator vib;

    private Object mPauseLock;
    private boolean mPaused;
    private String csvData="";

    private static final int REQUEST_CODE = 0x11;

    String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_test);


        mPauseLock = new Object();
        mPaused = false;
        GraphView graph = (GraphView) findViewById(R.id.graph);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        btnHold= (Button)findViewById(R.id.btn_hold);
        btnHold.setOnTouchListener(holdListener);
        vib= (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
        mSeriesx.setTitle("x");
        //y
        mSeriesy = new LineGraphSeries<>();
        mSeriesy.setColor(Color.argb(255, 255, 60, 60));
        mSeriesy.setTitle("y");
        //y
        mSeriesz = new LineGraphSeries<>();
        mSeriesz.setColor(Color.argb(255, 150, 120, 60));
        mSeriesz.setTitle("z");


        graph.addSeries(mSeriesx);
        graph.addSeries(mSeriesy);
        graph.addSeries(mSeriesz);

        // legend
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
    }

    private View.OnTouchListener holdListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d("Debug::", "Down");
                    csvData="";

                    sensorData = new ArrayList<>();
                    sensorManager.registerListener(LockScreen.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                    break;
                case MotionEvent.ACTION_UP:

                    Log.d("Debug::", "Up");
                    sensorManager.unregisterListener(LockScreen.this);
                    if (sensorData.size() > 150) {
                        for(Pair<Long,double[]> pair : sensorData){
                            csvData+=pair.second[0]+","+pair.second[1]+","+pair.second[2];
                            csvData+="\n";
                        }
                        SaveDataToFile(sensorData,LockScreen.this, 99);
                        //Toast.makeText(LockScreen.this, "Verifying...", Toast.LENGTH_SHORT).show();
//                        double[][] recordedGesture = Gesture.prepForCompare(sensorData);
                        if (isCloseEnough()) {
                            /**
                             * Authenticated!
                             * Opening resultscreen
                             */
                            vib.vibrate(50);
                            Intent i = new Intent(LockScreen.this,ResultScreen.class);
                            i.putExtra("auth",true);
                            startActivity(i);
                            Log.d("Debug::", "Welcome!");
                            Snackbar.make(findViewById(R.id.activity_set_gesture), "Welcome!", Snackbar.LENGTH_SHORT).show();
                        }else {
                            Log.d("Debug::", "Incorrect gesture,Try Again.");
                            vib.vibrate(500);
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.RED));
                            Snackbar.make(findViewById(R.id.activity_set_gesture), "Incorrect gesture,Try Again.", Snackbar.LENGTH_SHORT).show();
                        }

                    }
                    else {
                        Toast.makeText(LockScreen.this, "Try holding the button longer", Toast.LENGTH_SHORT).show();
                    }

                    break;
            }
            return true;
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];

            //Log.d("acc log","filtered Sensor data : " + Arrays.toString(linear_acceleration));
            int i=0;
            for (double val:linear_acceleration) {
                tmpacc[i++]=val;
            }
             sensorData.add(new Pair<>(System.nanoTime(),new double[]{linear_acceleration[0],linear_acceleration[1],linear_acceleration[2]}));


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void SaveDataToFile(ArrayList<Pair<Long, double[]>> sensorData, Activity activity, int curveCount) {
        File Dir = new File(Environment.getExternalStorageDirectory()+"/accData");
        Dir.mkdirs();
        String fileName = "c" + Integer.toString(curveCount);
        File file = new File(Dir, fileName);
        File csvFile = new File(Dir,"c" + Integer.toString(curveCount)+".csv");
        //Deleting already existing files
        if (file.exists() || csvFile.exists()){
            file.delete();
            csvFile.delete();
            Log.d("FILEDELETE","DELETED");
        }
        try{
            csvFile.createNewFile();
            file.createNewFile();
        }catch(Exception e){
            e.printStackTrace();
        }
        FileOutputStream fos,cfos ;
        try {
//            fos = new FileOutputStream(file);
            cfos = new FileOutputStream(csvFile);
            OutputStreamWriter osw = new OutputStreamWriter(cfos);
//            Log.d("csv",csvData);
            osw.write(csvData);
            osw.flush();
            osw.close();


        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Save file failed", Toast.LENGTH_SHORT).show();
        }

    }
    
    private boolean isCloseEnough() {
        File Dir = new File(Environment.getExternalStorageDirectory()+"/accData");
        Dir.mkdirs();
        final com.timeseries.TimeSeries ts0 = new com.timeseries.TimeSeries("c0.csv", false, false, ',');
        final com.timeseries.TimeSeries ts1 = new com.timeseries.TimeSeries("c1.csv", false, false, ',');
        final com.timeseries.TimeSeries ts2 = new com.timeseries.TimeSeries("c2.csv", false, false, ',');
        final com.timeseries.TimeSeries tsU = new com.timeseries.TimeSeries("c99.csv", false, false, ',');
        final DistanceFunction distFn;
        distFn = DistanceFunctionFactory.getDistFnByName("EuclideanDistance");
        double avgDist=(FastDTW.getWarpInfoBetween(tsU,ts0,10,distFn).getDistance()+
                FastDTW.getWarpInfoBetween(tsU,ts1,10,distFn).getDistance()+
                FastDTW.getWarpInfoBetween(tsU,ts2,10,distFn).getDistance())/3;
        File dirFiles[] = new File(Dir,"").listFiles();
        double initialDist = 0;
        for (File aFile : dirFiles) {
            if (aFile.getName().startsWith("d")) {
                String restOfFileName = aFile.getName().substring(1);
                initialDist = Double.parseDouble(restOfFileName);
                break;
            }
        }
        System.out.println("Original distance " + Double.toString(initialDist));
        System.out.println("Found distance " + Double.toString(avgDist));
        System.out.println("Ratio" + Double.toString( avgDist/initialDist));

        if (avgDist / initialDist < 1.35)
            return true;
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // save file
            } else {
                Toast.makeText(getApplicationContext(), "PERMISSION_DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onResume() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        super.onResume();
    }
}
