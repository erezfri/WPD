package com.example.erez.walkingpatterndetector;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;

import android.view.View;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //timer
    private Handler mHandler = new Handler();
    private long startTime;
    private long firstTimeDif;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;
    private String hours,minutes,seconds,milliseconds;
    private long secs,mins,hrs;
    private boolean stopped = true;
    private Runnable startTimer;
    private Runnable startSensorDetection;
    private Handler sensorHandler = new Handler();

    //public boolean waitToStart;
    public enum ControlMessage {start};
    public int startMessage=-1;

    //Sensor Experiment info variables
    public int[] mySensor;
    public boolean mTime;
    public int mSensorNum=1;//=mSenorTypeGroup.length;
    private boolean mDefaultSensor;


    //timer
    public MainActivity() {
        startTimer = new Runnable() {
            public void run() {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimer(elapsedTime);
                mHandler.postDelayed(this,REFRESH_RATE);
            }
        };
        startSensorDetection = new Runnable() {
            public void run() {

                SetStart();
                mAcquisitionFlag = true;
                registerSensorListener();
                sensorHandler.postDelayed(this,REFRESH_RATE);
            }
        };

        //Sensor Experiment info variables
        mySensor=new int[]{Sensor.TYPE_ACCELEROMETER};
        mDefaultSensor=false;
        sensorDelay= SensorManager.SENSOR_DELAY_FASTEST;
        //Packet variables
        mTotSampNum=100;
    }

    //sensor
    private ArrayList<Sensor> mSensorGroup = new ArrayList<Sensor>(mSensorNum);
    private boolean mAcquisitionFlag = false;
    private SensorManager mSensorManager;
    public static int sensorDelay=SensorManager.SENSOR_DELAY_GAME;//default value

    //Packet variables
    public int mTotSampNum;
    public int[] mSensorMaxSamp; // if sensors has different freq
    public int mSampByteNum;
    public ByteBuffer mPacket;   // for each sensor sampnum(int)|time(long)| value(int)
    public int[] mSampCount;
    public int[] mSampCountPos; //the position of the counter of the sensor's samples number
    public int[] mPosition;     // the index(dynamic) of the next sample related to a specific sensor
    public int mCurSensor;      //used for fast ploting at multi-sensor
    public float mCurVal,mCurTime; //  for fast ploting at multi-sensor
    private boolean initPos=true; // for once initilization of mSampCountPos
    public byte[] message;
    public long startTime2;

    //VIEW,FILES
    private Activity mActivity=this;
    private ArrayList<byte[]> Packets;

    //FILES
    public ArrayList<File> mFileGroup;
    public ArrayList<FileWriter> mFileWriterGroup;
    private String sampleName = "";

    TextView sensorType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Packets = new ArrayList<byte[]>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setVisibility(View.GONE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Get SensorManager and sensors
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        mSensorGroup = getSensors(mSensorManager);
        sensorType = (TextView)findViewById(R.id.txtSensorType);
        sensorType.setText("Accelerometer Sensor is ready");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    /**
     * getSensors gets the multi-sensor specific sensor.
     * the sensor type is default or Google.
     */
    public ArrayList<Sensor> getSensors(SensorManager sensorManager){
        ArrayList<Sensor> sensorGroup = new ArrayList<Sensor>(mSensorNum);

        //take the default sensors - HW
        if (mDefaultSensor){
            //Get SensorManager and sensors
            for (int i=0;i<mSensorNum;i++){
                sensorGroup.add(sensorManager.getDefaultSensor(mySensor[i]));
            }
        }
        else{
            //take Android open source sensors
            for (int i=0;i<mSensorNum;i++){
                List<Sensor> sensorList = sensorManager.getSensorList(mySensor[i]);
                for (Sensor sensor:sensorList){
                    if(sensor.getVendor().contains("Google Inc.")){
                        sensorGroup.add(sensor);
                        break;
                    }
                }
            }
        }
        if (sensorGroup.size()==0)
        {
            for (int i=0;i<mSensorNum;i++){
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
    private void registerSensorListener(){
        // register to SensorEventListener
        for (int i=0;i<mSensorNum;i++){
            mSensorManager.registerListener(this, mSensorGroup.get(i), sensorDelay);
        }

    }

    public void startClick(View view){
        showStopButton();
        if(stopped){
            startTime = System.currentTimeMillis() - elapsedTime;
        }
        else{ startTime = System.currentTimeMillis();
        }
        mHandler.removeCallbacks(startTimer);
        mHandler.postDelayed(startTimer, 0);
        Calendar c = Calendar.getInstance();
        sampleName =  "sample_" + c.getTime().toString();
        try{
            sensorHandler.removeCallbacks(startSensorDetection);
            sensorHandler.postDelayed(startSensorDetection, 0);



//            sendMessage("START_SENSORNUM=" + mSensorNum + "@");
//            mSensorManager.unregisterListener((SensorEventListener)mActivity);
//            Packets = new ArrayList<byte[]>();
//            SetStart();
//            mAcquisitionFlag = true;
//            registerSensorListener();
//            //write(getControlMessage(ControlMessage.start));

        }
        catch (Exception e){}

    }
    public void SetStart(){
        //set time
        startTime2=Long.MIN_VALUE;
        // initilize packet
        mPacket=ByteBuffer.allocate(4*mSensorNum+mTotSampNum*mSampByteNum);
        //SetPosition();
    }
    private void SetPosition(){
        mPosition=new int[mSensorNum];
        mSampCountPos=new int[mSensorNum];
        mSampCount= new int[mSensorNum];
        for(int i=0;i<mSensorNum;i++){
            mPosition[i]=4*(i+1)+i*mSensorMaxSamp[i]*mSampByteNum; //first 4, second 4+(SensorTotSampNum*SampByteNum)+4
        }
        if (initPos){
            for(int i=0;i<mSensorNum;i++){
                mSampCountPos[i]=i*(4+mSensorMaxSamp[i]*mSampByteNum); //first 0, second (4+(SensorTotSampNum*SampByteNum))
            }
        }
    }
    public void stopClick(View view){
        try{
            //sendMessage("STOP");
            mSensorManager.unregisterListener((SensorEventListener)mActivity);
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WPD/";
            File dir = new File(path);
            if(!dir.exists())
                dir.mkdirs();
            CreateFile(sampleName + ".csv");
            Packets2File(Packets);

        } catch (Exception e){}
        hideStopButton();
        mHandler.removeCallbacks(startTimer);
        sensorHandler.removeCallbacks(startSensorDetection);
        ((TextView) findViewById(R.id.counterText)).setText("00:00:00");
        stopped = false;


    }

    private void showStopButton(){
        (findViewById(R.id.startButton)).setVisibility(View.INVISIBLE);
        (findViewById(R.id.stopButton)).setVisibility(View.VISIBLE);
    }
    private void hideStopButton(){
        (findViewById(R.id.startButton)).setVisibility(View.VISIBLE);
        (findViewById(R.id.stopButton)).setVisibility(View.INVISIBLE);
    }

    private void updateTimer (float time){
        secs = (long)(time/1000);
        mins = (long)((time/1000)/60);
        hrs = (long)(((time/1000)/60)/60);
            /* Convert the seconds to String * and format to ensure it has * a leading zero when required */
        secs = secs % 60;
        seconds=String.valueOf(secs);
        if(secs == 0) {
            seconds = "00";
        }
        if(secs <10 && secs > 0){
            seconds = "0"+seconds;
        }
            /* Convert the minutes to String and format the String */
        mins = mins % 60;
        minutes=String.valueOf(mins);
        if(mins == 0){
            minutes = "00";
        }
        if(mins <10 && mins > 0){
            minutes = "0"+minutes;
        }
            /* Convert the hours to String and format the String */
        hours=String.valueOf(hrs);
        if(hrs == 0){
            hours = "00";
        }
        if(hrs <10 && hrs > 0){
            hours = "0"+hours;
        }
            /* Although we are not using milliseconds on the timer in this example * I included the code in the event that you wanted to include it on your own */
        milliseconds = String.valueOf((long)time);
        if(milliseconds.length()==2){
            milliseconds = "0"+milliseconds;
        }
        if(milliseconds.length()<=1){
            milliseconds = "00";
        }
        // milliseconds = milliseconds.substring(milliseconds.length()-3, milliseconds.length()-2);
             /* Setting the timer text to the elapsed time */
        ((TextView)findViewById(R.id.counterText)).setText(hours + ":" + minutes + ":" + seconds);
        //  ((TextView)findViewById(R.id.timerMs)).setText("." + milliseconds);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Create the message bytes and send via BluetoothChatService
        // Send message and plot at the SAME PHASE
            Packets.add(message);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public boolean PacketAdd(SensorEvent event){
        int i;
        boolean flag = false;
// get sensor index
        for(i=0;i<mSensorNum;i++){
            if(event.sensor.getType()==mySensor[i]) break;
        }
        mCurSensor=i;
// get value to put at the Packet buffer
        float axisY = event.values[1];
        //float omegaMagnitude = (float)Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ); //TODO change to y only!!
        float omegaMagnitude = axisY;

        mCurVal=omegaMagnitude;
        /*if(mModify[i]){
            mCurVal=ModifySensorVal(mSenorTypeGroup[i],mCurVal);
        }*/
// put sample at the Packet buffer and update buffer.
        mPacket.position(mPosition[i]);
        if(mTime) {
//mPacket.putLong(event.timestamp);
            if(startTime==Long.MIN_VALUE){
                startTime=event.timestamp;
                firstTimeDif = startTime;
            }
            mCurTime = (float)(1e-9*(event.timestamp-startTime));
            mPacket.putFloat(mCurTime);

        }
        mPacket.putFloat(mCurVal);
        mPosition[i]=mPosition[i]+mSampByteNum;
        mSampCount[i]++;

// if the sub‐buffer is full then sending packet
        if (mSampCount[i]==mSensorMaxSamp[i]){
            for(int j=0;j<mSensorNum;j++){
                mPacket.position(mSampCountPos[j]);
                mPacket.putInt(mSampCount[j]);
            }
            message=mPacket.array();
//allocate new buffer ‐ otherwise data will be override and won't be available for files
//            if (D_MULTI_SENSOR_FILE)
//                mPacket=ByteBuffer.allocate(4*mSensorNum+mTotSampNum*mSampByteNum);
            //SetPosition();
            flag = true;
            return true;
        }
        if (flag){
            return true;
        }
        else {
            return false;
        }
    }

    public byte[] getControlMessage(ControlMessage messagetype){
        byte[] message = new byte[4];

        switch (messagetype){
            case start:
                message=ByteBuffer.allocate(4).putInt(startMessage).array();
        }
        return message;

    }




    //FROM THE MONITOR

    public void CreateFile(String filename){
        String state = Environment.getExternalStorageState();
        if (!(state.equals(Environment.MEDIA_MOUNTED))) {
            Toast.makeText(this ,"Media is not mounted" ,Toast.LENGTH_SHORT).show();
            finish();
        }
        mFileGroup =new ArrayList<File>();
        mFileWriterGroup= new ArrayList<FileWriter>();

        //create files and it's writers
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WPD";
        try{
            File file = new File(path, filename);
            //mFileGroup.add(file);
            if (file.exists()){
                file.delete();
            }
            file.createNewFile();
            mFileGroup.add(file);
            mFileWriterGroup.add(new FileWriter(file));
            //put file tables titles

            FileWriter filewriter = mFileWriterGroup.get(0);

            filewriter.append("time[sec]");
            filewriter.append(',');
            filewriter.append("value,");
            filewriter.append('\n');

        }
        catch(IOException e)
        {
            //Toast.makeText(activity, e.getMessage() ,Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void Packets2File(ArrayList<byte[]> Packets){
        float startTime = Float.MAX_VALUE;
        for (byte[] p: Packets){
            ByteBuffer Packet = ByteBuffer.wrap(p);//for each packet
            //  at the packet - for each sensor i

            for(int i=0;i<mSensorNum;i++){
                //get appropriate filewriter
                FileWriter filewriter = mFileWriterGroup.get(i);
                //set position to the start of the sensor i message
                Packet.position(mSampCountPos[i]);

                //write to files
                //long x=Packet.getInt();
                long samplesNum=100;
                try{
                    for (long n=0;n<samplesNum;n++){
                        float timeFloat = Packet.getFloat();
                        if (timeFloat < startTime) startTime = timeFloat;
                        timeFloat = timeFloat-startTime;
                        String timeStr = Float.toString(timeFloat);
                        filewriter.append(timeStr);
                        filewriter.append(',');
                        String value = Float.toString(Packet.getFloat());
                        filewriter.append(value);
                        filewriter.append('\n');
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //close files
        for(int i=0;i<mSensorNum;i++){
            FileWriter filewriter = mFileWriterGroup.get(i);
            try{
                filewriter.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // The Handler that gets information back from the BluetoothService
        private final Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case BluetoothService.MESSAGE_STATE_CHANGE:
//                    switch (msg.arg1) {
//                        case BluetoothService.STATE_CONNECTED:
//                            break;
//                        case BluetoothService.STATE_CONNECTING:
//                            break;
//                        case BluetoothService.STATE_LISTEN:
//                        case BluetoothService.STATE_NONE:
//                            break;
//                    }
//                    break;
//                case BluetoothService.MESSAGE_WRITE:
//                    //byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    // String writeMessage = new String(writeBuf);
//                    break;
//                case BluetoothService.MESSAGE_READ:
//
//
//                    byte[] readBuf = (byte[]) msg.obj;
//                    String msgString = new String(readBuf);
//                    if (msgString.startsWith("START"))
//                    {
//                        recordingStatus = true;
//                        TextView t = (TextView)findViewById(R.id.recordingStatus);
//                        handleStartStop();
//                        t.setVisibility(View.VISIBLE);//RECORDING...
//                        Toast.makeText(getApplicationContext(),"The video started", Toast.LENGTH_SHORT).show();
//                        int indexStart = msgString.indexOf("=") + 1;
//                        int indexEnd = msgString.indexOf("@");
//                        mSensorNum = Integer.parseInt(msgString.substring(indexStart, indexEnd));
//                        mSampCountPos=new int[mSensorNum];
//                        mSampCountPosFlag = false;
//                        for (int i=0;i<mSensorNum;i++){if (mGraphControlInd[i]!=-1) mGraphControlNum++;}
//                        Plot();
//
//                    }
//                    else if (msgString.startsWith("STOP")){
//                        recordingStatus = false;
//                        mGraphControlNum = 0;
//                        TextView t = (TextView)findViewById(R.id.recordingStatus);
//                        handleStartStop();
//                        t.setVisibility(View.INVISIBLE);
//                        Toast.makeText(getApplicationContext(),"The video stopped, please wait", Toast.LENGTH_SHORT).show();
//                        graphPreview = (LinearLayout) findViewById(R.id.graph_preview);
//                        graphPreview.setVisibility(View.INVISIBLE);
//
//
//                        //file:
//                        CreateFile(sampleName + ".csv");
//                        Packets2File(Packets);
//                        System.gc();
//
//                        //make frames
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run()
//                            {
//                                movieToFrames();
//                                System.gc();
//
//                            }
//                        }).start();
//                        Toast.makeText(getApplicationContext(),"Done", Toast.LENGTH_SHORT).show();
//                    }
//                    else if (msgString.startsWith("SampCountPos") && recordingStatus) {
//
//                        int indexStartI = msgString.indexOf("[") + 1;
//                        int indexEndI = msgString.indexOf("]");
//                        int indexStartVal = msgString.indexOf("=") + 1;
//                        int indexEndVal = msgString.indexOf("@");
//                        mSampCountPos[Integer.parseInt(msgString.substring(indexStartI, indexEndI))] =
//                                Integer.parseInt(msgString.substring(indexStartVal, indexEndVal));
//                        mSampCountPosFlag = true;
//                    }
//                    else if (recordingStatus && mSampCountPosFlag){   //packets
//                        Packets.add(readBuf.clone());
//                        GraphAddData(readBuf, mGraph);
//                        mGraph.invalidate();
//                    }
//
//                    break;
//                case BluetoothService.MESSAGE_DEVICE_NAME:
//                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
//                    Toast.makeText(getApplicationContext(), "Connected to "
//                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                    break;
//                case BluetoothService.MESSAGE_TOAST:
//                    Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST),
//                            Toast.LENGTH_SHORT).show();
//                    break;
//            }
        }
    };
}
