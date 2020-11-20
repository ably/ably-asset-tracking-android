package com.ably.tracking.example.javapublisher;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Hello via Timber");
        setContentView(R.layout.activity_main);
    }
}
