package com.example.watch_together.auth.service;

import com.example.watch_together.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(mailProperties.getMail().getFrom());
            helper.setTo(toEmail);
            helper.setSubject("WatchTogether password reset");

            String html = """
                    <html>
                        <body>
                            <h2>Password reset</h2>
                            <p>You requested a password reset for your WatchTogether account.</p>
                            <p>Click the link below to set a new password:</p>
                            <p><a href="%s">%s</a></p>
                            <p>If you did not request this, you can ignore this email.</p>
                        </body>
                    </html>
                    """.formatted(resetLink, resetLink);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset email", e);
        }
    }
}