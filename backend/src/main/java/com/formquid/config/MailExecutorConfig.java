package com.formquid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class MailExecutorConfig {
    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService mailExecutor() {
        return Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "mail-dispatcher");
            thread.setDaemon(true);
            return thread;
        });
    }
}
