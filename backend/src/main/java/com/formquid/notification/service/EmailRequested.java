package com.formquid.notification.service;

public record EmailRequested(String recipient, String subject, String body) {
}
