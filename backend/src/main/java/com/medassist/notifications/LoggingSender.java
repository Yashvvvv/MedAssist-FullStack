package com.medassist.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Placeholder delivery for channels without a provider yet: records the message in the log. */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class LoggingSender implements NotificationSender {

    @Override
    public boolean supports(Notification.Channel channel) {
        return channel != Notification.Channel.IN_APP;
    }

    @Override
    public void send(Notification n) {
        log.info("[{}:dev] to={} title='{}' body='{}'", n.getChannel(), n.getRecipient(), n.getTitle(), n.getBody());
    }
}
