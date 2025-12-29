package com.example.reminderapp;

import static com.example.reminderapp.globalConfigs.*;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class ReminderWorker extends Worker {

    private final static String[] CHANNEL_ID = {"water_reminder_channel","food_reminder_channel","journaling_reminder_channel","random_reminder_channel"};
    private final static String[] reminderName = {"Water Reminder","Food Reminder","Journaling Reminder","Random Reminder"};
    private final static String[] title = {"Stay hydrated cutie \uD83D\uDCA7","Stay full sweetie \uD83C\uDF5A","Stay relaxed bubula \uD83D\uDCDA","Stay strong cutie \uD83E\uDD8B"};
    private final static String[] content = {
            "Drink a glass of water.",
            "Have some food.",
            "Time to write down your thoughts.",
            "Keep going sweetheart, you are best."
    };

    private int itemIndex;
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        itemIndex = getInputData().getInt(KEY_WORKER, 0);
        showNotification();
        updateNextEta();
        return Result.success();
    }

    private void showNotification() {
        String channelId = CHANNEL_ID[itemIndex];

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    reminderName[itemIndex],
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);


        builder.setContentTitle(title[itemIndex])
                .setContentText(content[itemIndex]);

        switch (itemIndex) {
            case 0:
                builder.setSmallIcon(R.drawable.glass);
                break;
            case 1:
                builder.setSmallIcon(R.drawable.food);
                break;
            case 2:
                builder.setSmallIcon(R.drawable.journaling);
                break;
            case 3:
                builder.setSmallIcon(R.drawable.positiveenergy);
                break;
        }

        notificationManager.notify(1, builder.build());
    }

    private void updateNextEta() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(GLOBAL_PREFS[itemIndex], Context.MODE_PRIVATE);

        long interval = prefs.getLong(GLOBAL_KEY_INTERVAL[itemIndex], -1);
        long eta = prefs.getLong(GLOBAL_KEY_ETA[itemIndex], -1);

        if (interval == -1 || eta == -1) return;

        long nextEta = eta + TimeUnit.MINUTES.toMillis(interval);
        prefs.edit().putLong(GLOBAL_KEY_ETA[itemIndex], nextEta).apply();
    }


}
