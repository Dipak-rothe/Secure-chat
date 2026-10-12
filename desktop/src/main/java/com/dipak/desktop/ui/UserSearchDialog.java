package com.dipak.desktop.ui;

import com.dipak.chatcore.api.ChatApi;
import com.dipak.chatcore.model.UserModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Modal dialog for searching registered users.
 * All network calls are dispatched to a background thread.
 */
public class UserSearchDialog extends Stage {

    private final ChatApi chatApi;
    private final ExecutorService executor;
    private final Consumer<UserModel> onUserSelected;

    private final TextField searchField = new TextField();
    private final ListView<UserModel> resultsList = new ListView<>();
    private final Label statusLabel = new Label();
    private final Button startChatButton = new Button("Start Chat");

    public UserSearchDialog(Stage parent, ChatApi chatApi, ExecutorService executor, Consumer<UserModel> onUserSelected) {
        this.chatApi = chatApi;
        this.executor = executor;
        this.onUserSelected = onUserSelected;

        initModality(Modality.WINDOW_MODAL);
        initOwner(parent);
        setTitle("New Chat - Find User");
        setWidth(420);
        setHeight(480);

        buildUi();
    }

    private void buildUi() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Label header = new Label("Search Users");
        header.getStyleClass().add("title");

        searchField.setPromptText("Enter name or email (2-10 characters)...");
        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> executeSearch());
        searchField.setOnAction(e -> executeSearch());

        HBox searchBox = new HBox(8, searchField, searchButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        resultsList.setCellFactory(lv -> new ListCell<UserModel>() {
            @Override
            protected void updateItem(UserModel user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String name = user.getName() != null ? user.getName() : "User";
                    String email = user.getEmail() != null ? user.getEmail() : "";
                    setText(name + (!email.isEmpty() ? " (" + email + ")" : ""));
                }
            }
        });

        startChatButton.setDisable(true);
        resultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            startChatButton.setDisable(newV == null);
        });

        startChatButton.setOnAction(e -> {
            UserModel selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                close();
                onUserSelected.accept(selected);
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("button-secondary");
        cancelButton.setOnAction(e -> close());

        HBox buttonBox = new HBox(8, cancelButton, startChatButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        statusLabel.getStyleClass().add("subtitle");

        root.getChildren().addAll(header, searchBox, statusLabel, resultsList, buttonBox);
        VBox.setVgrow(resultsList, Priority.ALWAYS);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(getOwner().getScene().getStylesheets());
        setScene(scene);
    }

    private void executeSearch() {
        String query = searchField.getText().trim();
        if (query.length() < 2 || query.length() > 10) {
            statusLabel.setText("Query must be between 2 and 10 characters.");
            return;
        }

        statusLabel.setText("Searching...");
        resultsList.getItems().clear();

        executor.submit(() -> {
            try {
                List<UserModel> users = chatApi.searchUsers(query);
                Platform.runLater(() -> {
                    resultsList.getItems().addAll(users);
                    if (users.isEmpty()) {
                        statusLabel.setText("No users found matching \"" + query + "\"");
                    } else {
                        statusLabel.setText("Found " + users.size() + " user(s)");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Search failed: " + ex.getMessage()));
            }
        });
    }
}
