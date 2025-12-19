package com.medassist.pharmacy.service;

import com.google.maps.GeoApiContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class GoogleMapsConfig {

    @Value("${google.maps.api-key}")
    private String apiKey;

    @Value("${google.maps.timeout:30}")
    private long timeoutSeconds;

    @Value("${google.maps.max-retries:3}")
    private int maxRetries;

    @Bean
    public GeoApiContext geoApiContext() {
        return new GeoApiContext.Builder()
            .apiKey(apiKey)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .retryTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .maxRetries(maxRetries)
            .build();
    }

    // Getters for configuration properties
    public String getApiKey() {
        return apiKey;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
