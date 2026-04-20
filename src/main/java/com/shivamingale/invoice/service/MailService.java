package com.shivamingale.invoice.service;

import com.shivamingale.invoice.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public MailService(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    public boolean checkConnection() {
        if (!mailProperties.isHealthCheckEnabled()) {
            log.info("Mail health check is disabled");
            return true;
        }

        try {
            if (mailSender instanceof JavaMailSenderImpl sender) {
                Session session = sender.getSession();

                String host = sender.getHost();
                int port = sender.getPort();

                log.info("Checking mail connection to {}:{}", host, port);

                try (Transport transport = session.getTransport()) {
                    transport.connect(host, port, sender.getUsername(), sender.getPassword());
                }

                log.info("Mail connection successful");
                return true;
            }

            log.warn("MailSender is not a JavaMailSenderImpl, attempting basic connection test");
            return true;
        } catch (MessagingException e) {
            log.error("Mail connection failed: {}", e.getMessage());
            throw new MailConnectionException("Failed to connect to mail server", e);
        }
    }

    public String getFromAddress() {
        return mailProperties.getFrom();
    }

    public String getFromName() {
        return mailProperties.getFromName();
    }

    public static class MailConnectionException extends RuntimeException {
        public MailConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}