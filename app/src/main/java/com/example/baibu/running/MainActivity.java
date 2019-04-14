package com.example.baibu.running;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.TransportMode;
import com.example.baibu.running.utils.BitmapUtil;
import com.example.baibu.running.utils.Constants;
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
    private MyAppLication myAppLication;
    private DistanceRequest distanceRequest;
    private ProcessOption processOption;
    private int i = 0;
    private long distanceStartTime = 0;
    private long distanceStopTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initListener();
        gainPermissions();
        setSupportActionBar(toolbar);
        myAppLication.mClient.startTrace(myAppLication.mTrace, onTraceListener);
    }
    /**
     * Handler可以用来更新UI
     * */
    private Handler mHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:

                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private Runnable task = new Runnable() {
        @Override
        public void run() {
            /**
             * 此处执行任务
             * */
            myAppLication.mClient.queryDistance(distanceRequest,onTrackListener);
            distanceStopTime = System.currentTimeMillis()/1000;
            distanceRequest.setEndTime(distanceStopTime);

            Log.d("handler", "run: "+i++);
            mHanlder.sendEmptyMessage(1);
            mHanlder.postDelayed(this, 4 * 1000);//延迟5秒,再次执行task本身,实现了循环的效果
        }
    };







    protected void onResume() {
        super.onResume();
        if (myAppLication.isTraceStarted){
            mHanlder.postDelayed(task, 0);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        mHanlder.removeCallbacks(task);
    }

    private void initListener() {
        onTraceListener = new OnTraceListener() {
            @Override
            public void onBindServiceCallback(int i, String s) {
            }
            @Override
            public void onStartTraceCallback(int i, String s) {
                if (i == 0) {
                    myAppLication.isTraceStarted = true;
                    if (myAppLication.isTraceStarted){
                    myAppLication.mClient.startGather(onTraceListener);}
                    myAppLication.isGatherStarted = true;
                    editor = trackConf.edit();
                    editor.putBoolean("is_gather_started", true);
                    editor.putBoolean("is_trace_started", true);
                    editor.apply();
                    Log.d("123456789", "onStartTraceCallback: " + s.toString());
                }
            }

            @Override
            public void onStopTraceCallback(int i, String s) {
                myAppLication.isGatherStarted = false;
                myAppLication.isTraceStarted = false;
                editor = trackConf.edit();
                editor.remove("is_trace_started");
                editor.remove("is_gather_started");
                editor.apply();
                myAppLication.clearTraceStatus();
                Log.d("123456789", "onStopTraceCallback: " + s.toString());
            }

            @Override
            public void onStartGatherCallback(int i, String s) {
                myAppLication.isGatherStarted = true;
                editor = trackConf.edit();
                editor.putBoolean("is_gather_started", true);
                editor.apply();
                Log.d("123456789", "onStartGatherCallback: " + s.toString());
            }

            @Override
            public void onStopGatherCallback(int i, String s) {
                myAppLication.isGatherStarted = false;
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
            }
            @Override
            public void onLatestPointCallback(LatestPointResponse latestPointResponse) {
                super.onLatestPointCallback(latestPointResponse);
            }
        };

    }


    private void gainPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                } else {
                    Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
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
                break;
            case R.id.start_button:
                pauseButton.setVisibility(View.VISIBLE);
                beginButton.setVisibility(View.GONE);
                editor = myAppLication.trackConf.edit();
                editor.putBoolean("is_track_started", true);
                editor.putBoolean("is_gather_started", true);
                editor.apply();
                distanceStartTime = myAppLication.historyStartTime;
                distanceStopTime = distanceStartTime;
                myAppLication.isTraceStarted = true;
                myAppLication.isGatherStarted = true;
                myAppLication.historyStartTime = System.currentTimeMillis() / 1000;


                mHanlder.postDelayed(task, 0);

                break;
            case R.id.over_button:
                beginButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                overButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.GONE);
                myAppLication.mClient.setOnTraceListener(null);
                myAppLication.mClient.stopTrace(myAppLication.mTrace, onTraceListener);
                myAppLication.historyStopTime = System.currentTimeMillis() / 1000;
                isTraceStart = false;
                myAppLication.isTraceStarted = false;
                myAppLication.isGatherStarted = false;
                editor = myAppLication.trackConf.edit();
                editor.remove("is_trace_started");
                editor.remove("is_gather_started");
                editor.apply();
                mHanlder.removeCallbacks(task);
                break;
            case R.id.continue_button:
                beginButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                overButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.GONE);
                break;
            case R.id.turn_to_map:
                Bundle bundle1 = new Bundle();
                Intent intent = new Intent(this, MapRunShow.class);
                intent.putExtras(bundle1);
                startActivity(intent);
            default:
                break;
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
        myAppLication = (MyAppLication) getApplicationContext();
        trackConf = getSharedPreferences("track_conf", MODE_PRIVATE);
        distanceRequest = new DistanceRequest(Constants.TAG, serviceId, entityName);
        distanceRequest.setProcessed(true);
        processOption = new ProcessOption();
        processOption.setNeedDenoise(true);
        processOption.setNeedVacuate(true);
        processOption.setTransportMode(TransportMode.walking);
        distanceRequest.setProcessOption(processOption);

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
    }
}




