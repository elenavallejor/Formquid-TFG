package com.formquid.notification.service;

public record EmailRequested(String recipient, String subject, String body, Layout layout, String actionUrl) {
    public enum Layout { STANDARD, PASSWORD_RESET }

    public EmailRequested(String recipient, String subject, String body) {
        this(recipient, subject, body, Layout.STANDARD, null);
    }

    public static EmailRequested passwordReset(String recipient, String subject, String body, String actionUrl) {
        return new EmailRequested(recipient, subject, body, Layout.PASSWORD_RESET, actionUrl);
    }
}
