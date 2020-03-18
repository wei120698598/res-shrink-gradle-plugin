package com.planb.res;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.zanfou.webp.R;
import com.zanfou.webp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDrawable(R.drawable.abc_vector_test);
        getDrawable(R.drawable.abc_ic_menu_copy_mtrl_am_alpha);
        super.onCreate(savedInstanceState);
        setContentView(ActivityMainBinding.inflate(getLayoutInflater()).getRoot());
    }
}
