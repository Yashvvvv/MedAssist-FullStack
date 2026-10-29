package com.medassist.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Polls the outbox and delivers due messages. Failures retry with exponential backoff
 * (1, 2, 4, 8, 16 min) and give up after {@value #MAX_ATTEMPTS} attempts.
 */
@Slf4j
@Component
public class NotificationDispatcher {

    static final int MAX_ATTEMPTS = 5;
    private static final int BATCH = 50;

    private final NotificationRepository repository;
    private final List<NotificationSender> senders;
    private final Clock clock;

    public NotificationDispatcher(NotificationRepository repository, List<NotificationSender> senders, Clock clock) {
        this.repository = repository;
        this.senders = senders;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.notifications.dispatch-interval:30s}", initialDelayString = "20s")
    @Transactional
    public int dispatchDue() {
        List<Notification> due = repository.claimDue(clock.instant(), PageRequest.of(0, BATCH));
        for (Notification n : due) {
            deliver(n);
        }
        return due.size();
    }

    private void deliver(Notification n) {
        NotificationSender sender = senders.stream().filter(s -> s.supports(n.getChannel())).findFirst().orElse(null);
        n.setAttempts(n.getAttempts() + 1);
        try {
            if (sender == null) {
                throw new IllegalStateException("No sender for channel " + n.getChannel());
            }
            sender.send(n);
            n.setStatus(Notification.Status.SENT);
            n.setSentAt(clock.instant());
            n.setLastError(null);
        } catch (Exception e) {
            log.warn("Delivery failed for notification {} (attempt {}): {}", n.getId(), n.getAttempts(), e.getMessage());
            n.setLastError(e.getMessage() == null ? e.getClass().getSimpleName()
                    : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
            if (n.getAttempts() >= MAX_ATTEMPTS) {
                n.setStatus(Notification.Status.FAILED);
            } else {
                n.setScheduledAt(clock.instant().plus(Duration.ofMinutes(1L << (n.getAttempts() - 1))));
            }
        }
    }
}
