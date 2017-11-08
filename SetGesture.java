package com.ar.motionauthprototype;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class SetGesture extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    final double alpha = 0.8f;
    double[] gravity=new double[3];
    double[] linear_acceleration=new double[3];
    private ArrayList<Pair<Long, double[]>> sensorData;
    private ArrayList<Double> rollAvg;
    private ArrayList<Double> accAvg;
    private Button btnHold;
    private TextView[] acc_tv =new TextView[3];
    private double[] tmpacc=new double[]{0f,0f,0f};
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeriesx,mSeriesy,mSeriesz;
    private int count = 0,first=1;
    private String csvData="";

    private Object mPauseLock;
    private boolean mPaused;

    private static final int REQUEST_CODE = 0x11;

    String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Set Gesture");
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE); // without sdk version check
        setContentView(R.layout.activity_set_gesture);
        mPauseLock = new Object();
        mPaused = false;
        GraphView graph = (GraphView) findViewById(R.id.graph);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        btnHold= (Button)findViewById(R.id.btn_hold);
        btnHold.setOnTouchListener(holdListener);

//        acc_tv[0] = (TextView)findViewById(R.id.acc_x);
//        acc_tv[1] = (TextView)findViewById(R.id.acc_y);
//        acc_tv[2] = (TextView)findViewById(R.id.acc_z);

        initGraph(graph);
        Thread t = new Thread();
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
                    sensorManager.registerListener
                            (SetGesture.this, accelerometer,SensorManager.SENSOR_DELAY_FASTEST);

                    break;
                case MotionEvent.ACTION_UP:
                    Log.d("Debug::", "Up");
                    Long starting=0l;
                    double rollavg=0f;
                    sensorManager.unregisterListener(SetGesture.this);
                    if (sensorData.size() > 150) {
                        Toast.makeText(SetGesture.this, "OK", Toast.LENGTH_SHORT).show();

                        for(Pair<Long,double[]> pair : sensorData){
                            csvData+=pair.second[0]+","+pair.second[1]+","+pair.second[2];
                            csvData+="\n";
                        }
                        SaveDataToFile(sensorData, SetGesture.this, count);
//                        first=1;
//                        rollAvg.clear();
//                        for(Pair<Long,double[]> pair : sensorData){
//                            rollavg=0f;
//                            if(first==1){
//                                first=0;
//                                starting=pair.first;
//                            }
//                            accAvg.add((pair.second[0]+pair.second[1]+pair.second[2])/3);
//                            for(double a : accAvg){
//                                rollavg+=a;
//                            }
//                            rollavg=rollavg/accAvg.size();
//                            rollAvg.add(rollavg);
//                            allcsvData+="a,";
//                            allcsvData+=(pair.first-starting)+",";
//                            allcsvData+=pair.second[0]+","+pair.second[1]+","+pair.second[2];
//                            allcsvData+="\n";
//
//                        }
                        Log.d("Debug::",Environment.getExternalStorageDirectory()+"/accData/CC" );

                        count++;
                        btnHold.setText(Integer.toString(3-count));
                        Arrays.fill(gravity,0f);
                        if (count== 3){
                            try {

                                double dist = getInitialDistance();
                                Log.d("Debug::",Environment.getExternalStorageDirectory()+"/accData/" + "d" + Double.toString(dist));
                                File Dir = new File(Environment.getExternalStorageDirectory()+"/accData");
                                Dir.mkdirs();
                                //Deleteing previous dist file
                                File dirFiles[] = new File(Dir,"").listFiles();
                                String restOfFileName="";
                                for (File aFile : dirFiles) {
                                    if (aFile.getName().startsWith("d")) {
                                        restOfFileName = aFile.getName().substring(1);
                                        File dfile = new File(Dir, "d"+restOfFileName);
                                        if (dfile.exists()){
                                            dfile.delete();
                                            Log.d("Debug::",restOfFileName+" DELETED");
                                        }
                                    }
                                }
                                //creating new dist file
                                new File(Dir ,"d" + Double.toString(dist)).createNewFile();
                            }catch (Exception e){
                                e.getMessage();
                            }
                            startActivity(new Intent(getApplicationContext(),Menu.class));
                        }
                    }
                    else {
                        Toast.makeText(SetGesture.this, "Try holding the button longer", Toast.LENGTH_SHORT).show();
                    }

                    break;
            }
            return true;
        }
    };

    private double getInitialDistance() throws IOException, ClassNotFoundException {
        File Dir = new File(Environment.getExternalStorageDirectory()+"/accData");
        Dir.mkdirs();

        final com.timeseries.TimeSeries ts0 = new com.timeseries.TimeSeries("c0.csv", false, false, ',');
        final com.timeseries.TimeSeries ts1 = new com.timeseries.TimeSeries("c1.csv", false, false, ',');
        final com.timeseries.TimeSeries ts2 = new com.timeseries.TimeSeries("c2.csv", false, false, ',');
        final DistanceFunction distFn;
        distFn = DistanceFunctionFactory.getDistFnByName("EuclideanDistance");
        return (FastDTW.getWarpInfoBetween(ts0,ts1,10,distFn).getDistance()+
                FastDTW.getWarpInfoBetween(ts0,ts2,10,distFn).getDistance()+
                FastDTW.getWarpInfoBetween(ts1,ts2,10,distFn).getDistance())/3;
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

            //Log.d("acc log","filtered Sensor data : " + Arrays.toString(linear_acceleration));
            int i=0;
            //Make it red if there is change in acceleration beyond threshold
//            for (double val:tmpacc) {
//                if((linear_acceleration[i]-val)>0.1){
//                    //red
//                    acc_tv[i].setTextColor(Color.parseColor("#d42e2e"));
//                }else{
//                    acc_tv[i++].setTextColor(Color.parseColor("#0d9839"));
//                }
//            }
            i=0;
            for (double val:linear_acceleration) {
                tmpacc[i++]=val;
            }

//            acc_tv[0].setText("x:"+String.valueOf(linear_acceleration[0]));
//            acc_tv[1].setText("y:"+String.valueOf(linear_acceleration[1]));
//            acc_tv[2].setText("z:"+String.valueOf(linear_acceleration[2]));
//            Log.d("acc log","DATA:\t"+event.timestamp+"\t"+ event.values[0]+"\t"+event.values[1]+"\t"+event.values[2]+"\t"+ linear_acceleration[0]+"\t"+linear_acceleration[1]+"\t"+linear_acceleration[2]);
            sensorData.add(new Pair<>(System.nanoTime(),new double[]{linear_acceleration[0],linear_acceleration[1],linear_acceleration[2]}));


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void SaveDataToFile(ArrayList<Pair<Long, double[]>> sensorData, Activity activity, int curveCount) {
        Long starting=0l;
        File Dir = new File(Environment.getExternalStorageDirectory()+"/accData");
        Dir.mkdirs();
        String fileName = "c" + Integer.toString(curveCount);
        File file = new File(Dir, fileName);
        File csvFile = new File(Dir,"c" + Integer.toString(curveCount)+".csv");
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
            fos = new FileOutputStream(file);
            cfos = new FileOutputStream(csvFile);
//            BufferedOutputStream bos = new BufferedOutputStream(cfos);
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
}

