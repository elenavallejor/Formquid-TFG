package com.formquid.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEmailDispatcherTest {
    @Mock JavaMailSender sender;
    @InjectMocks NotificationEmailDispatcher dispatcher;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(dispatcher, "enabled", true);
        ReflectionTestUtils.setField(dispatcher, "from", "notifications@formquid.local");
    }

    @Test
    void sendsMessageToConfiguredSmtpServer() {
        dispatcher.send("test@example.com", "Solicitud recibida", "Hemos recibido tu solicitud.");

        var capture = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(capture.capture());
        assertThat(capture.getValue().getTo()).containsExactly("test@example.com");
        assertThat(capture.getValue().getText()).isEqualTo("Hemos recibido tu solicitud.");
    }

    @Test
    void smtpFailureDoesNotAbortTheBusinessOperation() {
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> dispatcher.send("test@example.com", "Aviso", "Contenido"))
            .doesNotThrowAnyException();
    }

    @Test
    void disabledMailDoesNotContactSmtpServer() {
        ReflectionTestUtils.setField(dispatcher, "enabled", false);

        dispatcher.send("test@example.com", "Aviso", "Contenido");

        verifyNoInteractions(sender);
    }
}
