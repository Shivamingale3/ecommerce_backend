package com.shivamingale.ecom.service;

import com.shivamingale.ecom.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class EmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final TemplateEngine templateEngine;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public EmailTemplateService(
            TemplateEngine templateEngine,
            JavaMailSender mailSender,
            MailProperties mailProperties) {
        this.templateEngine = templateEngine;
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    public void sendWelcomeEmail(String to, String firstName, String verificationUrl) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "verificationUrl", verificationUrl,
                "currentYear", LocalDateTime.now().getYear());
        sendTemplateEmail(to, "Welcome to Mirra", "welcome", variables);
    }

    public void sendEmailVerification(String to, String firstName, String verificationUrl) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "verificationUrl", verificationUrl,
                "expiryHours", 24,
                "currentYear", LocalDateTime.now().getYear());
        sendTemplateEmail(to, "Verify Your Email Address", "email-verification", variables);
    }

    public void sendPasswordResetEmail(String to, String firstName, String resetUrl, String expiryHours) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "resetUrl", resetUrl,
                "expiryHours", expiryHours,
                "currentYear", LocalDateTime.now().getYear());
        sendTemplateEmail(to, "Reset Your Password", "password-reset", variables);
    }

    public void sendOrderConfirmation(
            String to,
            String firstName,
            String orderId,
            String orderDate,
            String totalAmount,
            String deliveryDate,
            Map<String, Object> orderItems,
            String currentYear) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "orderId", orderId,
                "orderDate", orderDate,
                "totalAmount", totalAmount,
                "deliveryDate", deliveryDate,
                "currentYear", currentYear,
                "orderItems", orderItems);
        sendTemplateEmail(to, "Order Confirmed - #" + orderId, "order-confirmation", variables);
    }

    public void sendOrderShipped(
            String to,
            String firstName,
            String orderId,
            String trackingNumber,
            String carrier,
            String estimatedDelivery,
            String trackingUrl) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "orderId", orderId,
                "trackingNumber", trackingNumber,
                "carrier", carrier,
                "estimatedDelivery", estimatedDelivery,
                "trackingUrl", trackingUrl,
                "currentYear", LocalDateTime.now().getYear());
        sendTemplateEmail(to, "Your Order Has Shipped - #" + orderId, "order-shipped", variables);
    }

    public void sendContactInquiryReply(
            String to,
            String firstName,
            String inquirySubject,
            String ticketNumber) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "inquirySubject", inquirySubject,
                "ticketNumber", ticketNumber,
                "currentYear", LocalDateTime.now().getYear());
        sendTemplateEmail(to, "We Received Your Inquiry - " + inquirySubject, "contact-reply", variables);
    }

    public void sendOtpEmail(String to, String firstName, String otp, int expiryMinutes) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "otp", otp,
                "expiryMinutes", expiryMinutes,
                "currentYear", LocalDateTime.now().getYear());
        sendTemplateEmail(to, "Your Sign-In Code", "otp", variables);
    }

    private void sendTemplateEmail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> variables) {
        try {
            String htmlContent = renderTemplate(templateName, variables);
            sendHtmlEmail(to, subject, htmlContent);
            log.info("Email sent successfully to {} using template {}", to, templateName);
        } catch (Exception e) {
            log.error("Failed to send email to {} using template {}: {}", to, templateName, e.getMessage());
            throw new EmailSendException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        context.setVariable("currentDate", LocalDateTime.now().format(DATE_FORMATTER));
        return templateEngine.process(templateName, context);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(mailProperties.getFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    public String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }

    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
