package com.dipak.securechat.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.dipak.securechat.R;
import com.dipak.securechat.SecureChatApp;
import com.dipak.securechat.ui.MainActivity;

/**
 * Foreground service to maintain persistent live WebSocket connection in background.
 */
public class ChatForegroundService extends Service {

    private static final int NOTIFICATION_ID = 1001;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, SecureChatApp.CHANNEL_SERVICE_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.foreground_service_active))
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
