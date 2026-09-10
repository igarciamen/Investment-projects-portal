package com.igarciamen.notifications.service;

import com.igarciamen.notifications.payloads.request.EmailRequest;
import com.igarciamen.notifications.service.impl.EmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private TemplateEngine templateEngine;

    @InjectMocks private EmailServiceImpl emailService;

    @Test
    void sendEmail_processesTheEmailTemplateAndSendsIt() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email"), any(Context.class))).thenReturn("<html>content</html>");

        EmailRequest req = new EmailRequest("promoter@mail.com", "Project status update",
                "Your project has been submitted for evaluation.");

        emailService.sendEmail(req);

        verify(templateEngine).process(eq("email"), any(Context.class));
        verify(javaMailSender).send(mimeMessage);

        System.out.println("=== sendEmail: template processed and message sent ===");
    }

    @Test
    void sendEmail_passesTheMessageTextToTheTemplateContext() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("email"), contextCaptor.capture())).thenReturn("<html>content</html>");

        EmailRequest req = new EmailRequest("admin@mail.com", "New project submitted",
                "A new project has just been submitted for review.");

        emailService.sendEmail(req);

        Context usedContext = contextCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(
                "A new project has just been submitted for review.",
                usedContext.getVariable("message"));

        System.out.println("=== sendEmail: message text passed to the template context ===");
    }

    @Test
    void sendEmail_whenJavaMailSenderFails_propagatesAsRuntimeException() {
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP unreachable"));

        EmailRequest req = new EmailRequest("promoter@mail.com", "Subject", "Message");

        assertThrows(RuntimeException.class, () -> emailService.sendEmail(req));

        System.out.println("=== sendEmail: SMTP failure propagated as RuntimeException ===");
    }
}
