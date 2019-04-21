package com.example.baibu.running;


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;

import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.api.entity.LocRequest;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.LatestPoint;
import com.baidu.trace.api.track.LatestPointRequest;
import com.baidu.trace.api.track.LatestPointResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.SupplementMode;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.SortType;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TraceLocation;
import com.baidu.trace.model.TransportMode;
import com.example.baibu.running.utils.BitmapUtil;
import com.example.baibu.running.utils.CommonUtil;
import com.example.baibu.running.utils.Constants;
import com.example.baibu.running.utils.MapUtill;
import com.example.baibu.running.utils.NetUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MapRunShow extends AppCompatActivity  implements View.OnClickListener{
    private BaiduMap baiduMap;
    private MapView mapView;
    private Button snapshot;
    private Timer timer;
    private boolean isRealTimeRunning = false;
    private boolean isRealtimeHistoryTrack = false;
    private List<LatLng> trackPoints;
    private SortType sortType;
    private File fileDir;
    private boolean isCanWrite = false;
    /**
     * 实时定位任务
     */
    private RealTimeHandler realTimeHandler = new RealTimeHandler();
    private RealtimeTrackHandle realtimeTrackHandle = new RealtimeTrackHandle();

    private RealTimeLocRunnable realTimeLocRunnable = null;
    private RealtimeHistoryTrack realtimeHistoryTrackRunnable = null;

    //轨迹监听器
    private OnTrackListener onTrackListener;

    //实体监听器
    private OnEntityListener onEntityListener;

    //最新点请求
    private LatestPointRequest latestPointRequest;

    //历史轨迹请求
    private HistoryTrackRequest historyTrackRequest = null;

    //地图工具类
    private MapUtill mapUtill;

    //应用全局声明
    private MyAppLication myApp;

    //参数类声明
    private Constants constants;

    //纠偏选项
    private ProcessOption processOption;

    //在初始化地图界面时，即打开地图界面的时候进行判断
    //如果此时没有进入查询实时轨迹模式，那么就进行实时定位
    //如果进入，则进行实时轨迹查询展现
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_run_show);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
            Log.i("permission", "onCreate: Requesting permission");
        }else {
            isCanWrite = true;
            Log.i("permission", "onCreate: permission granted");
        }

        init();
        initRequest();
        initListener();
        //实时定位,两秒一次
       if (!myApp.pressOverButton){
           if (!myApp.isTraceStarted){
           startRealTimeLoc(5);
           }else{
               mapUtill.setCenter(myApp);
               startRealtimeHistoryTrack(5);
           }
       }
        //此处呈现完整的运动轨迹路线
        if (myApp.pressOverButton){
            historyTrackRequest.setStartTime(myApp.historyStartTime);
            historyTrackRequest.setEndTime(myApp.historyStopTime);
            myApp.mClient.queryHistoryTrack(historyTrackRequest,onTrackListener);
        }
    }
    protected void onResume(){
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1001:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i("permission", "onRequestPermissionsResult: successful");
                    isCanWrite = true;
                }else {
                    isCanWrite = false;
                    Log.i("permission", "onRequestPermissionsResult: failed");
                }
                break;
        }
    }

    private void init(){
        mapView = (MapView)findViewById(R.id.run_map_show);
        baiduMap = mapView.getMap();
        timer = new Timer();
        trackPoints = new ArrayList<LatLng>();
        sortType = SortType.asc;
        myApp = (MyAppLication)getApplicationContext();
        mapUtill = new MapUtill();
        mapUtill.init(mapView);
        snapshot = (Button)findViewById(R.id.snapsshot);
        snapshot.setOnClickListener(this);
        if (isCanWrite){
            fileDir = new File(Environment.getExternalStorageDirectory().getPath());
        }else {
            fileDir = new File(this.getCacheDir().getAbsolutePath() );
            if (!fileDir.exists()) {
                fileDir.mkdir();
            }
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.snapsshot:
                // 截图，在SnapshotReadyCallback中保存图片到 sd 卡
                baiduMap.snapshot(new BaiduMap.SnapshotReadyCallback() {
                    public void onSnapshotReady(Bitmap snapshot) {
                        SimpleDateFormat time = new SimpleDateFormat("yyyyMMddHHmmss");
                        String fileName = time.format(System.currentTimeMillis());
                        File file = new File(fileDir, fileName + ".png");
                        FileOutputStream out;
                        try {
                            out = new FileOutputStream(file);
                            if (snapshot.compress(
                                    Bitmap.CompressFormat.PNG, 100, out)) {
                                out.flush();
                                out.close();
                            }
                            Toast.makeText(MapRunShow.this, "屏幕截图成功，图片存在: " + file.toString(),
                                    Toast.LENGTH_SHORT).show();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Toast.makeText(this, "正在截取屏幕图片...",
                        Toast.LENGTH_SHORT).show();
        }
    }

    private void initRequest(){
        //此处进行纠偏初始化设置，内容：
        //方式：步行；抽稀，去噪，去除精度低的点，绑路
        historyTrackRequest = new HistoryTrackRequest(Constants.TAG,myApp.serviceId,myApp.entityName);
        processOption = new ProcessOption();
        processOption.setTransportMode(TransportMode.walking);
        processOption.setNeedVacuate(true);
        processOption.setRadiusThreshold(50);
        processOption.setNeedDenoise(true);
        processOption.setNeedMapMatch(true);
        historyTrackRequest.setProcessed(true);
        historyTrackRequest.setProcessOption(processOption);
        historyTrackRequest.setStartTime(myApp.historyStartTime);
        historyTrackRequest.setEndTime(myApp.historyStopTime);
    }
    private void initListener() {
        //轨迹监听器(用于接收纠偏后实时位置回调)
        onTrackListener = new OnTrackListener() {
            //此处为实时纠偏定位
            @Override
            public void onLatestPointCallback(LatestPointResponse response) {
                if (StatusCodes.SUCCESS!=response.getStatus()){
                    Log.d("error", "onReceiveLocation: "+response.getStatus());
                    return;
                }
                LatestPoint latestPoint = response.getLatestPoint();
                Log.d("error", "onReceiveLocation: "+response.getStatus());
                if (null==latestPoint||CommonUtil.isZeroPoint(latestPoint.getLocation().getLatitude(),
                        latestPoint.getLocation().getLongitude())){
                    return;
                }
                LatLng currentLatlng = mapUtill.convertTrace2Map(latestPoint.getLocation());
                CurrentLocation.locTime = latestPoint.getLocTime();
                CurrentLocation.latitude = currentLatlng.latitude;
                CurrentLocation.longitude = currentLatlng.longitude;
                if (mapUtill!=null){
                    mapUtill.updateStatus(currentLatlng,true);
                }
            }

            //查看历史轨迹并显示出来
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
                super.onHistoryTrackCallback(historyTrackResponse);

                int total = historyTrackResponse.getTotal();
                if (StatusCodes.SUCCESS!=historyTrackResponse.getStatus()){
                    Toast.makeText(MapRunShow.this,"轨迹查询失败",
                            Toast.LENGTH_SHORT).show();
                }else if (0==total){
                    Toast.makeText(MapRunShow.this,"没有查询到轨迹数据",
                            Toast.LENGTH_SHORT).show();
                }else {
                    List<TrackPoint> points = historyTrackResponse.getTrackPoints();
                    if (points!=null){
                        for (TrackPoint trackPoint:points){
                            if (!CommonUtil.isZeroPoint(trackPoint.getLocation().getLatitude(),
                                    trackPoint.getLocation().getLongitude())){
                                    trackPoints.add(MapUtill.convertTrace2Map(trackPoint.getLocation()));
                            }
                        }
                    }
                    mapUtill.drawHistoryTrack( trackPoints,sortType,myApp.isTraceStarted,myApp);
                }

            }
        };
        onEntityListener = new OnEntityListener() {
            //此处为实时定位
            @Override
            public void onReceiveLocation(TraceLocation location) {
                if (StatusCodes.SUCCESS!=location.getStatus()||CommonUtil.isZeroPoint(location.getLatitude(),
                        location.getLongitude())){
                    return;
                }
                LatLng currentLatlng = mapUtill.convertTraceLocation2Map(location);
                Log.d("error", "onReceiveLocation: "+currentLatlng.latitude);
                if (currentLatlng==null){
                    return;
                }
                CurrentLocation.locTime = CommonUtil.toTimeStamp(location.getTime());
                CurrentLocation.latitude = currentLatlng.latitude;
                CurrentLocation.longitude = currentLatlng.longitude;
                if (mapUtill!=null){
                    mapUtill.updateStatus(currentLatlng,true);
                }
            }
        };
    }
    /**
     * 实时轨迹任务
     */
    class RealtimeHistoryTrack implements Runnable{
        int interval = 0;
        public RealtimeHistoryTrack(int interval){
            this.interval = interval;
        }
        @Override
        public void run() {
            if (isRealtimeHistoryTrack){
                myApp.mClient.queryHistoryTrack(historyTrackRequest,onTrackListener);
                long startTime = myApp.historyStopTime;
                long stop = System.currentTimeMillis()/1000;
                historyTrackRequest.setStartTime(startTime);
                historyTrackRequest.setEndTime(stop);
                realtimeTrackHandle.postDelayed(this,interval*1000);
            }
        }
    }

    public void startRealtimeHistoryTrack(int interval){
        isRealtimeHistoryTrack = true;
        realtimeHistoryTrackRunnable = new RealtimeHistoryTrack(interval);
        realtimeTrackHandle.post(realtimeHistoryTrackRunnable);
    }

    public void stopRealTimeHistoryTrack() {
        isRealtimeHistoryTrack = false;
        if (null != realtimeTrackHandle && null != realtimeHistoryTrackRunnable) {
            realtimeTrackHandle.removeCallbacks(realtimeHistoryTrackRunnable);
        }
    }




    /**
     * 实时定位任务
     */
    class RealTimeLocRunnable implements Runnable {

        private int interval = 0;

        public RealTimeLocRunnable(int interval) {
            this.interval = interval;
        }

        @Override
        public void run() {
            if (isRealTimeRunning) {
                myApp.getCurrentLocation(onEntityListener, onTrackListener);
                realTimeHandler.postDelayed(this, interval * 1000);
            }
        }
    }

    public void startRealTimeLoc(int interval) {
        isRealTimeRunning = true;
        realTimeLocRunnable = new RealTimeLocRunnable(interval);
        realTimeHandler.post(realTimeLocRunnable);
    }

    public void stopRealTimeLoc() {
        isRealTimeRunning = false;
        if (null != realTimeHandler && null != realTimeLocRunnable) {
            realTimeHandler.removeCallbacks(realTimeLocRunnable);
        }
        myApp.mClient.stopRealTimeLoc();
    }

    static class RealTimeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
    static class RealtimeTrackHandle extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRealTimeLoc();
        stopRealTimeHistoryTrack();
    }
}



