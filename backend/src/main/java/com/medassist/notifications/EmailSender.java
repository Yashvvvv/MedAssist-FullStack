package com.medassist.notifications;

import com.medassist.common.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP email (any provider: Brevo / Resend / Gmail SMTP). Falls back to logging when MAIL_HOST is unset. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmailSender implements NotificationSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean configured;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender, AppProperties app,
                       org.springframework.core.env.Environment env) {
        this.mailSender = mailSender;
        this.from = app.notifications().emailFrom();
        String host = env.getProperty("spring.mail.host");
        this.configured = host != null && !host.isBlank();
    }

    @Override
    public boolean supports(Notification.Channel channel) {
        return channel == Notification.Channel.EMAIL;
    }

    @Override
    public void send(Notification n) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!configured || sender == null) {
            log.info("[email:dev] to={} subject='{}' body='{}'", n.getRecipient(), n.getTitle(), n.getBody());
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(n.getRecipient());
        msg.setSubject(n.getTitle());
        msg.setText(n.getBody());
        sender.send(msg);
    }
}
