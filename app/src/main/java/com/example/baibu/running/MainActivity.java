package com.example.baibu.running;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button beginButton;
    private Button pauseButton;
    private Button overButton;
    private Button continueButton;
    private Button turnToMapButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        pauseButton = (Button) findViewById(R.id.pause_button);
        overButton = (Button) findViewById(R.id.over_button);
        beginButton = (Button)findViewById(R.id.start_button);
        continueButton = (Button)findViewById(R.id.continue_button);
        turnToMapButton = (Button)findViewById(R.id.turn_to_map);
        turnToMapButton.setOnClickListener(this);
        overButton.setOnClickListener(this);
        continueButton.setOnClickListener(this);
        beginButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        setSupportActionBar(toolbar);
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
                

                break;
            case R.id.over_button:
                beginButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                overButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.GONE);
                break;
            case R.id.continue_button:
                beginButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                overButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.GONE);
                break;
            case R.id.turn_to_map:
                Intent intent = new Intent(this,MapRunShow.class);
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
}




