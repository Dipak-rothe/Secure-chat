package com.dipak.desktop.ui;

import com.dipak.chatcore.api.ChatApi;
import com.dipak.chatcore.model.UserModel;
import com.dipak.chatcore.store.LocalStore;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;

/**
 * Main application screen.
 * Left: User profile, new chat button, theme toggle, logout.
 * Center: Active chat pane or welcome screen.
 */
public class MainScreen {

    private final Stage stage;
    private final ChatApi chatApi;
    private final LocalStore localStore;
    private final ExecutorService executor;
    private final Runnable onLogout;
    private final Runnable onToggleTheme;

    private UserModel currentUser;
    private final Label userHeaderLabel = new Label();
    private final ListView<String> conversationList = new ListView<>();
    private final VBox chatArea = new VBox();
    private final Label activeChatTitle = new Label("Select a conversation");

    public MainScreen(Stage stage,
                      ChatApi chatApi,
                      LocalStore localStore,
                      ExecutorService executor,
                      Runnable onLogout,
                      Runnable onToggleTheme) {
        this.stage = stage;
        this.chatApi = chatApi;
        this.localStore = localStore;
        this.executor = executor;
        this.onLogout = onLogout;
        this.onToggleTheme = onToggleTheme;
    }

    public Parent createContent() {
        BorderPane root = new BorderPane();

        // Load profile in background
        loadProfile();

        // Left sidebar
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(12));
        sidebar.setPrefWidth(280);
        sidebar.setStyle("-fx-border-color: #313244; -fx-border-width: 0 1 0 0;");

        // Top bar in sidebar: Profile + New Chat Button + Theme + Logout
        userHeaderLabel.getStyleClass().add("title");
        userHeaderLabel.setStyle("-fx-font-size: 16px;");

        Button newChatBtn = new Button("+ New Chat");
        newChatBtn.setMaxWidth(Double.MAX_VALUE);
        newChatBtn.setOnAction(e -> openUserSearch());

        Button themeBtn = new Button("Toggle Theme");
        themeBtn.getStyleClass().add("button-secondary");
        themeBtn.setMaxWidth(Double.MAX_VALUE);
        themeBtn.setOnAction(e -> onToggleTheme.run());

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("button-secondary");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> doLogout());

        VBox topControls = new VBox(8, userHeaderLabel, newChatBtn, themeBtn, logoutBtn);

        Label convHeader = new Label("Chats");
        convHeader.getStyleClass().add("subtitle");

        conversationList.getItems().add("Welcome to Secure Chat");
        VBox.setVgrow(conversationList, Priority.ALWAYS);

        sidebar.getChildren().addAll(topControls, new Separator(), convHeader, conversationList);
        root.setLeft(sidebar);

        // Center: Chat area
        chatArea.setPadding(new Insets(16));
        chatArea.setAlignment(Pos.CENTER);

        activeChatTitle.getStyleClass().add("title");
        Label placeholderMsg = new Label("Click '+ New Chat' to find users and start messaging.");
        placeholderMsg.getStyleClass().add("subtitle");

        chatArea.getChildren().addAll(activeChatTitle, placeholderMsg);
        root.setCenter(chatArea);

        return root;
    }

    private void loadProfile() {
        executor.submit(() -> {
            try {
                currentUser = chatApi.getMe();
                Platform.runLater(() -> {
                    if (currentUser != null) {
                        userHeaderLabel.setText(currentUser.getName());
                    }
                });
            } catch (Exception ex) {
                // Fall back to local store
                UserModel cached = localStore.getCurrentUser();
                Platform.runLater(() -> {
                    if (cached != null) {
                        userHeaderLabel.setText(cached.getName());
                    } else {
                        userHeaderLabel.setText("Secure Chat");
                    }
                });
            }
        });
    }

    private void openUserSearch() {
        UserSearchDialog dialog = new UserSearchDialog(stage, chatApi, executor, targetUser -> {
            Platform.runLater(() -> {
                conversationList.getItems().add(targetUser.getName() + " (" + targetUser.getEmail() + ")");
                activeChatTitle.setText("Chat with " + targetUser.getName());
            });
        });
        dialog.showAndWait();
    }

    private void doLogout() {
        executor.submit(() -> {
            try {
                chatApi.logout();
            } catch (Exception ignored) {
            }
            localStore.clear();
            Platform.runLater(onLogout);
        });
    }
}
