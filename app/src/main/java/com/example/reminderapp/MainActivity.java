package com.example.reminderapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "water_prefs";
    private static final String KEY_INTERVAL = "interval_minutes";
    private static final String KEY_ETA = "next_eta";

    EditText editText;
    TextView etaBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.start);
        editText = (EditText) findViewById(R.id.time);
        etaBox = (TextView) findViewById(R.id.ETABox);
        restoreSavedData();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scheduleWaterReminder();
            }
        });

        // scheduleWaterReminder();
        requestNotificationPermission();
    }

    private void restoreSavedData() {
        long interval =
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getLong(KEY_INTERVAL, -1);

        long eta =
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getLong(KEY_ETA, -1);

        if (interval != -1) {
            editText.setText(String.valueOf(interval));
        }

        if (eta != -1) {
            updateEtaText(eta);
        }
    }

    private void savePreferences(long minutes, long eta) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putLong(KEY_INTERVAL, minutes)
                .putLong(KEY_ETA, eta)
                .apply();
    }

    private void updateEtaText(long etaMillis) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("hh:mm a", Locale.getDefault());

        String time = sdf.format(new Date(etaMillis));
        etaBox.setText("Next reminder around " + time);
    }


    private void scheduleWaterReminder() {
        /* testing code
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(ReminderWorker.class)
                        .setInitialDelay(20, TimeUnit.SECONDS)
                        .build();

        WorkManager.getInstance(this).enqueue(workRequest);*/

        String input = editText.getText().toString();

        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "Enter minutes", Toast.LENGTH_SHORT).show();
            return;
        }

        long minutes = Long.parseLong(input);
        if (minutes > 240) {
            Toast.makeText(this, "Cutie drink one glass every 4 hours", Toast.LENGTH_LONG).show();
            minutes = 240;
        }

        if (minutes < 30 ) {
            Toast.makeText(this, "Cutie wait for 30 minutes between two sips", Toast.LENGTH_LONG).show();
            minutes = 30;
        }

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(
                        ReminderWorker.class,
                        minutes,
                        TimeUnit.MINUTES
                ).build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "WaterReminder",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                );


        long eta = System.currentTimeMillis() +
                TimeUnit.MINUTES.toMillis(minutes);

        savePreferences(minutes, eta);
        updateEtaText(eta);
        
        String etaTime = String.valueOf(minutes) + "minutes";

        Toast.makeText(this, "Reminder in "+ etaTime +". Start Hydrated!", Toast.LENGTH_LONG).show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    101
            );
        }
    }
}