package com.medassist.storage;

import com.medassist.common.config.AppProperties;
import com.medassist.common.error.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Supabase Storage over its REST API, using the service-role key (server side only) and a
 * private bucket. Free tier: 1 GB, plenty for compressed prescription photos.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseFileStorage implements FileStorage {

    private final RestClient client;
    private final String bucket;

    public SupabaseFileStorage(AppProperties app, RestClient.Builder builder) {
        AppProperties.Storage s = app.storage();
        if (s.supabaseUrl() == null || s.supabaseUrl().isBlank() || s.supabaseServiceKey() == null
                || s.supabaseServiceKey().isBlank()) {
            throw new IllegalStateException("SUPABASE_URL and SUPABASE_SERVICE_KEY are required for supabase storage");
        }
        this.bucket = s.bucket();
        this.client = builder
                .baseUrl(s.supabaseUrl() + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + s.supabaseServiceKey())
                .defaultHeader("apikey", s.supabaseServiceKey())
                .build();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        client.post().uri("/object/{bucket}/{key}", bucket, key)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public byte[] get(String key) {
        try {
            return client.get().uri("/object/{bucket}/{key}", bucket, key).retrieve().body(byte[].class);
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest e) {
            throw ApiException.notFound("File");
        }
    }

    @Override
    public void delete(String key) {
        client.delete().uri("/object/{bucket}/{key}", bucket, key).retrieve().toBodilessEntity();
    }
}
