package com.medassist.common.tenant;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Holds the organization (tenant) the current thread acts for.
 *
 * <ul>
 *   <li>Set per request by {@code TenantContextFilter} from the JWT {@code org} claim.</li>
 *   <li>{@link #ROOT} means "no tenant filter": used by public endpoints, consumers
 *       browsing across pharmacies and scheduled jobs. Code running as ROOT must filter
 *       explicitly and must never write tenant rows (use {@link TenantScope} for that).</li>
 * </ul>
 */
public final class TenantContext {

    /** Sentinel tenant id that tells Hibernate to skip the tenant filter. */
    public static final UUID ROOT = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID currentOrRoot() {
        UUID id = CURRENT.get();
        return id == null ? ROOT : id;
    }

    public static void set(UUID orgId) {
        CURRENT.set(orgId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Runs {@code work} with the given tenant, restoring the previous one afterwards. */
    public static <T> T callAs(UUID orgId, Callable<T> work) {
        UUID previous = CURRENT.get();
        CURRENT.set(orgId);
        try {
            return work.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
