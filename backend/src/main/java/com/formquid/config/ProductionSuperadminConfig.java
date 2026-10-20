package com.formquid.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
@EnableConfigurationProperties(ProductionSuperadminProperties.class)
public class ProductionSuperadminConfig {
}
