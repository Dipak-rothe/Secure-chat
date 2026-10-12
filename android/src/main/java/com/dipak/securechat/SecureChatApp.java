package com.dipak.securechat;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Application class for SecureChat Android.
 * Initializes Notification Channels on startup (Android 8.0+).
 */
public class SecureChatApp extends Application {

    public static final String CHANNEL_SERVICE_ID = "channel_foreground_service";
    public static final String CHANNEL_MESSAGES_ID = "channel_messages";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Low priority channel for background live service
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_SERVICE_ID,
                    getString(R.string.channel_foreground_service),
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Persistent service keeping real-time connection alive");

            // High priority channel for incoming chat messages
            NotificationChannel messageChannel = new NotificationChannel(
                    CHANNEL_MESSAGES_ID,
                    getString(R.string.channel_messages),
                    NotificationManager.IMPORTANCE_HIGH
            );
            messageChannel.setDescription("Notifications for incoming direct and group messages");
            messageChannel.enableVibration(true);

            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(messageChannel);
        }
    }
}
