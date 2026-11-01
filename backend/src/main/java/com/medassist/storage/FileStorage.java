package com.medassist.storage;

/**
 * Private file storage for prescriptions and medicine photos. Files are never public:
 * the API streams them after the tenant check. Implementations: local disk (dev) and
 * Supabase Storage (free tier, prod).
 */
public interface FileStorage {

    void put(String key, byte[] content, String contentType);

    byte[] get(String key);

    void delete(String key);
}
