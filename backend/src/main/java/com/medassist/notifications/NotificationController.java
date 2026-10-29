package com.medassist.notifications;

import com.medassist.common.error.ApiException;
import com.medassist.common.web.PageResponse;
import com.medassist.common.web.Pages;
import com.medassist.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The bell icon: in-app notifications for the logged-in user. Staff see their current
 * org's alerts plus personal ones; patients see order updates from every pharmacy.
 */
@Tag(name = "Notifications", description = "In-app inbox and push device registration")
@RestController
public class NotificationController {

    public record InboxItem(UUID id, String title, String body, String category, UUID orgId, boolean read,
                            Instant createdAt) {
        static InboxItem of(Notification n) {
            return new InboxItem(n.getId(), n.getTitle(), n.getBody(), n.getCategory().name(), n.getOrgId(),
                    n.getReadAt() != null, n.getCreatedAt());
        }
    }

    public record DeviceRequest(@NotBlank @Size(max = 500) String token,
                                @NotBlank @Pattern(regexp = "ANDROID|IOS|WEB") String platform) {
    }

    private final NotificationRepository notifications;
    private final DeviceTokenRepository devices;
    private final Clock clock;

    public NotificationController(NotificationRepository notifications, DeviceTokenRepository devices, Clock clock) {
        this.notifications = notifications;
        this.devices = devices;
        this.clock = clock;
    }

    @GetMapping("/api/v1/notifications")
    @Transactional(readOnly = true)
    public PageResponse<InboxItem> inbox(CurrentUser user, @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "30") int size) {
        return PageResponse.of(notifications.inbox(user.userId(), user.orgId(), Pages.of(page, size)), InboxItem::of);
    }

    @GetMapping("/api/v1/notifications/unread-count")
    @Transactional(readOnly = true)
    public Map<String, Long> unread(CurrentUser user) {
        return Map.of("unread", notifications.unreadCount(user.userId(), user.orgId()));
    }

    @PostMapping("/api/v1/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void markRead(CurrentUser user, @PathVariable UUID id) {
        Notification n = notifications.findById(id)
                .filter(x -> user.userId().equals(x.getUserId()) && x.getChannel() == Notification.Channel.IN_APP)
                .orElseThrow(() -> ApiException.notFound("Notification"));
        if (n.getReadAt() == null) {
            n.setReadAt(clock.instant());
        }
    }

    @PostMapping("/api/v1/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void markAllRead(CurrentUser user) {
        notifications.markAllRead(user.userId(), clock.instant());
    }

    @Operation(summary = "Register this phone for push notifications (FCM token); re-registering moves the token")
    @PostMapping("/api/v1/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void registerDevice(CurrentUser user, @Valid @RequestBody DeviceRequest req) {
        DeviceToken d = devices.findByToken(req.token()).orElseGet(DeviceToken::new);
        d.setToken(req.token());
        d.setPlatform(req.platform());
        d.setUserId(user.userId());   // a shared phone that logs in as someone else follows the new user
        devices.save(d);
    }

    @Operation(summary = "Unregister on logout")
    @DeleteMapping("/api/v1/devices/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unregisterDevice(CurrentUser user, @PathVariable String token) {
        devices.deleteByTokenAndUserId(token, user.userId());
    }
}
