package com.xxl.job.admin.core.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xxl.job.db.bootstrap")
public class DbBootstrapProperties {

    private boolean enabled = true;
    private String adminDatabase = "postgres";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdminDatabase() {
        return adminDatabase;
    }

    public void setAdminDatabase(String adminDatabase) {
        this.adminDatabase = adminDatabase;
    }
}
