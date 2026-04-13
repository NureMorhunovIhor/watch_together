package com.example.watch_together.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class MailProperties {
    private Mail mail = new Mail();
    private Frontend frontend = new Frontend();

    @Data
    public static class Mail {
        private String from;
    }

    @Data
    public static class Frontend {
        private String resetPasswordUrl;
    }
}