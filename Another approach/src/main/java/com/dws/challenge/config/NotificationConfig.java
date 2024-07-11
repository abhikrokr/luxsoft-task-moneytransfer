package com.dws.challenge.config;

import com.dws.challenge.service.EmailNotificationService;
import com.dws.challenge.service.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public NotificationService getNotificationService(){
        return new EmailNotificationService();
    }
}
