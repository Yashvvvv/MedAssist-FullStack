package com.medassist.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Refuses to run the prod profile with development settings that would be a security
 * hole (the public dev JWT secret lets anyone mint tokens), and warns about settings
 * that silently lose data on a free host.
 */
@Slf4j
@Component
@Profile("prod")
public class ProductionSafetyCheck {

    private final AppProperties app;

    public ProductionSafetyCheck(AppProperties app) {
        this.app = app;
        String secret = app.jwt().secret();
        if (secret == null || secret.contains("dev-only") || secret.contains("not-for-production")) {
            throw new IllegalStateException("JWT_SECRET is the development default. Set a random secret "
                    + "(openssl rand -base64 48) before starting with the prod profile.");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warn() {
        if ("local".equals(app.storage().provider())) {
            log.warn("STORAGE_PROVIDER=local in prod: uploaded prescriptions live on the container disk and are "
                    + "lost on every redeploy. Use STORAGE_PROVIDER=supabase.");
        }
        if (app.demo().enabled()) {
            log.warn("DEMO_DATA=true: the demo pharmacy uses public credentials. Do not store real data here.");
        }
    }
}
