package com.formquid.notification.service;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEmailDispatcherTest {
    @Mock JavaMailSender sender;
    @Mock ScheduledExecutorService mailExecutor;
    @InjectMocks NotificationEmailDispatcher dispatcher;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(dispatcher, "enabled", true);
        ReflectionTestUtils.setField(dispatcher, "from", "notifications@formquid.local");
    }

    @Test
    void sendsBrandedHtmlWithPlainTextFallbackAndInlineLogo() throws Exception {
        mockMimeMessage();
        dispatcher.send("test@example.com", "Solicitud recibida", "Abre este enlace:\nhttps://example.com/reset?token=a&b=2");

        var capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(capture.capture());
        MimeMessage message = capture.getValue();
        message.saveChanges();
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("test@example.com");
        assertThat(message.getSubject()).isEqualTo("Solicitud recibida");

        List<Part> parts = allParts(message);
        assertThat(findPart(parts, "text/plain").getContent().toString())
            .contains("https://example.com/reset?token=a&b=2");
        assertThat(findPart(parts, "text/html").getContent().toString())
            .contains("cid:formquid-logo", "FormQuid", "href=\"https://example.com/reset?token=a&amp;b=2\"");
        assertThat(findPart(parts, "image/png").getHeader("Content-ID"))
            .containsExactly("<formquid-logo>");
    }

    @Test
    void passwordResetEmailUsesCenteredLayoutAndInlineLock() throws Exception {
        mockMimeMessage();
        String link = "https://formquid.test/restablecer-contrasena?token=abc";
        dispatcher.send(EmailRequested.passwordReset("test@example.com", "Restablece tu contraseña de FormQuid",
            "Abre este enlace:\n" + link + "\nCaduca en 30 minutos.", link));

        var capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(capture.capture());
        MimeMessage message = capture.getValue();
        message.saveChanges();
        List<Part> parts = allParts(message);

        assertThat(findPart(parts, "text/plain").getContent().toString()).contains(link);
        assertThat(findPart(parts, "text/html").getContent().toString())
            .contains("FORMQUID", "cid:formquid-logo", "cid:formquid-lock", "¡Hola!",
                "¿Has pedido cambiar tu contraseña?", "href=\"" + link + "\"", "RESTABLECER CONTRASEÑA");
        assertThat(findPart(parts, "image/png").getHeader("Content-ID")).containsExactly("<formquid-logo>");
        assertThat(parts).filteredOn(part -> "<formquid-lock>".equals(contentId(part))).hasSize(1);
    }

    @Test
    void smtpFailureIsReportedToTheRetryHandler() {
        mockMimeMessage();
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> dispatcher.send("test@example.com", "Aviso", "Contenido"))
            .isInstanceOf(MailSendException.class);
    }

    @Test
    void failedMessageIsRetriedTwiceAndThenLogged() {
        mockMimeMessage();
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(MimeMessage.class));

        dispatcher.dispatch(new EmailRequested("test@example.com", "Aviso", "Contenido"));
        var tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(mailExecutor).schedule(tasks.capture(), eq(0L), eq(TimeUnit.SECONDS));

        tasks.getValue().run();
        verify(mailExecutor).schedule(tasks.capture(), eq(30L), eq(TimeUnit.SECONDS));

        tasks.getValue().run();
        verify(mailExecutor).schedule(tasks.capture(), eq(60L), eq(TimeUnit.SECONDS));

        tasks.getValue().run();
        verify(sender, times(3)).send(any(MimeMessage.class));
        verifyNoMoreInteractions(mailExecutor);
    }

    @Test
    void disabledMailDoesNotContactSmtpServer() {
        ReflectionTestUtils.setField(dispatcher, "enabled", false);

        dispatcher.send("test@example.com", "Aviso", "Contenido");

        verifyNoInteractions(sender);
        dispatcher.dispatch(new EmailRequested("test@example.com", "Aviso", "Contenido"));
        verifyNoInteractions(mailExecutor);
    }

    private static List<Part> allParts(Part part) throws Exception {
        List<Part> parts = new ArrayList<>();
        if (part.getContent() instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                parts.addAll(allParts(multipart.getBodyPart(index)));
            }
        } else {
            parts.add(part);
        }
        return parts;
    }

    private static Part findPart(List<Part> parts, String mimeType) throws Exception {
        for (Part part : parts) {
            if (part.isMimeType(mimeType)) return part;
        }
        throw new AssertionError("Falta la parte MIME " + mimeType);
    }

    private void mockMimeMessage() {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private static String contentId(Part part) {
        try {
            String[] values = part.getHeader("Content-ID");
            return values == null ? null : values[0];
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }
}
