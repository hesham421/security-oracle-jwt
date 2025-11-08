package com.example.security.multitenancy;
public interface TenantScoped {
    String getTenantId();
    void setTenantId(String tenantId);
}
