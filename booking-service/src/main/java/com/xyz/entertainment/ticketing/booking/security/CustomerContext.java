package com.xyz.entertainment.ticketing.booking.security;

/**
 * Simple thread-local context holding userId extracted
 * from the JWT for the current request.
 */
public final class CustomerContext {

    private static final ThreadLocal<Long> USER_HOLDER = new ThreadLocal<>();

    private CustomerContext() {
    }

    public static void setUserId(Long userId) {
        USER_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}

