package com.formquid.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Envía los eventos prioritarios al servidor SMTP configurado. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailDispatcher {
    private static final int MAX_ATTEMPTS = 3;

    private final JavaMailSender sender;
    private final ScheduledExecutorService mailExecutor;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from}")
    private String from;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(EmailRequested email) {
        if (enabled) {
            schedule(email, 1, 0);
        }
    }

    private void schedule(EmailRequested email, int attempt, long delaySeconds) {
        try {
            mailExecutor.schedule(() -> sendAttempt(email, attempt), delaySeconds, TimeUnit.SECONDS);
        } catch (RejectedExecutionException error) {
            log.warn("No se ha podido programar el envío de correo ({})", error.getClass().getSimpleName());
        }
    }

    private void sendAttempt(EmailRequested email, int attempt) {
        try {
            send(email.recipient(), email.subject(), email.body());
        } catch (MailException error) {
            if (attempt < MAX_ATTEMPTS) {
                log.warn("Falló el envío de correo (intento {}/{}, {}). Se reintentará.",
                    attempt, MAX_ATTEMPTS, error.getClass().getSimpleName());
                schedule(email, attempt + 1, 30L << (attempt - 1));
            } else {
                log.error("No se ha podido enviar el correo tras {} intentos ({})",
                    MAX_ATTEMPTS, error.getClass().getSimpleName());
            }
        } catch (RuntimeException error) {
            log.error("Error al preparar el correo ({})", error.getClass().getSimpleName());
        }
    }

    public void send(String recipient, String subject, String body) {
        if (!enabled) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        sender.send(message);
    }
}
