package com.formquid.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Envía los eventos prioritarios al servidor SMTP configurado. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailDispatcher {
    private final JavaMailSender sender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from}")
    private String from;

    public void send(String recipient, String subject, String body) {
        if (!enabled) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            sender.send(message);
        } catch (MailException error) {
            log.warn("No se ha podido enviar una notificación por correo ({})",
                error.getClass().getSimpleName());
        }
    }
}
