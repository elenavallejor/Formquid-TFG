package com.formquid.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Envía los eventos prioritarios al servidor SMTP configurado. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailDispatcher {
    private static final int MAX_ATTEMPTS = 3;
    private static final String LOGO_CONTENT_ID = "formquid-logo";
    private static final String LOCK_CONTENT_ID = "formquid-lock";
    private static final ClassPathResource LOGO = new ClassPathResource("mail/formquid-logo.png");
    private static final ClassPathResource LOCK = new ClassPathResource("mail/formquid-lock.png");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>]+", Pattern.CASE_INSENSITIVE);

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
            send(email);
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
        send(new EmailRequested(recipient, subject, body));
    }

    public void send(EmailRequested email) {
        if (!enabled) {
            return;
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email.recipient());
            helper.setSubject(email.subject());
            helper.setText(email.body(), email.layout() == EmailRequested.Layout.PASSWORD_RESET
                ? passwordResetHtml(email.actionUrl()) : htmlBody(email.subject(), email.body()));
            helper.addInline(LOGO_CONTENT_ID, LOGO, "image/png");
            if (email.layout() == EmailRequested.Layout.PASSWORD_RESET) {
                helper.addInline(LOCK_CONTENT_ID, LOCK, "image/png");
            }
            sender.send(message);
        } catch (MessagingException error) {
            throw new MailPreparationException("No se ha podido preparar el correo", error);
        }
    }

    private static String passwordResetHtml(String actionUrl) {
        String safeUrl = escapeHtml(actionUrl);
        return "<html><body style=\"margin:0;padding:32px 16px;background:#fff;font-family:Arial,sans-serif;color:#002536;text-align:center\">"
            + "<div style=\"max-width:600px;margin:0 auto\">"
            + "<div style=\"padding:28px 0;text-align:center\">"
            + "<img src=\"cid:" + LOGO_CONTENT_ID + "\" alt=\"Logo de FormQuid\" width=\"48\" height=\"48\" style=\"vertical-align:middle\">"
            + "<strong style=\"margin-left:8px;font-size:24px;vertical-align:middle\">FORMQUID</strong></div>"
            + "<div style=\"padding:42px 24px;background:#f3f3f3;text-align:center\">"
            + "<img src=\"cid:" + LOCK_CONTENT_ID + "\" alt=\"Candado\" width=\"44\" height=\"44\">"
            + "<h1 style=\"margin:18px 0 20px;font-size:32px;line-height:1.2\">¡Hola!</h1>"
            + "<p style=\"margin:0 0 28px;font-size:20px;font-weight:bold;line-height:1.4\">¿Has pedido cambiar tu contraseña?</p>"
            + "<a href=\"" + safeUrl + "\" style=\"display:inline-block;padding:14px 30px;border-radius:6px;background:#002536;color:#fff;font-size:16px;font-weight:bold;text-decoration:none\">RESTABLECER CONTRASEÑA</a>"
            + "<p style=\"margin:24px 0 0;color:#53616a;font-size:14px;line-height:1.5\">El enlace caduca en 30 minutos y solo puede usarse una vez.<br>Si no has solicitado este cambio, ignora este correo.</p>"
            + "</div></div></body></html>";
    }

    private static String htmlBody(String subject, String body) {
        return "<html><body style=\"margin:0;padding:24px;background:#f4f7f7;font-family:Arial,sans-serif;color:#002536\">"
            + "<div style=\"max-width:600px;margin:0 auto;padding:28px;background:#fff;border-radius:10px\">"
            + "<div style=\"display:flex;align-items:center;margin-bottom:24px\">"
            + "<img src=\"cid:" + LOGO_CONTENT_ID + "\" alt=\"Logo de FormQuid\" width=\"56\" height=\"56\" style=\"vertical-align:middle\">"
            + "<strong style=\"margin-left:10px;font-size:24px;vertical-align:middle\">FormQuid</strong></div>"
            + "<h1 style=\"font-size:22px;line-height:1.3\">" + escapeHtml(subject) + "</h1>"
            + "<p style=\"font-size:16px;line-height:1.6\">" + formatBody(body) + "</p>"
            + "</div></body></html>";
    }

    private static String formatBody(String body) {
        Matcher urls = URL_PATTERN.matcher(body);
        StringBuilder html = new StringBuilder();
        int position = 0;
        while (urls.find()) {
            html.append(escapeHtml(body.substring(position, urls.start())).replace("\n", "<br>"));
            String url = escapeHtml(urls.group());
            html.append("<a href=\"").append(url).append("\">").append(url).append("</a>");
            position = urls.end();
        }
        return html.append(escapeHtml(body.substring(position)).replace("\n", "<br>")).toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
