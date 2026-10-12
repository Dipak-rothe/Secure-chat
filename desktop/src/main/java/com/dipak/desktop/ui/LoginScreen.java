package com.dipak.desktop.ui;

import com.dipak.chatcore.api.ApiClient;
import com.dipak.chatcore.api.ChatApi;
import com.dipak.chatcore.model.AuthResponse;
import com.dipak.chatcore.model.UserModel;
import com.dipak.chatcore.store.LocalStore;
import com.dipak.chatcore.store.SecureKeyStorage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Login and registration screen.
 * Handles user authentication with server URL configuration.
 */
public class LoginScreen {

    private final SecureKeyStorage keyStorage;
    private final LocalStore localStore;
    private final ExecutorService executor;
    private final Consumer<ChatApi> onLoginSuccess;

    private final TextField serverUrlField = new TextField("http://localhost:8090");
    private final Label errorLabel = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();

    public LoginScreen(SecureKeyStorage keyStorage,
                       LocalStore localStore,
                       ExecutorService executor,
                       Consumer<ChatApi> onLoginSuccess) {
        this.keyStorage = keyStorage;
        this.localStore = localStore;
        this.executor = executor;
        this.onLoginSuccess = onLoginSuccess;
    }

    public Parent createContent() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(400);

        Label title = new Label("Secure Chat");
        title.getStyleClass().add("title");

        Label subtitle = new Label("End-to-End Encrypted Messaging");
        subtitle.getStyleClass().add("subtitle");

        VBox headerBox = new VBox(4, title, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // Server URL input
        Label serverLabel = new Label("Server URL");
        VBox serverBox = new VBox(4, serverLabel, serverUrlField);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab loginTab = new Tab("Sign In", createLoginForm());
        Tab registerTab = new Tab("Register", createRegisterForm());
        tabPane.getTabs().addAll(loginTab, registerTab);

        errorLabel.getStyleClass().add("error-text");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        progress.setVisible(false);
        progress.setMaxSize(24, 24);

        card.getChildren().addAll(headerBox, serverBox, tabPane, errorLabel, progress);
        root.getChildren().add(card);
        return root;
    }

    private Parent createLoginForm() {
        VBox form = new VBox(12);
        form.setPadding(new Insets(16, 0, 0, 0));

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> doLogin(emailField.getText().trim(), passwordField.getText()));

        form.getChildren().addAll(
                new Label("Email"), emailField,
                new Label("Password"), passwordField,
                loginBtn
        );
        return form;
    }

    private Parent createRegisterForm() {
        VBox form = new VBox(12);
        form.setPadding(new Insets(16, 0, 0, 0));

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 8 chars)");

        Button registerBtn = new Button("Create Account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> doRegister(
                nameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText()
        ));

        form.getChildren().addAll(
                new Label("Name"), nameField,
                new Label("Email"), emailField,
                new Label("Password"), passwordField,
                registerBtn
        );
        return form;
    }

    private void doLogin(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        setLoading(true);
        String serverUrl = serverUrlField.getText().trim();

        executor.submit(() -> {
            try {
                ApiClient client = new ApiClient(serverUrl, keyStorage);
                ChatApi api = new ChatApi(client);
                AuthResponse resp = api.login(email, password);

                if (resp != null && resp.getUser() != null) {
                    localStore.saveCurrentUser(resp.getUser());
                }

                Platform.runLater(() -> {
                    setLoading(false);
                    onLoginSuccess.accept(api);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Login failed: " + ex.getMessage());
                });
            }
        });
    }

    private void doRegister(String name, String email, String password) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("All fields are required.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        setLoading(true);
        String serverUrl = serverUrlField.getText().trim();

        executor.submit(() -> {
            try {
                ApiClient client = new ApiClient(serverUrl, keyStorage);
                ChatApi api = new ChatApi(client);
                AuthResponse resp = api.register(name, email, password);

                if (resp != null && resp.getUser() != null) {
                    localStore.saveCurrentUser(resp.getUser());
                }

                Platform.runLater(() -> {
                    setLoading(false);
                    onLoginSuccess.accept(api);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Registration failed: " + ex.getMessage());
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisible(loading);
        errorLabel.setVisible(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
