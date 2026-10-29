package com.medassist.notifications;

import com.medassist.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A phone's FCM registration token, so push notifications can reach the app. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device_tokens")
public class DeviceToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String platform;
}
