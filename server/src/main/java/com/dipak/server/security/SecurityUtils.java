package com.dipak.server.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility methods for extracting the authenticated user from the SecurityContext.
 * Used by controllers to get the current user's ID without repeating boilerplate.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Returns the authenticated user's ID from the JWT subject claim.
     * Throws IllegalStateException if called outside an authenticated request.
     */
    public static Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return Long.parseLong(ud.getUsername());
        }
        throw new IllegalStateException("No authenticated user found in SecurityContext");
    }
}
