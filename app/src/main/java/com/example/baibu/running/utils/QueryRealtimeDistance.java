package com.example.baibu.running.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.baidu.trace.api.track.DistanceRequest;
import com.baidu.trace.api.track.OnTrackListener;
import com.example.baibu.running.MyAppLication;



public class QueryRealtimeDistance extends Thread {
    private MyAppLication myAppLication;
    private DistanceRequest distanceRequest;
    private OnTrackListener onTrackListener;
    private Handler handler;
    private Message message;
    public Thread thread;
    private int i = 0;



    public QueryRealtimeDistance(final MyAppLication myAppLication,
                                 final DistanceRequest distanceRequest, final Handler handler,
                                 final OnTrackListener onTrackListener){
        this.myAppLication = myAppLication;
        this.distanceRequest = distanceRequest;
        this.onTrackListener = onTrackListener;
        this.handler = handler;
    }
            @Override
            public void run() {
            synchronized (this){
                while (true) {
                    myAppLication.mClient.queryDistance(distanceRequest, onTrackListener);
                    message = new Message();
                    message.what = Constants.UPDATE_DISTANCE;
                    handler.sendMessage(message);
                    i = i +1;
                    Log.d("distance", "run: "+i);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
        }
    }

    /*public void run() {
        super.run();
        while (myAppLication.isTraceStarted){
            myAppLication.mClient.queryDistance(distanceRequest,onTrackListener);
            message = new Message();
            message.what = Constants.UPDATE_DISTANCE;
            handler.sendMessage(message);

        }
    }*/
}
