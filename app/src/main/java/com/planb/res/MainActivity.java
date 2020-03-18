package com.planb.res;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.zanfou.webp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityMainBinding.inflate(getLayoutInflater()).getRoot());
    }
}
