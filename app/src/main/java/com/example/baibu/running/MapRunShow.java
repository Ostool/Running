package com.example.baibu.running;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuWrapperFactory;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.api.entity.LocRequest;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.model.LocationMode;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.TraceLocation;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;

public class MapRunShow extends AppCompatActivity {
    private BaiduMap baiduMap = null;
    private MapView mapView = null;
    private com.baidu.mapapi.model.LatLng ll;
    private MyLocationConfiguration configuration;
    private MyLocationConfiguration.LocationMode mode;
    private BitmapDescriptor bitmap;
    private boolean enableDiretion;
    private int accuracyCircleFillColor;
    private int accuracyCircleStrokeColor;
    private MyLocationListener myListener;
    //地图截屏回调接口


    private Button startButton;
    private Button stopButton;
    private Button turnToMap;
    private static BitmapDescriptor realTimeBitmap = null;
    private boolean isFirstLoc = true;
    private boolean isFirstTra = true;
    private boolean isFirstTurn =true;

    private int tag = 1;
    private Trace trace;
    private RefreshThread refreshThread;
    private OnTraceListener onTraceListener;
    private long serviceId = 210523;
    private int gatherInterval = 1;
    private int packInterval = 1 ;
    private String entityName = null;
    private boolean isNeedStorage = false;
    private LBSTraceClient traceClient;
    private LocationClient locClient;
    private List<LatLng> pointList = null;
    private OnEntityListener onEntityListener;
    private static OverlayOptions overlay;//起点图标
    private static PolylineOptions polyline;
    private LocRequest locRequest = null;
    private List<String> permissionList;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(MyAppLication.getContext());
        setContentView(R.layout.map_run_show);
        permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MapRunShow.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MapRunShow.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MapRunShow.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MapRunShow.this, permissions, 1);
        }
        init();
        initOnEntityListener();
        initOnStartTraceListener();

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

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation == null || baiduMap == null) {
                return;
            }
            MyLocationData locationData = new MyLocationData.Builder()
                    .latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude())

                    .speed(bdLocation.getSpeed())
                    .direction(bdLocation.getDirection())
                    .build();
            baiduMap.setMyLocationData(locationData);
            if (isFirstLoc) {
                isFirstLoc = false;
                ll = new LatLng(bdLocation.getLatitude(),
                        bdLocation.getLongitude());
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(ll, 18);
                baiduMap.animateMapStatus(update);
            }
        }
    };


    private void init() {
        mapView = (MapView) findViewById(R.id.run_map_show);
        baiduMap = mapView.getMap();
        myListener = new MyLocationListener();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setMyLocationEnabled(true);
        realTimeBitmap = BitmapDescriptorFactory.fromResource(R.drawable.dog_home);
        LayoutInflater factory = LayoutInflater.from(MapRunShow.this);
        View layout = factory.inflate(R.layout.activity_main, null);
        //记录轨迹开始按钮
        startButton = (Button) layout.findViewById(R.id.start_button);
        //结束轨迹记录按钮
        stopButton = (Button) layout.findViewById(R.id.over_button);
        turnToMap = (Button)layout.findViewById(R.id.turn_to_map);

        //设置定位点的状态
        mode = MyLocationConfiguration.LocationMode.FOLLOWING;
        bitmap = null;
        enableDiretion = true;
        accuracyCircleFillColor = 0xAAFFFF88;
        accuracyCircleStrokeColor = 0xAA00FF00;
        configuration = new MyLocationConfiguration(mode,enableDiretion,bitmap,
                accuracyCircleFillColor,accuracyCircleStrokeColor);
        baiduMap.setMyLocationConfiguration(configuration);
        //实例化LocationClient
        locClient = new LocationClient(MyAppLication.getContext());
        //注册监听函数
        locClient.registerLocationListener(myListener);
        //设置定位Option
        this.setLocationOption();



        //实体名字
        entityName = "123";
        //实例化轨迹客服端
        traceClient = new LBSTraceClient(getApplicationContext());
        //实例化轨迹服务
        trace = new Trace(serviceId, entityName, isNeedStorage);
        //设置位置采集和打包周期
        traceClient.setInterval(gatherInterval, packInterval);
        //设置轨迹定位方式
        traceClient.setLocationMode(LocationMode.High_Accuracy);
        traceClient.setOnTraceListener(onTraceListener);

        /*realTimeBitmap = BitmapDescriptorFactory.fromResource(R.drawable.dog_home);*/
    }



    //初始化轨迹监听器
    private void initOnStartTraceListener() {


        onTraceListener = new OnTraceListener() {
            @Override
            public void onBindServiceCallback(int i, String s) {

            }

            @Override
            public void onStartTraceCallback(int i, String s) {

            }

            @Override
            public void onStopTraceCallback(int i, String s) {

            }

            @Override
            public void onStartGatherCallback(int i, String s) {

            }

            @Override
            public void onStopGatherCallback(int i, String s) {

            }

            @Override
            public void onPushCallback(byte b, PushMessage pushMessage) {

            }

            @Override
            public void onInitBOSCallback(int i, String s) {

            }
        };
    }

    /*//地图截图回调接口
    private BaiduMap.SnapshotReadyCallback callback = new BaiduMap.SnapshotReadyCallback() {
        @Override
        public void onSnapshotReady(Bitmap bitmap) {

        }
    };*/


    //初始化实体监听器
    private void initOnEntityListener() {


        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFirstTra) {
                    //开启轨迹服务
                    traceClient.startTrace(trace, null);
                    startRefreshThread(true);
                    baiduMap.clear();
                    traceClient.startGather(null);
                    isFirstTra = false;

                }
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFirstTra) {
                    isFirstTra = true;
                    pointList.clear();
                    baiduMap.clear();
                    traceClient.stopGather(null);
                    traceClient.stopTrace(trace,null);
                    startRefreshThread(false);
                }
            }
        });

        /**
         * 实体监听器  在这里监听设备的运动轨迹，并把合适的轨迹点保存下来，并
         * 设定开启轨迹后的第一个轨迹点的图标，然后画出轨迹的实时路线
         */
        onEntityListener = new OnEntityListener() {

            @Override
            public void onReceiveLocation(TraceLocation traceLocation) {
                super.onReceiveLocation(traceLocation);
                LatLng point = new LatLng(
                        traceLocation.getLatitude(), traceLocation.getLongitude());
                if (pointList.size() == 0) {
                    overlay = new MarkerOptions().position(point).icon(realTimeBitmap)
                            .zIndex(9).draggable(true);
                    baiduMap.addOverlay(overlay);
                    pointList.add(point);
                } else {
                    LatLng last = pointList.get(pointList.size() - 1);
                    double distance = getDistance(point, last);
                    if (distance < 80 && distance > 0) {
                        pointList.add(point);
                        //将这里的每两个点画一次线改为直接将保存下来的坐标集合全部画出来
                        drawRealtimePoint(pointList);
                    }
                }

            }
        };
    }


    /**
     * 计算两点之间的距离
     */
    public static double getDistance(LatLng point1, LatLng point2) {
        double lat1 = point1.latitude * 100000;
        double lng1 = point1.longitude * 100000;
        double lat2 = point2.latitude * 100000;
        double lng2 = point2.longitude * 100000;
        return sqrt((lat1 - lat2) * (lat1 - lat2) + (lng1 - lng2) * (lng1 - lng2));
    }

    private class RefreshThread extends Thread {

        protected boolean refresh = true;

        public void run() {

            while (refresh) {
                //用来记录实时轨迹点
                queryRealtimeTrack();
                System.out.println("线程更新" + pointList.size());
                try {
                    Thread.sleep(packInterval * 1000);
                } catch (InterruptedException e) {
                    System.out.println("线程休眠失败");
                }
            }

        }
    }

    /**
     * 查询实时线路    这个有什么作用？ 目前的理解：配合刷新进程，获取实时轨迹点
     * 获取到的点会回馈到实体监听器供使用
     */
    private void queryRealtimeTrack() {
        locRequest = new LocRequest(tag, serviceId);
        traceClient.queryRealTimeLoc(locRequest, onEntityListener);
    }

    /**
     * 启动刷新线程
     *
     * @param isStart
     */
    private void startRefreshThread(boolean isStart) {

        if (refreshThread == null) {
            refreshThread = new RefreshThread();
        }

        refreshThread.refresh = isStart;

        if (isStart) {
            if (!refreshThread.isAlive()) {
                refreshThread.start();
            }
        } else {
            refreshThread = null;
        }

    }

    /**
     *  获取手机识别码
     */
    private String getImei(Context context) {
        String mImei = "NULL";
        try {

            mImei = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        } catch (SecurityException e) {
            mImei = "NULL";
        }
        return mImei;
    }

    /**
     * 设置定位选项
     */
    private void setLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);  //打开GPS
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); //设置定位模式
        option.setCoorType("bd09ll"); //返回的定位结果是百度经纬度默认值gcj02
        //option.setScanSpan(2000);  //设置发起定位请求的间隔时间为2000ms
        option.setOpenAutoNotifyMode(); //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化
        // 就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
        option.setIsNeedAddress(true);  //返回的定位结果包含地址信息
        option.setNeedDeviceDirect(true);  //返回的定位结果包含手机机头的方向
        locClient.setLocOption(option);
    }



    /**
     * 画出实时线路点
     *
     * @param pointList
     */
    private void drawRealtimePoint(List<LatLng> pointList) {

/*//           每次画两个点
        List<LatLng> latLngs = new ArrayList<LatLng>();
        latLngs.add(last);
        latLngs.add(point);
        polyline = new PolylineOptions().width(10).color(Color.BLUE).points(latLngs);
        baiduMap.addOverlay(polyline);*/
        if (pointList.size() >= 2 && pointList.size() < 10000) {
            OverlayOptions ooPolyline = new PolylineOptions().width(10)
                    .color(ContextCompat.getColor(this, R.color.map_line)).points(pointList);
            baiduMap.addOverlay(ooPolyline);


        }

    }
}
