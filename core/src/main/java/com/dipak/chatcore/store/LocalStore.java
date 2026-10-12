package com.dipak.chatcore.store;

import com.dipak.chatcore.model.UserModel;

/**
 * Platform-specific local cache interface.
 * Desktop: SQLite via sqlite-jdbc.
 * Android: Room database.
 *
 * All methods are synchronous and may block; call from a background thread.
 */
public interface LocalStore {

    /** Persist the authenticated user's profile. */
    void saveCurrentUser(UserModel user);

    /** Retrieve the cached current user, or null if none. */
    UserModel getCurrentUser();

    /** Clear all cached data (e.g. on logout). */
    void clear();
}
