package com.medassist.notifications;

import com.medassist.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Both the in-app inbox and the transactional outbox for external channels.
 *
 * <p>Business code only inserts a PENDING row inside its own transaction; the
 * {@link NotificationDispatcher} delivers it later. So a sale never fails because an
 * SMTP server is slow, and a rolled-back sale never sends a message (outbox pattern).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    public enum Channel { IN_APP, EMAIL, PUSH, WHATSAPP, SMS }

    public enum Status { PENDING, SENT, FAILED }

    public enum Category {
        ACCOUNT, REFILL_DUE, ORDER_UPDATE, LOW_STOCK, EXPIRY_ALERT, NEW_ORDER, BILLING, INVITATION
    }

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    /** Email address / phone / device token, depending on the channel. */
    private String recipient;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
