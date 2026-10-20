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
    void sendsMessageToConfiguredSmtpServer() {
        dispatcher.send("test@example.com", "Solicitud recibida", "Hemos recibido tu solicitud.");

        var capture = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(capture.capture());
        assertThat(capture.getValue().getTo()).containsExactly("test@example.com");
        assertThat(capture.getValue().getText()).isEqualTo("Hemos recibido tu solicitud.");
    }

    @Test
    void smtpFailureIsReportedToTheRetryHandler() {
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> dispatcher.send("test@example.com", "Aviso", "Contenido"))
            .isInstanceOf(MailSendException.class);
    }

    @Test
    void failedMessageIsRetriedTwiceAndThenLogged() {
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(SimpleMailMessage.class));

        dispatcher.dispatch(new EmailRequested("test@example.com", "Aviso", "Contenido"));
        var tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(mailExecutor).schedule(tasks.capture(), eq(0L), eq(TimeUnit.SECONDS));

        tasks.getValue().run();
        verify(mailExecutor).schedule(tasks.capture(), eq(30L), eq(TimeUnit.SECONDS));

        tasks.getValue().run();
        verify(mailExecutor).schedule(tasks.capture(), eq(60L), eq(TimeUnit.SECONDS));

        tasks.getValue().run();
        verify(sender, times(3)).send(any(SimpleMailMessage.class));
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
}
