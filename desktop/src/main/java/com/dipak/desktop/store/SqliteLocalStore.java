package com.dipak.desktop.store;

import com.dipak.chatcore.model.UserModel;
import com.dipak.chatcore.store.LocalStore;

import java.io.File;
import java.sql.*;

/**
 * SQLite implementation of LocalStore for Desktop.
 * Stores local cache (current user profile, messages cache, etc.) in %APPDATA%\SecureChat\chat.db.
 */
public class SqliteLocalStore implements LocalStore {

    private final String dbUrl;

    public SqliteLocalStore() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.trim().isEmpty()) {
            appData = System.getProperty("user.home") + File.separator + ".securechat";
        } else {
            appData = appData + File.separator + "SecureChat";
        }
        File dir = new File(appData);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.dbUrl = "jdbc:sqlite:" + appData + File.separator + "chat.db";
        initTables();
    }

    public SqliteLocalStore(String customDbPath) {
        this.dbUrl = "jdbc:sqlite:" + customDbPath;
        initTables();
    }

    private void initTables() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS current_user (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT, " +
                    "email TEXT, " +
                    "last_seen TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS cached_messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "client_message_id TEXT UNIQUE, " +
                    "conversation_id INTEGER, " +
                    "sender_id INTEGER, " +
                    "content TEXT, " +
                    "status TEXT, " +
                    "created_at INTEGER)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise SQLite database", e);
        }
    }

    @Override
    public synchronized void saveCurrentUser(UserModel user) {
        if (user == null) {
            return;
        }
        String sql = "INSERT OR REPLACE INTO current_user (id, name, email, last_seen) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, user.getId());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getLastSeen());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save current user", e);
        }
    }

    @Override
    public synchronized UserModel getCurrentUser() {
        String sql = "SELECT id, name, email, last_seen FROM current_user LIMIT 1";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                UserModel user = new UserModel();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setLastSeen(rs.getString("last_seen"));
                return user;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get current user", e);
        }
    }

    @Override
    public synchronized void clear() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM current_user");
            stmt.execute("DELETE FROM cached_messages");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear local cache", e);
        }
    }
}
