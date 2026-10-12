package com.dipak.securechat.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.dipak.chatcore.api.ApiClient;
import com.dipak.chatcore.api.ChatApi;
import com.dipak.chatcore.model.UserModel;
import com.dipak.securechat.R;
import com.dipak.securechat.databinding.ActivityMainBinding;
import com.dipak.securechat.service.ChatForegroundService;
import com.dipak.securechat.store.SharedPreferencesKeyStorage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main conversation activity for Android.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferencesKeyStorage keyStorage;
    private ChatApi chatApi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        keyStorage = new SharedPreferencesKeyStorage(this);
        ApiClient client = new ApiClient("http://10.0.2.2:8080", keyStorage);
        chatApi = new ChatApi(client);

        startLiveService();
        loadUserProfile();
    }

    private void startLiveService() {
        Intent serviceIntent = new Intent(this, ChatForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void loadUserProfile() {
        executor.submit(() -> {
            try {
                UserModel me = chatApi.getMe();
                runOnUiThread(() -> {
                    if (me != null && getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle("Logged in as " + me.getName());
                    }
                });
            } catch (Exception e) {
                // Ignore or handle offline
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Log Out");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            keyStorage.clear();
            stopService(new Intent(this, ChatForegroundService.class));
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
