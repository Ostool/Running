package com.example.baibu.running;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//考虑到时间信息手机状态栏本身具备，所以不添加此功能，但是仍保留该类，用作展示
//所在地的天气情况跟温度情况

public class TimeShow extends Fragment {
    private TextView showTime;
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.time_show, container, false);
        showTime = (TextView) view.findViewById(R.id.show_time);

        return view;
    }/* private TextView showTime;
    public static final int GET_INTER_TIME = 0;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case GET_INTER_TIME:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getTime();
                        }
                    }).start();
                    break;
                default:
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.time_show, container, false);
        showTime = (TextView) view.findViewById(R.id.show_time);
        new TimeThread().start();
        return view;
    }
    private class TimeThread extends Thread{
        @Override
        public void run() {
            super.run();
            do {
                try{
                    Thread.sleep(1000);
                    Message message = new Message();
                    message.what = GET_INTER_TIME;
                    handler.sendMessage(message);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }while (true);
        }
    }

    private void getTime(){
        URL url = null;
        try{
            url = new URL("http://www.baidu.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            long ld = connection.getDate();
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(ld);
            final String format = formatter.format(calendar.getTime());

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showTime.setText(format);
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }*/
}
