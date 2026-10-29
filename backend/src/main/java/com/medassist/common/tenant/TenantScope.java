package com.medassist.common.tenant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Runs a unit of work in a fresh transaction bound to one tenant.
 *
 * <p>Needed wherever code that is not tenant-scoped (a consumer placing an order at
 * some pharmacy, a nightly job looping over all pharmacies) must write tenant rows.
 * Because Hibernate fixes the tenant when the session opens, the tenant has to be set
 * <em>before</em> the transaction begins; REQUIRES_NEW guarantees a new session.</p>
 */
@Component
public class TenantScope {

    private final TransactionTemplate tx;

    public TenantScope(PlatformTransactionManager txManager) {
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T inTenant(UUID orgId, Supplier<T> work) {
        return TenantContext.callAs(orgId, () -> tx.execute(status -> work.get()));
    }

    public void runInTenant(UUID orgId, Runnable work) {
        inTenant(orgId, () -> {
            work.run();
            return null;
        });
    }
}
