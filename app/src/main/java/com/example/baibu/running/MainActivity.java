package com.example.baibu.running;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.api.entity.LocRequest;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.track.DistanceRequest;
import com.baidu.trace.api.track.DistanceResponse;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.LatestPoint;
import com.baidu.trace.api.track.LatestPointResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.SupplementMode;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TransportMode;
import com.example.baibu.running.utils.BitmapUtil;
import com.example.baibu.running.utils.Constants;
import com.example.baibu.running.utils.PermissionUtils;
import com.example.baibu.running.utils.QueryRealtimeDistance;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button beginButton;
    private Button pauseButton;
    private Button overButton;
    private Button continueButton;
    private Button turnToMapButton;
    private TextView distanceKilomiter;
    private TextView speed;
    public BaiduMap baiduMap;
    public MapView mapView;
    private Toolbar toolbar;
    private long serviceId = 210523;
    private String entityName = "123";
    private List<String> permissionList;
    private OnTrackListener onTrackListener;
    private OnTraceListener onTraceListener;
    private SharedPreferences trackConf;
    private SharedPreferences.Editor editor;
    private boolean isTraceStart;
    private MyAppLication myApp;
    private DistanceRequest distanceRequest;
    private ProcessOption processOption;
    private int i = 0;
    private long distanceStartTime = 0;
    private long distanceStopTime = 0;
    private boolean isShowDistance = false;
    private RealtimeDistanceHandler realtimeDistanceHandler = new RealtimeDistanceHandler();
    private RealtimeDistance realtimeDistanceRunnable ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setSupportActionBar(toolbar);
        gainPermissions();
        initListener();
        myApp.mClient.startTrace(myApp.mTrace,onTraceListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRealtimeDistance();
    }

    private void initListener() {
        onTraceListener = new OnTraceListener() {
            @Override
            public void onBindServiceCallback(int i, String s) {
            }
            @Override
            public void onStartTraceCallback(int i, String s) {
                if (i == 0) {
                    editor = trackConf.edit();
                    editor.putBoolean("is_gather_started", true);
                    editor.putBoolean("is_trace_started", true);
                    editor.apply();
                    Log.d("123456789", "onStartTraceCallback: " + s.toString());
                    myApp.mClient.startGather(onTraceListener);
                    myApp.isGatherStarted = true;
                }
            }

            @Override
            public void onStopTraceCallback(int i, String s) {
                myApp.isGatherStarted = false;
                myApp.isTraceStarted = false;
                editor = trackConf.edit();
                editor.remove("is_trace_started");
                editor.remove("is_gather_started");
                editor.apply();
                myApp.clearTraceStatus();
                Log.d("123456789", "onStopTraceCallback: " + s.toString());
            }

            @Override
            public void onStartGatherCallback(int i, String s) {
                myApp.isGatherStarted = true;
                editor = trackConf.edit();
                editor.putBoolean("is_gather_started", true);
                editor.apply();
                Log.d("123456789", "onStartGatherCallback: " + s.toString());
            }

            @Override
            public void onStopGatherCallback(int i, String s) {
                myApp.isGatherStarted = false;
                editor = trackConf.edit();
                editor.remove("is_gather_started");
                editor.apply();
                Log.d("123456789", "onStopGatherCallback: " + s.toString());
            }

            @Override
            public void onPushCallback(byte b, PushMessage pushMessage) {
            }

            @Override
            public void onInitBOSCallback(int i, String s) {
            }
        };
        onTrackListener = new OnTrackListener() {
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
                super.onHistoryTrackCallback(historyTrackResponse);
            }
            @Override
            public void onDistanceCallback(DistanceResponse distanceResponse) {
                super.onDistanceCallback(distanceResponse);
                Log.d("historydistance", "onDistanceCallback: "+distanceResponse.getMessage());
                if (StatusCodes.SUCCESS!=distanceResponse.getStatus()){
                    return;
                }
                double distance = distanceResponse.getDistance();
                double distanceSpeed = distance/(distanceRequest.getEndTime()-distanceRequest.getStartTime());
                distanceKilomiter.setText(String.valueOf(distance));
                speed.setText(String.valueOf(distanceSpeed));
            }
            @Override
            public void onLatestPointCallback(LatestPointResponse latestPointResponse) {
                super.onLatestPointCallback(latestPointResponse);
            }
        };

    }
    private void gainPermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.READ_PHONE_STATE},3);
            Log.d("abc", "gainPermissions: "+1234);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
            Log.d("abc", "gainPermissions: "+123);
        }




    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }

            case 2:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                }else {
                    Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case 3:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                }else {
                    Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                }
            default:

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pause_button:
                pauseButton.setVisibility(View.GONE);
                beginButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.VISIBLE);
                overButton.setVisibility(View.VISIBLE);
                myApp.mClient.stopGather(onTraceListener);
                stopRealtimeDistance();
                break;
            case R.id.start_button:
                pauseButton.setVisibility(View.VISIBLE);
                beginButton.setVisibility(View.GONE);
                editor = myApp.trackConf.edit();
                editor.putBoolean("is_track_started", true);
                editor.putBoolean("is_gather_started", true);
                editor.apply();
                myApp.pressOverButton = false;
                myApp.isTraceStarted = true;
                //此处仅开启轨迹服务，但点击开始按钮后才会开始轨迹采集
                myApp.distanceStartTime = System.currentTimeMillis()/1000;
                startRealtimeDistance(2);
                myApp.pressOverButton = false;

                break;
            case R.id.over_button:
                beginButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                overButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.GONE);
                myApp.mClient.setOnTraceListener(null);
                myApp.mClient.stopGather(onTraceListener);
                isTraceStart = false;
                myApp.isTraceStarted = false;
                myApp.isGatherStarted = false;
                editor = myApp.trackConf.edit();
                editor.remove("is_trace_started");
                editor.remove("is_gather_started");
                editor.apply();
                myApp.isFirstShow = true;
                myApp.pressOverButton = true;
                myApp.historyStopTime = System.currentTimeMillis()/1000;
                stopRealtimeDistance();
                break;
            case R.id.continue_button:
                beginButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                overButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.GONE);
                //重新开始轨迹采集
                myApp.mClient.startGather(onTraceListener);
                startRealtimeDistance(2);
                break;
            case R.id.turn_to_map:
                Intent intent = new Intent(this, MapRunShow.class);
                //如果此时已按了结束按钮，那么停止时间应该是停止按钮记录的时间
                if (!myApp.pressOverButton){
                myApp.historyStopTime = System.currentTimeMillis()/1000;
                }
                stopRealtimeDistance();
                startActivity(intent);
            default:
                break;
        }
    }

    class RealtimeDistance implements Runnable{
        int interval = 0;
        public RealtimeDistance(int interval){
            this.interval = interval;
        }
        @Override
        public void run() {
            if (isShowDistance){
                myApp.getRealtimeDistance(distanceRequest,onTrackListener);
                Log.d("historydistance", "startRealtimeDistance: "+isShowDistance);
                realtimeDistanceHandler.postDelayed(this,interval*1000);
            }
        }
    }

    public void startRealtimeDistance(int interval){
        isShowDistance = true;
        realtimeDistanceRunnable = new RealtimeDistance(interval);
        realtimeDistanceHandler.post(realtimeDistanceRunnable);
    }

    public void stopRealtimeDistance(){
        isShowDistance = false;
        if (realtimeDistanceHandler!=null&&realtimeDistanceRunnable!=null){
            realtimeDistanceHandler.removeCallbacks(realtimeDistanceRunnable);
        }
    }

    static class RealtimeDistanceHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.toolbar, menu);
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
    private void init() {
        BitmapUtil.init();
        myApp = (MyAppLication) getApplicationContext();
        myApp.getScreenSize();
        trackConf = getSharedPreferences("track_conf", MODE_PRIVATE);
        distanceRequest = new DistanceRequest(Constants.TAG, serviceId, entityName);
        distanceRequest.setProcessed(true);
        processOption = new ProcessOption();
        processOption.setNeedDenoise(true);
        processOption.setNeedVacuate(true);
        processOption.setTransportMode(TransportMode.walking);
        //距离请求参数设置
        distanceRequest.setProcessOption(processOption);

        distanceRequest.setSupplementMode(SupplementMode.no_supplement);
        LayoutInflater factorys = LayoutInflater.from(MainActivity.this);
        View view = factorys.inflate(R.layout.map_run_show, null);
        mapView = (MapView) view.findViewById(R.id.run_map_show);
        baiduMap = mapView.getMap();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        pauseButton = (Button) findViewById(R.id.pause_button);
        overButton = (Button) findViewById(R.id.over_button);
        beginButton = (Button) findViewById(R.id.start_button);
        continueButton = (Button) findViewById(R.id.continue_button);
        turnToMapButton = (Button) findViewById(R.id.turn_to_map);
        turnToMapButton.setOnClickListener(this);
        overButton.setOnClickListener(this);
        continueButton.setOnClickListener(this);
        beginButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        permissionList = new ArrayList<>();
        distanceKilomiter = (TextView)findViewById(R.id.textView2);
        speed = (TextView)findViewById(R.id.textView5);
    }
}