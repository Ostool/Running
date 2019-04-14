package com.example.baibu.running;


import android.content.SharedPreferences;
import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapRunShow extends AppCompatActivity {
    private BaiduMap baiduMap = null;
    private MapView mapView = null;
    private int tag = 1;
    private long serviceId = 210523;
    private String entityName = null;
    private LocRequest locRequest = null;
    private LatestPointRequest latestPointRequest;
    private ProcessOption processOption;
    private List<LatLng> trackPoints;
    private MyAppLication trackApp;
    private OnTrackListener onTrackListener;
    private OnEntityListener onEntityListener;
    private SharedPreferences trackConf;

    private Bundle bundle;
    private HistoryTrackRequest historyTrackRequest;
    private SortType sortType;
    private boolean isProcession;
    private SupplementMode supplementMode;
    private CoordType coordType;
    private boolean isRealTimeRunning;
    private MapUtill mapUtill = null;
    private Timer mTimer;
    private BaiduMap.OnMapLoadedCallback onMapLoadedCallback;
    private HandlerThread handlerThread;
    private Handler childHandler;
    private boolean queryResult = false;
    private Handler mMainHandler = new Handler();

    private Handler uiHandler;
    private static final int MSG_UPDATE_INFO = 0x110;

    /**
     * 实时定位任务
     */
    private RealTimeHandler realTimeHandler = new RealTimeHandler();

    private RealTimeLocRunnable realTimeLocRunnable = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_run_show);
        init();
        initListener();


    }

    //实时定位时刻开启，目的：将实施定位点的图标用在实时轨迹线路上展示方向
    //轨迹线路应增加一个判断，如果轨迹服务没有停止，那么实时轨迹便不会停，那么此时
    //应该把每一次查询的历史轨迹的终点图标隐藏，当停止轨迹服务时再显现出来

    @Override
    protected void onResume() {
        super.onResume();
        if (!trackApp.isTraceStarted){
            startRealTimeLoc(1);
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // (1) 使用handler发送消息
                Message message = new Message();
                message.what = 0;
                mHandler.sendMessage(message);
            }
        }, 0, 1000);//每隔一秒使用handler发送一下消息,也就是每隔一秒执行一次,一直重复执行

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimer.cancel();
        stopRealTimeLoc();
        //当切出地图界面是，保存最新的位置信息
        CommonUtil.saveCurrentLocation(trackApp);
    }

    private void initListener() {

        //轨迹监听器(用于接收纠偏后实时位置回调)
        onTrackListener = new OnTrackListener() {
            @Override
            public void onLatestPointCallback(LatestPointResponse response) {
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    return;
                }
                LatestPoint point = response.getLatestPoint();
                if (null == point || CommonUtil.isZeroPoint(point.getLocation().getLatitude(), point.getLocation()
                        .getLongitude())) {
                    return;
                }

                LatLng currentLatLng = mapUtill.convertTrace2Map(point.getLocation());
                if (null == currentLatLng) {
                    return;
                }
                CurrentLocation.locTime = point.getLocTime();
                CurrentLocation.latitude = currentLatLng.latitude;
                CurrentLocation.longitude = currentLatLng.longitude;
                if (null != mapUtill) {
                    mapUtill.updateStatus(currentLatLng, true);
                }
            }

            //查看历史轨迹并显示出来
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
                super.onHistoryTrackCallback(historyTrackResponse);
                List<TrackPoint> points = historyTrackResponse.getTrackPoints();
                if (points != null) {
                    for (TrackPoint trackPoint : points) {
                        if (!CommonUtil.isZeroPoint(trackPoint.getLocation().getLatitude(),
                                trackPoint.getLocation().getLongitude()))
                            //将轨迹点转化为地图画图层的LatLng类
                            trackPoints.add(mapUtill.convertTrace2Map(trackPoint.getLocation()));
                    }
                }
                mapUtill.drawHistoryTrack(baiduMap, trackPoints, sortType);
            }
        };
        onEntityListener = new OnEntityListener() {

            @Override
            public void onReceiveLocation(TraceLocation location) {

                if (StatusCodes.SUCCESS != location.getStatus() || CommonUtil.isZeroPoint(location.getLatitude(),
                        location.getLongitude())) {
                    return;
                }
                LatLng currentLatLng = mapUtill.convertTraceLocation2Map(location);
                if (null == currentLatLng) {
                    return;
                }
                CurrentLocation.locTime = CommonUtil.toTimeStamp(location.getTime());
                CurrentLocation.latitude = currentLatLng.latitude;
                CurrentLocation.longitude = currentLatLng.longitude;

                if (null != mapUtill) {
                    mapUtill.updateStatus(currentLatLng, true);
                }
            }
        };

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (trackApp.isTraceStarted) {
                    //需要执行的代码
                    trackApp.mClient.queryHistoryTrack(historyTrackRequest, onTrackListener);
                    trackApp.historyStopTime = System.currentTimeMillis() / 1000;
                    historyTrackRequest.setEndTime(trackApp.historyStopTime);
                }
            }
        }
    };


    private void init() {
        trackApp = (MyAppLication) getApplicationContext();
        mapView = (MapView) findViewById(R.id.run_map_show);
        baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setMyLocationEnabled(true);
        entityName = "123";
        trackPoints = new ArrayList<LatLng>();
        mapUtill = new MapUtill();
        mapUtill.init(mapView);
        mTimer = new Timer();


        trackConf = getSharedPreferences("track_conf", MODE_PRIVATE);
        coordType = CoordType.bd09ll;
        supplementMode = SupplementMode.walking;
        sortType = SortType.asc;
        latestPointRequest = new LatestPointRequest(Constants.TAG, serviceId, entityName);
        processOption = new ProcessOption();
        processOption.setNeedVacuate(true);
        processOption.setNeedDenoise(true);
        processOption.setNeedMapMatch(false);
        processOption.setRadiusThreshold(50);
        processOption.setTransportMode(TransportMode.walking);
        latestPointRequest.setProcessOption(processOption);

        bundle = getIntent().getExtras();

        historyTrackRequest = new HistoryTrackRequest(Constants.TAG, serviceId, entityName);
        processOption = new ProcessOption();
        processOption.setRadiusThreshold(50);
        processOption.setTransportMode(TransportMode.walking);
        processOption.setNeedDenoise(true);
        processOption.setNeedVacuate(true);
        historyTrackRequest.setProcessOption(processOption);
        historyTrackRequest.setSortType(SortType.asc);
        historyTrackRequest.setCoordTypeOutput(CoordType.bd09ll);
        historyTrackRequest.setProcessed(true);
        historyTrackRequest.setStartTime(trackApp.historyStartTime);
        historyTrackRequest.setEndTime(System.currentTimeMillis() / 1000);


    }

    static class RealTimeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    /**
     * 实时定位任务
     *
     * @author baidu
     */
    class RealTimeLocRunnable implements Runnable {

        private int interval = 0;

        public RealTimeLocRunnable(int interval) {
            this.interval = interval;
        }

        @Override
        public void run() {
            if (isRealTimeRunning) {
                trackApp.getCurrentLocation(onEntityListener, onTrackListener);
                realTimeHandler.postDelayed(this, interval * 1000);
            }
        }
    }

    public void startRealTimeLoc(int interval) {
        trackApp.isStartRealtimeLoc = true;
        isRealTimeRunning = true;
        realTimeLocRunnable = new RealTimeLocRunnable(interval);
        realTimeHandler.post(realTimeLocRunnable);
    }

    public void stopRealTimeLoc() {
        isRealTimeRunning = false;
        if (null != realTimeHandler && null != realTimeLocRunnable) {
            realTimeHandler.removeCallbacks(realTimeLocRunnable);
        }
        trackApp.mClient.stopRealTimeLoc();
    }
}



