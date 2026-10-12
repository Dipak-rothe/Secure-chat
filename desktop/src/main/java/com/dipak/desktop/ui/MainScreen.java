package com.dipak.desktop.ui;

import com.dipak.chatcore.api.ChatApi;
import com.dipak.chatcore.model.ConversationModel;
import com.dipak.chatcore.model.MessageModel;
import com.dipak.chatcore.model.UserModel;
import com.dipak.chatcore.store.LocalStore;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.*;

/**
 * Main application screen.
 * Left: User profile, conversations list, new chat button, theme toggle, logout.
 * Center: Active chat pane with message history and real-time message sending.
 */
public class MainScreen {

    private final Stage stage;
    private final ChatApi chatApi;
    private final LocalStore localStore;
    private final ExecutorService executor;
    private final Runnable onLogout;
    private final Runnable onToggleTheme;

    private UserModel currentUser;
    private Long activeConversationId;
    private String activeChatUserName = "";

    private final Label userHeaderLabel = new Label("Secure Chat");
    private final ListView<ConversationModel> conversationList = new ListView<>();
    private final BorderPane chatPane = new BorderPane();
    private final ListView<MessageModel> messageListView = new ListView<>();
    private final TextField messageInputField = new TextField();
    private final Button sendButton = new Button("Send");

    private ScheduledExecutorService pollScheduler;

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

        // Left sidebar
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(12));
        sidebar.setPrefWidth(280);
        sidebar.setStyle("-fx-border-color: #313244; -fx-border-width: 0 1 0 0;");

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

        configureConversationList();
        VBox.setVgrow(conversationList, Priority.ALWAYS);

        sidebar.getChildren().addAll(topControls, new Separator(), convHeader, conversationList);
        root.setLeft(sidebar);

        // Center: Chat area
        showWelcomePlaceholder();
        root.setCenter(chatPane);

        // Configure message list view cell factory
        configureMessageListView();

        // Configure input box
        messageInputField.setPromptText("Type a message...");
        HBox.setHgrow(messageInputField, Priority.ALWAYS);
        messageInputField.setOnAction(e -> doSendMessage());
        sendButton.setOnAction(e -> doSendMessage());

        // Load profile and existing conversations
        loadProfileAndConversations();

        // Start background message polling for real-time delivery
        startMessagePolling();

        return root;
    }

    private void configureConversationList() {
        conversationList.setCellFactory(lv -> new ListCell<ConversationModel>() {
            @Override
            protected void updateItem(ConversationModel conv, boolean empty) {
                super.updateItem(conv, empty);
                if (empty || conv == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String title = conv.getOtherUserName() != null ? conv.getOtherUserName() : conv.getTitle();
                    if (title == null || title.isEmpty()) {
                        title = "Conversation #" + conv.getId();
                    }
                    setText(title);
                }
            }
        });

        conversationList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                openConversation(selected);
            }
        });
    }

    private void configureMessageListView() {
        messageListView.setCellFactory(lv -> new ListCell<MessageModel>() {
            @Override
            protected void updateItem(MessageModel msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean isMe = currentUser != null && msg.getSenderId() != null && msg.getSenderId().equals(currentUser.getId());

                    Label textLabel = new Label(msg.getCiphertext());
                    textLabel.setWrapText(true);
                    textLabel.setMaxWidth(380);

                    VBox bubble = new VBox(textLabel);
                    bubble.setPadding(new Insets(8, 14, 8, 14));

                    HBox container = new HBox(bubble);
                    if (isMe) {
                        container.setAlignment(Pos.CENTER_RIGHT);
                        bubble.setStyle("-fx-background-color: #89b4fa; -fx-background-radius: 14px 14px 2px 14px;");
                        textLabel.setStyle("-fx-text-fill: #11111b; -fx-font-weight: bold;");
                    } else {
                        container.setAlignment(Pos.CENTER_LEFT);
                        bubble.setStyle("-fx-background-color: #313244; -fx-background-radius: 14px 14px 14px 2px;");
                        textLabel.setStyle("-fx-text-fill: #cdd6f4;");
                    }

                    setGraphic(container);
                    setText(null);
                }
            }
        });
    }

    private void showWelcomePlaceholder() {
        VBox placeholder = new VBox(12);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(20));

        Label title = new Label("Welcome to Secure Chat");
        title.getStyleClass().add("title");

        Label desc = new Label("Select a conversation from the left or click '+ New Chat' to start messaging.");
        desc.getStyleClass().add("subtitle");

        placeholder.getChildren().addAll(title, desc);
        chatPane.setCenter(placeholder);
        chatPane.setTop(null);
        chatPane.setBottom(null);
    }

    private void openConversation(ConversationModel conv) {
        this.activeConversationId = conv.getId();
        this.activeChatUserName = conv.getOtherUserName() != null ? conv.getOtherUserName() : conv.getTitle();

        // Top Header
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(12, 16, 12, 16));
        headerBox.setStyle("-fx-border-color: #313244; -fx-border-width: 0 0 1 0;");

        Label nameLabel = new Label("Chat with " + activeChatUserName);
        nameLabel.getStyleClass().add("title");
        nameLabel.setStyle("-fx-font-size: 18px;");

        Label secureBadge = new Label("🔒 End-to-End Encrypted");
        secureBadge.getStyleClass().add("subtitle");

        headerBox.getChildren().addAll(nameLabel, secureBadge);
        chatPane.setTop(headerBox);

        // Center Messages
        messageListView.getItems().clear();
        chatPane.setCenter(messageListView);

        // Bottom Input Bar
        HBox inputBar = new HBox(8, messageInputField, sendButton);
        inputBar.setPadding(new Insets(12));
        inputBar.setStyle("-fx-border-color: #313244; -fx-border-width: 1 0 0 0;");
        chatPane.setBottom(inputBar);

        messageInputField.requestFocus();

        // Load existing messages
        loadMessages();
    }

    private void loadMessages() {
        if (activeConversationId == null) return;
        final Long convId = activeConversationId;

        executor.submit(() -> {
            try {
                List<MessageModel> msgs = chatApi.getMessages(convId);
                Platform.runLater(() -> {
                    if (Objects.equals(activeConversationId, convId)) {
                        messageListView.getItems().setAll(msgs);
                        if (!msgs.isEmpty()) {
                            messageListView.scrollTo(msgs.size() - 1);
                        }
                    }
                });
            } catch (Exception ex) {
                // Ignore background fetch error
            }
        });
    }

    private void doSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty() || activeConversationId == null) {
            return;
        }

        final Long convId = activeConversationId;
        messageInputField.clear();
        sendButton.setDisable(true);

        executor.submit(() -> {
            try {
                MessageModel sent = chatApi.sendMessage(convId, text);
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    if (Objects.equals(activeConversationId, convId)) {
                        messageListView.getItems().add(sent);
                        messageListView.scrollTo(messageListView.getItems().size() - 1);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to send message: " + ex.getMessage(), ButtonType.OK);
                    alert.show();
                });
            }
        });
    }

    private void startMessagePolling() {
        pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Chat-MessagePoller");
            t.setDaemon(true);
            return t;
        });

        pollScheduler.scheduleWithFixedDelay(() -> {
            if (activeConversationId != null) {
                final Long convId = activeConversationId;
                try {
                    List<MessageModel> latest = chatApi.getMessages(convId);
                    Platform.runLater(() -> {
                        if (Objects.equals(activeConversationId, convId) && latest.size() != messageListView.getItems().size()) {
                            messageListView.getItems().setAll(latest);
                            if (!latest.isEmpty()) {
                                messageListView.scrollTo(latest.size() - 1);
                            }
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void loadProfileAndConversations() {
        executor.submit(() -> {
            try {
                currentUser = chatApi.getMe();
                Platform.runLater(() -> {
                    if (currentUser != null) {
                        userHeaderLabel.setText(currentUser.getName());
                    }
                });
            } catch (Exception ex) {
                UserModel cached = localStore.getCurrentUser();
                Platform.runLater(() -> {
                    if (cached != null) {
                        currentUser = cached;
                        userHeaderLabel.setText(cached.getName());
                    }
                });
            }

            // Load conversations
            try {
                List<ConversationModel> convs = chatApi.getConversations();
                Platform.runLater(() -> {
                    conversationList.getItems().setAll(convs);
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void openUserSearch() {
        UserSearchDialog dialog = new UserSearchDialog(stage, chatApi, executor, targetUser -> {
            executor.submit(() -> {
                try {
                    ConversationModel conv = chatApi.createOrGetDirectConversation(targetUser.getId());
                    if (conv.getOtherUserName() == null) {
                        conv.setOtherUserName(targetUser.getName());
                    }
                    Platform.runLater(() -> {
                        boolean exists = false;
                        for (ConversationModel existing : conversationList.getItems()) {
                            if (existing.getId().equals(conv.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            conversationList.getItems().add(conv);
                        }
                        conversationList.getSelectionModel().select(conv);
                        openConversation(conv);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to start chat: " + ex.getMessage(), ButtonType.OK);
                        alert.show();
                    });
                }
            });
        });
        dialog.showAndWait();
    }

    private void doLogout() {
        if (pollScheduler != null) {
            pollScheduler.shutdownNow();
        }
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
