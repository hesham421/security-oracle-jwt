package com.example.security.multitenancy;

import jakarta.persistence.PrePersist;

public class TenantEntityListener {
    @PrePersist
    public void setTenantBeforeInsert(Object entity) {
        if (entity instanceof TenantScoped ts) {
            if (ts.getTenantId() == null || ts.getTenantId().isBlank()) {
                String tenant = TenantContext.getTenantId();
                if (tenant == null || tenant.isBlank()) tenant = "default";
                ts.setTenantId(tenant);
            }
        }
    }
}
