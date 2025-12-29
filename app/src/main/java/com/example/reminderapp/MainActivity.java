package com.example.reminderapp;

import static com.example.reminderapp.globalConfigs.*;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    EditText editText;
    TextView etaBox;
    Spinner spinner;
    int itemIndex;
    String PREFS, KEY_ETA, KEY_INTERVAL;

    final private String[] items = new String[]{"Water", "Dinner", "Journaling", "Random Stuff"};
    final private String[] upperBoundMessage = new String[]{
            "Cutie drink one glass every 4 hours",
            "Cutie have some food every 6-7 hours",
            "Cutie journal once a day",
            "Cutie a random thought a days is good",
    };
    final private String[] lowerBoundMessage = new String[]{
            "Cutie wait for 30 minutes between two sips",
            "Cutie wait for couple of hour between food",
            "Cutie relax for few hours before writing again",
            "Cutie you are so strong already",
    };

    final private String[] reminderType = new String[] {"WaterReminder","FoodReminder", "JournalingReminder",
    "RandomReminder"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        itemIndex = -1;

        spinner = findViewById(R.id.dropDownList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);

        Button button = findViewById(R.id.start);
        editText = (EditText) findViewById(R.id.time);
        etaBox = (TextView) findViewById(R.id.ETABox);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i >=0 && i<=3) {
                    MainActivity.this.itemIndex = i;
                    updateBackground(MainActivity.this, i);
                }
                else
                    Toast.makeText(MainActivity.this, "InCorrect option", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (itemIndex == -1)
            itemIndex = 0; // default to water.. kind of important ;)

        updateBackground(this, itemIndex);

        PREFS = GLOBAL_PREFS[itemIndex];
        KEY_INTERVAL = GLOBAL_KEY_INTERVAL[itemIndex];
        KEY_ETA = GLOBAL_KEY_ETA[itemIndex];

        restoreSavedData();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scheduleWaterReminder(MainActivity.this.itemIndex);
            }
        });

        // scheduleWaterReminder();
        requestNotificationPermission();
    }

    private void updateBackground(MainActivity cls, int itemIndex) {
        View rootLayout = findViewById(R.id.main);

        int hexColor = R.color.bg_water;
        if (itemIndex == 1) hexColor = R.color.bg_food;
        else if (itemIndex == 2) hexColor = R.color.bg_journaling;
        else if (itemIndex == 3) hexColor = R.color.bg_random;

        int color = ContextCompat.getColor(cls, hexColor);
        rootLayout.setBackgroundColor(color);

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


    private void scheduleWaterReminder(int itemIndex) {
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
            Toast.makeText(this, upperBoundMessage[itemIndex], Toast.LENGTH_LONG).show();
            minutes = 240;
        }

        if (minutes < 30 ) {
            Toast.makeText(this, lowerBoundMessage[itemIndex], Toast.LENGTH_LONG).show();
            minutes = 30;
        }

        Data inputData = new Data.Builder()
                .putInt(KEY_WORKER,itemIndex)
                .build();

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(
                        ReminderWorker.class,
                        minutes,
                        TimeUnit.MINUTES
                ).setInputData(inputData).build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        reminderType[itemIndex],
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                );
        long eta = System.currentTimeMillis() +
                TimeUnit.MINUTES.toMillis(minutes);

        savePreferences(minutes, eta);
        updateEtaText(eta);

        String etaTime = String.valueOf(minutes) + " mins";

        Toast.makeText(this, "Reminder in "+ etaTime +". Stay strong \uD83D\uDCAA!", Toast.LENGTH_LONG).show();
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