package com.medassist.notifications;

/**
 * Delivers one notification on one channel. Implementations: {@link EmailSender}
 * (SMTP when configured) and {@link LoggingSender} (WhatsApp/SMS/push until a provider
 * such as Meta Cloud API, MSG91 or FCM is wired in: drop in a bean, nothing else changes).
 */
public interface NotificationSender {

    boolean supports(Notification.Channel channel);

    void send(Notification notification) throws Exception;
}
