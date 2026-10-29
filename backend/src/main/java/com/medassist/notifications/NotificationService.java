package com.medassist.notifications;

import com.medassist.notifications.Notification.Category;
import com.medassist.notifications.Notification.Channel;
import com.medassist.tenancy.MembershipRepository;
import com.medassist.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Business-facing API for sending anything to anyone. Only writes outbox/inbox rows. */
@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final MembershipRepository memberships;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, MembershipRepository memberships, Clock clock) {
        this.repository = repository;
        this.memberships = memberships;
        this.clock = clock;
    }

    @Transactional
    public void email(UUID orgId, UUID userId, String to, String subject, String body, Category category) {
        save(orgId, userId, null, Channel.EMAIL, to, subject, body, category, clock.instant());
    }

    @Transactional
    public void inApp(UUID orgId, UUID userId, String title, String body, Category category) {
        Notification n = build(orgId, userId, null, Channel.IN_APP, null, title, body, category, clock.instant());
        n.setStatus(Notification.Status.SENT);
        n.setSentAt(clock.instant());
        repository.save(n);
    }

    /** Message to a pharmacy's customer (patient) on their phone: WhatsApp first, SMS fallback is the sender's job. */
    @Transactional
    public void toCustomer(UUID orgId, UUID customerId, String phone, String title, String body, Category category) {
        save(orgId, null, customerId, Channel.WHATSAPP, phone, title, body, category, clock.instant());
    }

    /** In-app alert to every active staff member of an org holding one of the roles. */
    @Transactional
    public void toStaff(UUID orgId, List<Role> roles, String title, String body, Category category) {
        for (UUID userId : memberships.findActiveUserIdsWithRoles(orgId, roles)) {
            inApp(orgId, userId, title, body, category);
        }
    }

    private void save(UUID orgId, UUID userId, UUID customerId, Channel channel, String recipient, String title,
                      String body, Category category, Instant when) {
        repository.save(build(orgId, userId, customerId, channel, recipient, title, body, category, when));
    }

    private static Notification build(UUID orgId, UUID userId, UUID customerId, Channel channel, String recipient,
                                      String title, String body, Category category, Instant when) {
        Notification n = new Notification();
        n.setOrgId(orgId);
        n.setUserId(userId);
        n.setCustomerId(customerId);
        n.setChannel(channel);
        n.setRecipient(recipient);
        n.setTitle(truncate(title, 150));
        n.setBody(truncate(body, 1000));
        n.setCategory(category);
        n.setScheduledAt(when);
        return n;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
