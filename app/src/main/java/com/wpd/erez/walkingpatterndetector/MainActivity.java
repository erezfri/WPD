package com.wpd.erez.walkingpatterndetector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    Context context;
    //timer
    private Handler mHandler = new Handler();
    private long startTime;
    private long firstTimeDif;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;
    private String hours, minutes, seconds, milliseconds;
    private long secs, mins, hrs;
    private boolean stopped = true;
    private Runnable startTimer;
    private Runnable startSensorDetection;
    private Handler sensorHandler = new Handler();

    Boolean firstTimeInPacketAdd = true;
    float timeDiff = 0;

    public static boolean D_MULTI_SENSOR_FILE;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.wpd.erez.walkingpatterndetector/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.wpd.erez.walkingpatterndetector/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    //Sensor Experiment info variables
    public int[] mySensor;
    public boolean mTime;

    public int mSensorNum = 1;//=mSenorTypeGroup.length;
    private boolean mDefaultSensor;


    //timer
    public MainActivity() {

        startTimer = new Runnable() {
            public void run() {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimer(elapsedTime);
                mHandler.postDelayed(this, REFRESH_RATE);

            }
        };
        // submit task to threadpool:


        //Sensor Experiment info variables
        mySensor = new int[]{Sensor.TYPE_ACCELEROMETER};
        mDefaultSensor = false;
        //sensorDelay = SensorManager.SENSOR_DELAY_GAME;
        //Packet variables
        mTotSampNum = 100;

        mSampByteNum = mTime ? 4 + 4 : 4; //if time change to 4 change GraphAddData val

        mSensorMaxSamp = new int[mSensorNum];
        for (int i = 0; i < mSensorNum; i++) {
            mSensorMaxSamp[i]
                    = mTotSampNum / mSensorNum;
        }
        SetPosition();
    }

    //sensor
    private ArrayList<Sensor> mSensorGroup = new ArrayList<Sensor>(mSensorNum);
    private boolean mAcquisitionFlag = false;
    private SensorManager mSensorManager;
    //public static int sensorDelay = SensorManager.SENSOR_DELAY_GAME;//default value

    //Packet variables
    public int mTotSampNum;
    public int[] mSensorMaxSamp; // if sensors has different freq
    public int mSampByteNum;
    public ByteBuffer mPacket;   // for each sensor sampnum(int)|time(long)| value(int)
    public int[] mSampCount;
    public int[] mSampCountPos; //the position of the counter of the sensor's samples number
    public int[] mPosition;     // the index(dynamic) of the next sample related to a specific sensor
    public int mCurSensor;      //used for fast ploting at multi-sensor
    public float mCurVal, mCurTime; //  for fast ploting at multi-sensor
    private boolean initPos = true; // for once initilization of mSampCountPos
    public byte[] message;
    public long startTime2;

    //VIEW,FILES
    private Activity mActivity = this;
    private ArrayList<byte[]> Packets;

    //FILES
    FileWriter filewriter;
    private String sampleName = "";

    TextView sensorType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);

        context = getApplicationContext();
        Packets = new ArrayList<byte[]>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setVisibility(View.GONE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Check if the network is available first (for sending the data at the end of the recording)
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected()==true;

        if (!(isConnected)){
            Toast.makeText(context,"NO NETWORK AVAILABLE! PLEASE CONNECT TO A WIFI/CELLULAR NETWORK",Toast.LENGTH_LONG).show();
        }

        //Get SensorManager and sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mSensorGroup = getSensors(mSensorManager);
        sensorType = (TextView) findViewById(R.id.txtSensorType);
        sensorType.setText("Accelerometer Sensor is ready");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        startSensorDetection = new Runnable() {
            public void run() {
                Packets = new ArrayList<byte[]>();
                SetStart();
                mAcquisitionFlag = true;
                registerSensorListener();
                sensorHandler.postDelayed(this, REFRESH_RATE);
            }
        };
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 112);
            return;
        }
    }

    /**
     * getSensors gets the multi-sensor specific sensor.
     * the sensor type is default or Google.
     */
    public ArrayList<Sensor> getSensors(SensorManager sensorManager) {
        ArrayList<Sensor> sensorGroup = new ArrayList<Sensor>(mSensorNum);

        //take the default sensors - HW
        if (mDefaultSensor) {
            //Get SensorManager and sensors
            for (int i = 0; i < mSensorNum; i++) {
                sensorGroup.add(sensorManager.getDefaultSensor(mySensor[i]));
            }
        } else {
            //take Android open source sensors
            for (int i = 0; i < mSensorNum; i++) {
                List<Sensor> sensorList = sensorManager.getSensorList(mySensor[i]);
                for (Sensor sensor : sensorList) {
                    if (sensor.getVendor().contains("Google Inc.")) {
                        sensorGroup.add(sensor);
                        break;
                    }
                }
            }
        }
        if (sensorGroup.size() == 0) {
            for (int i = 0; i < mSensorNum; i++) {
                sensorGroup.add(sensorManager.getDefaultSensor(mySensor[i]));
            }
        }

        return sensorGroup;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.

        if (mAcquisitionFlag) {
            registerSensorListener();
        }

    }
    @Override
    protected void onPause() {

        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    private void registerSensorListener() {
        // register to SensorEventListener
        for (int i = 0; i < mSensorNum; i++) {
            mSensorManager.registerListener(this, mSensorGroup.get(i), SENSOR_DELAY_GAME); //30000 because it will get us 30 samples in 1 seconds
                                                                        //=20000
        }

    }

    public void startClick(View view) {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED )
        {
            Toast.makeText(context,"You don't have permission to write with this app, please add permission and try again",Toast.LENGTH_SHORT).show();
            return;
        }
        showStopButton();
        if (stopped) {
            startTime = System.currentTimeMillis();// - elapsedTime;
        } else {
            startTime = System.currentTimeMillis();
        }
        mHandler.removeCallbacks(startTimer);
        mHandler.postDelayed(startTimer, 0);
        Calendar c = Calendar.getInstance();
        sampleName = "sample_" + c.getTime().toString();
        CreateFile(sampleName + ".csv");
        sensorHandler.removeCallbacks(startSensorDetection);
        sensorHandler.postDelayed(startSensorDetection, 0);

    }

    public void helpClick(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.help)
                .setTitle("Help")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    public void stopClick(View view) {
        try {
            mSensorManager.unregisterListener((SensorEventListener) mActivity);
        } catch (Exception e) {
        }
        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WPD/";

        //Packets2File(Packets);
        try {
            filewriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        hideStopButton();
        mHandler.removeCallbacks(startTimer);
        sensorHandler.removeCallbacks(startSensorDetection);
        ((TextView) findViewById(R.id.counterText)).setText("00:00:00");
        stopped = false;

//       AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//       builder.setTitle("Send CSV")
//               .setMessage("Do you want to send us the data?")
//               .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                   public void onClick(DialogInterface dialog, int id) {
//                       Intent emailIntent = new Intent(Intent.ACTION_SEND);
//                       emailIntent.setData(Uri.parse("mailto:"));
//                       emailIntent.setType("plain/text");
//                       emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"ahofman@rambam.health.gov.il"});
//                       emailIntent.putExtra(Intent.EXTRA_SUBJECT, sampleName );
//                       Uri uri = Uri.parse("file://" + path + sampleName + ".csv");
//                       emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
//                       try {
//                          startActivity(Intent.createChooser(emailIntent, "Send mail..."));
//                           //startService(emailIntent);

//                       } catch (ActivityNotFoundException ex) {
//                           Toast.makeText(MainActivity.this,
//                                   "There is no email client installed.", Toast.LENGTH_SHORT).show();
 //                       }
//                    }});
//        builder.setNegativeButton("NO", null);
//        builder.create();
//        builder.show();
        //Toast.makeText(context,"The data was sent, thanks!",Toast.LENGTH_SHORT).show();


       CharSequence sendOptions[] = new CharSequence[] {"Send to Prof. Hoffman ", "Send To Someone Else", "Nothing"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What do you want to do with the data?");
        builder.setItems(sendOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                       Intent emailIntent = new Intent(Intent.ACTION_SEND);
                       emailIntent.setData(Uri.parse("mailto:"));
                       emailIntent.setType("plain/text");
                       emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"ahofman@rambam.health.gov.il"});
                       emailIntent.putExtra(Intent.EXTRA_SUBJECT, sampleName );
                       Uri uri = Uri.parse("file://" + path + sampleName + ".csv");
                       emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                       try {
                          startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                       } catch (ActivityNotFoundException ex) {
                           Toast.makeText(MainActivity.this,
                                   "There is no email client installed.", Toast.LENGTH_SHORT).show();
                       }
                        break;
                        /*
                           new AsyncTask<Void, Void, String>() {
                            @Override
                            protected String doInBackground(Void... params) {
                                try {
                                    Uri uri = Uri.parse("file://" + path + sampleName + ".csv");
                                    File sampleFile = new File(uri.getPath());
                                    GMailSender sender = new GMailSender("wpdapp@gmail.com", "technion1234567890");
                                    sender.sendMail(sampleName,
                                            "",
                                            "wpdapp@gmail.com",
                                            "wpdapp@gmail.com", sampleFile);
                                } catch (Exception e) {
                                    Log.e("SendMail", e.getMessage(), e);
                                }
                                return null;
                            }
                        }.execute(null,null,null);
                        */
                        /*
                        Toast.makeText(context,"The data was sent, thanks!",Toast.LENGTH_SHORT).show();
                        Intent emailIntent1 = new Intent(Intent.ACTION_SEND);
                        emailIntent1.setData(Uri.parse("mailto:"));
                        emailIntent1.setType("text/plain");
                        emailIntent1.putExtra(Intent.EXTRA_EMAIL, new String[] {"ahofman@rambam.health.gov.il"});
                        emailIntent1.putExtra(Intent.EXTRA_SUBJECT, sampleName );
                        Uri uri = Uri.parse("file://" + path + sampleName + ".csv");
                        emailIntent1.putExtra(Intent.EXTRA_STREAM, uri);
                        try {
                            startActivity(Intent.createChooser(emailIntent1, "Send mail..."));
                            //startService(emailIntent);

                        } catch (ActivityNotFoundException ex) {
                            Toast.makeText(MainActivity.this,
                                    "There is no email client installed.", Toast.LENGTH_SHORT).show();
                        }
*/


                    case 1:
                       Intent emailIntent2 = new Intent(Intent.ACTION_SEND);
                        emailIntent2.setData(Uri.parse("mailto:"));
                        emailIntent2.setType("plain/text");
                        emailIntent2.putExtra(Intent.EXTRA_EMAIL, new String[] {""});
                        emailIntent2.putExtra(Intent.EXTRA_SUBJECT, sampleName );
                        Uri uri1 = Uri.parse("file://" + path + sampleName + ".csv");
                        emailIntent2.putExtra(Intent.EXTRA_STREAM, uri1);
                       try {
                          startActivity(Intent.createChooser(emailIntent2, "Send mail..."));
                           //startService(emailIntent);

                       } catch (ActivityNotFoundException ex) {
                           Toast.makeText(MainActivity.this,
                                   "There is no email client installed.", Toast.LENGTH_SHORT).show();
                       }
                       break;

                   case 2:Toast.makeText(context,"The file is located in the WPD folder in your device",Toast.LENGTH_SHORT).show();
                       break;

                   default:
                       Toast.makeText(context,"No option has been chosen",Toast.LENGTH_SHORT).show();
                       break;

               }
           }
       });
       builder.show();

//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setTitle("Send CSV")
//                .setMessage("Do you want to send us the data?")
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        new AsyncTask<Void, Void, String>() {
//                            @Override
//                            protected String doInBackground(Void... params) {
//                                try {
//                                    Uri uri = Uri.parse("file://" + path + sampleName + ".csv");
//                                    File sampleFile = new File(uri.getPath());
//                                    GMailSender sender = new GMailSender("wpdapp@gmail.com", "technion123456789");
//                                    sender.sendMail(sampleName,
//                                            "",
//                                            "wpdapp@gmail.com",
//                                            "wpdapp@gmail.com", sampleFile);
//                                } catch (Exception e) {
//                                    Log.e("SendMail", e.getMessage(), e);
//                                }
//                                return null;
//                            }
//                        }.execute(null,null,null);
//                        Toast.makeText(context,"The data was sent, thanks!",Toast.LENGTH_SHORT).show();
//                    }});
//        builder.setNegativeButton("NO", null);
//        builder.create();
//        builder.show();

//        new AsyncTask<Void, Void, String>() {
//            @Override
//            protected String doInBackground(Void... params) {
//                try {
//                    Uri uri = Uri.parse("file://" + path + sampleName + ".csv");
//                    File sampleFile = new File(uri.getPath());
//                    GMailSender sender = new GMailSender("wpdapp@gmail.com", "technion1234567890");
//                    sender.sendMail(sampleName,
//                            "",
//                            "wpdapp@gmail.com",
//                            "ahofman@rambam.health.gov.il", sampleFile);
//                } catch (Exception e) {
//                    Log.e("SendMail", e.getMessage(), e);
//                }
//                return null;
//            }
//        }.execute(null,null,null);
//        Toast.makeText(context,"The data was sent, thanks!",Toast.LENGTH_SHORT).show();
        firstTimeInPacketAdd = true;


    }

    public void SetStart() {
        //set time
        startTime2 = Long.MIN_VALUE;
        // initilize packet
        mPacket = ByteBuffer.allocate(4 * mSensorNum + mTotSampNum * mSampByteNum);
        SetPosition();
    }


    private void SetPosition() {
        mPosition = new int[mSensorNum];
        mSampCountPos = new int[mSensorNum];
        mSampCount = new int[mSensorNum];
        for (int i = 0; i < mSensorNum; i++) {
            mPosition[i] = 4 * (i + 1) + i * mSensorMaxSamp[i] * mSampByteNum; //first 4, second 4+(SensorTotSampNum*SampByteNum)+4
        }
        if (initPos) {
            for (int i = 0; i < mSensorNum; i++) {
                mSampCountPos[i] = i * (4 + mSensorMaxSamp[i] * mSampByteNum); //first 0, second (4+(SensorTotSampNum*SampByteNum))
            }
        }
    }


    private void showStopButton() {
        (findViewById(R.id.startButton)).setVisibility(View.INVISIBLE);
        (findViewById(R.id.stopButton)).setVisibility(View.VISIBLE);
    }

    private void hideStopButton() {
        (findViewById(R.id.startButton)).setVisibility(View.VISIBLE);
        (findViewById(R.id.stopButton)).setVisibility(View.INVISIBLE);
    }

    private void updateTimer(float time){
        secs = (long) (time / 1000);
        mins = (long) ((time / 1000) / 60);
        hrs = (long) (((time / 1000) / 60) / 60);
            /* Convert the seconds to String * and format to ensure it has * a leading zero when required */
        secs = secs % 60;
        seconds = String.valueOf(secs);
        if (secs == 0) {
            seconds = "00";
        }
        if (secs < 10 && secs > 0) {
            seconds = "0" + seconds;
        }
            /* Convert the minutes to String and format the String */
        mins = mins % 60;
        minutes = String.valueOf(mins);
        if (mins == 0) {
            minutes = "00";
        }
        if (mins < 10 && mins > 0) {
            minutes = "0" + minutes;
        }
        /* Convert the hours to String and format the String */
        hours = String.valueOf(hrs);
        if (hrs == 0) {
            hours = "00";
        }
        if (hrs < 10 && hrs > 0) {
            hours = "0" + hours;
        }
            /* Although we are not using milliseconds on the timer in this example * I included the code in the event that you wanted to include it on your own */
        milliseconds = String.valueOf((long) time);
        if (milliseconds.length() == 2) {
            milliseconds = "0" + milliseconds;
        }
        if (milliseconds.length() <= 1) {
            milliseconds = "00";
        }
        // milliseconds = milliseconds.substring(milliseconds.length()-3, milliseconds.length()-2);
             /* Setting the timer text to the elapsed time */
        ((TextView) findViewById(R.id.counterText)).setText(hours + ":" + minutes + ":" + seconds);
        //  ((TextView)findViewById(R.id.timerMs)).setText("." + milliseconds);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        boolean tosendFlag = false;
        try {
            tosendFlag = PacketAdd(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (tosendFlag) {
            SetPosition();
            Packets.add(message);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public boolean PacketAdd(SensorEvent event) throws IOException {
        int i;
        boolean flag = false;
        // get sensor index
        for (i = 0; i < mSensorNum; i++) {
            if (event.sensor.getType() == mySensor[i]) break;
        }
        mCurSensor = i;
        // get value to put at the Packet buffer
        float axisY = event.values[1];
        float walkingSample = axisY;
        mCurVal = walkingSample;

        // put sample at the Packet buffer and update buffer.
        mPacket.position(mPosition[i]);
        if (true) {
            //mPacket.putLong(event.timestamp);
            if (startTime == Long.MIN_VALUE) {
                startTime = event.timestamp;
                firstTimeDif = startTime;
            }
            mCurTime = (float) (1e-9 * (event.timestamp - startTime));
            if (firstTimeInPacketAdd) {
                timeDiff = mCurTime;
                firstTimeInPacketAdd = false;
            }
            mPacket.putFloat(mCurTime);
            //filewriter.append((char) mCurTime);
        }

        String timeStr = Float.toString(mCurTime - timeDiff);
        filewriter.append(timeStr);
        filewriter.append(',');
        //mPacket.putFloat(mCurVal);
        String value = Float.toString(mCurVal);
        filewriter.append(value);
        filewriter.append('\n');



        mPosition[i] = mPosition[i] + mSampByteNum;
        mSampCount[i]++;

        if (mins>9){
            Button stopButton;

            stopButton = (Button) findViewById(R.id.stopButton);
            stopButton.callOnClick();
        }

        // if the sub‐buffer is full then sending packet
        if (mSampCount[i] == mSensorMaxSamp[i]) {
            for (int j = 0; j < mSensorNum; j++) {
                mPacket.position(mSampCountPos[j]);
                mPacket.putInt(mSampCount[j]);
            }
            message = mPacket.array();
            //allocate new buffer ‐ otherwise data will be override and won't be available for file
            if (D_MULTI_SENSOR_FILE) {
                mPacket = ByteBuffer.allocate(4 * mSensorNum + mTotSampNum * mSampByteNum);
            }
            //SetPosition();
            flag = true;
            return true;
        }
        if (flag) {
            return true;
        } else {
            return false;
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 112) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted  start reading

            } else {
                Toast.makeText(this, "No permission to read external storage.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //FROM THE MONITOR
    public void CreateFile(String filename) {
        String state = Environment.getExternalStorageState();
        if (!(state.equals(Environment.MEDIA_MOUNTED))) {
            Toast.makeText(this, "Media is not mounted", Toast.LENGTH_SHORT).show();
            finish();
        }


        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WPD/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            File file = new File(path, filename);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            //put file tables titles
            filewriter = new FileWriter(file);
            filewriter.append("time[sec]");
            filewriter.append(',');
            filewriter.append("value,");
            filewriter.append('\n');

        } catch (IOException e) {
            //Toast.makeText(activity, e.getMessage() ,Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }
}
