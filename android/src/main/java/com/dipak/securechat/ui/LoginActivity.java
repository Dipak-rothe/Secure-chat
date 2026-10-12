package com.dipak.securechat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.dipak.chatcore.api.ApiClient;
import com.dipak.chatcore.api.ChatApi;
import com.dipak.chatcore.model.AuthResponse;
import com.dipak.securechat.databinding.ActivityLoginBinding;
import com.dipak.securechat.store.SharedPreferencesKeyStorage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Authentication activity for Android with sign-in and register modes.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SharedPreferencesKeyStorage keyStorage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        keyStorage = new SharedPreferencesKeyStorage(this);

        // Auto-login if token exists
        if (keyStorage.get("access_token") != null) {
            navigateToMain();
            return;
        }

        setupViews();
    }

    private void setupViews() {
        binding.tvToggleMode.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            if (isRegisterMode) {
                binding.tilName.setVisibility(View.VISIBLE);
                binding.btnSubmit.setText("Create Account");
                binding.tvToggleMode.setText("Already have an account? Sign In");
            } else {
                binding.tilName.setVisibility(View.GONE);
                binding.btnSubmit.setText("Sign In");
                binding.tvToggleMode.setText("Don't have an account? Register");
            }
            binding.tvError.setVisibility(View.GONE);
        });

        binding.btnSubmit.setOnClickListener(v -> submitAuth());
    }

    private void submitAuth() {
        String serverUrl = binding.etServerUrl.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String name = binding.etName.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password are required.");
            return;
        }

        if (isRegisterMode && name.isEmpty()) {
            showError("Name is required.");
            return;
        }

        setLoading(true);

        executor.submit(() -> {
            try {
                ApiClient client = new ApiClient(serverUrl, keyStorage);
                ChatApi api = new ChatApi(client);
                AuthResponse resp;

                if (isRegisterMode) {
                    resp = api.register(name, email, password);
                } else {
                    resp = api.login(email, password);
                }

                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Welcome, " + resp.getUser().getName(), Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
        binding.tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
