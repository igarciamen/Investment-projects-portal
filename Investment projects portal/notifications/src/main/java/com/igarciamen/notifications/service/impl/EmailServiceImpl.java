package com.igarciamen.notifications.service.impl;

import com.igarciamen.notifications.payloads.request.EmailRequest;
import com.igarciamen.notifications.service.IEmailService;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public EmailServiceImpl(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());

            Context context = new Context();
            context.setVariable("message", emailRequest.getMessage());
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending the email: " + e.getMessage(), e);
        }
    }
}
