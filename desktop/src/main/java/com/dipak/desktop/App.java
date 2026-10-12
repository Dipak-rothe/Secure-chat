package com.dipak.desktop;

import com.dipak.chatcore.api.ApiClient;
import com.dipak.chatcore.api.ChatApi;
import com.dipak.chatcore.store.LocalStore;
import com.dipak.chatcore.store.SecureKeyStorage;
import com.dipak.desktop.store.FileSecureKeyStorage;
import com.dipak.desktop.store.SqliteLocalStore;
import com.dipak.desktop.ui.LoginScreen;
import com.dipak.desktop.ui.MainScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main JavaFX Application class.
 * Coordinates themes, system tray integration, navigation between Login and Main screens.
 */
public class App extends Application {

    private Stage primaryStage;
    private Scene mainScene;
    private boolean isDarkTheme = true;

    private SecureKeyStorage keyStorage;
    private LocalStore localStore;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.keyStorage = new FileSecureKeyStorage();
        this.localStore = new SqliteLocalStore();

        stage.setTitle("Secure Chat");
        stage.setWidth(950);
        stage.setHeight(650);

        // Check if existing token exists
        if (keyStorage.get("access_token") != null) {
            ApiClient client = new ApiClient("http://localhost:8090", keyStorage);
            showMainScreen(new ChatApi(client));
        } else {
            showLoginScreen();
        }

        setupSystemTray();
        stage.show();
    }

    public void showLoginScreen() {
        LoginScreen login = new LoginScreen(keyStorage, localStore, executor, this::showMainScreen);
        mainScene = new Scene(login.createContent());
        applyTheme();
        primaryStage.setScene(mainScene);
    }

    public void showMainScreen(ChatApi chatApi) {
        MainScreen mainScreen = new MainScreen(
                primaryStage,
                chatApi,
                localStore,
                executor,
                this::showLoginScreen,
                this::toggleTheme
        );
        mainScene = new Scene(mainScreen.createContent());
        applyTheme();
        primaryStage.setScene(mainScene);
    }

    public void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
    }

    private void applyTheme() {
        if (mainScene == null) return;
        mainScene.getStylesheets().clear();
        String themePath = isDarkTheme ? "/styles/dark.css" : "/styles/light.css";
        var resource = getClass().getResource(themePath);
        if (resource != null) {
            mainScene.getStylesheets().add(resource.toExternalForm());
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            // Create a small 16x16 default icon image if none provided
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                    16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new java.awt.Color(30, 144, 255));
            g.fillOval(0, 0, 16, 16);
            g.dispose();

            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("Open Secure Chat");
            openItem.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));

            MenuItem quitItem = new MenuItem("Quit");
            quitItem.addActionListener(e -> {
                executor.shutdown();
                Platform.exit();
                System.exit(0);
            });

            popup.add(openItem);
            popup.addSeparator();
            popup.add(quitItem);

            TrayIcon trayIcon = new TrayIcon(img, "Secure Chat", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));

            tray.add(trayIcon);
            Platform.setImplicitExit(false);

            primaryStage.setOnCloseRequest(event -> {
                // Minimize to tray on close
                event.consume();
                primaryStage.hide();
            });
        } catch (Exception ignored) {
            // System tray fallback: regular window close
        }
    }

    @Override
    public void stop() {
        executor.shutdown();
    }
}
