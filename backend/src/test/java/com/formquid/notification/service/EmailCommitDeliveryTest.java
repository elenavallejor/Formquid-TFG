package com.formquid.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(EmailCommitDeliveryTest.TestConfig.class)
@TestPropertySource(properties = {
    "app.mail.enabled=true",
    "app.mail.from=notifications@formquid.local"
})
class EmailCommitDeliveryTest {
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean JavaMailSender sender;
    @MockitoBean ScheduledExecutorService mailExecutor;

    @Test
    void emailIsScheduledOnlyAfterCommit() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(new EmailRequested("person@example.com", "Aviso", "Contenido"));
            verifyNoInteractions(mailExecutor);
        });

        verify(mailExecutor).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.SECONDS));
    }

    @Test
    void emailIsDiscardedAfterRollback() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(new EmailRequested("person@example.com", "Aviso", "Contenido"));
            status.setRollbackOnly();
        });

        verifyNoInteractions(mailExecutor);
    }

    @Test
    void emailWithoutTransactionIsNotScheduled() {
        publisher.publishEvent(new EmailRequested("person@example.com", "Aviso", "Contenido"));

        verifyNoInteractions(mailExecutor);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import(NotificationEmailDispatcher.class)
    static class TestConfig {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }
}
