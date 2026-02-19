package com.xyz.entertainment.ticketing.user.service;

/**
 * Thrown when partner login is attempted for a tenant with status PENDING_APPROVAL.
 * Maps to 403 Forbidden.
 */
public class PartnerPendingApprovalException extends RuntimeException {

    public PartnerPendingApprovalException(String message) {
        super(message);
    }
}
