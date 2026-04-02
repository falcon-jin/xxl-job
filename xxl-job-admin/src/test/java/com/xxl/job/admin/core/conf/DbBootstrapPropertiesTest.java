package com.xxl.job.admin.core.conf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbBootstrapPropertiesTest {

    @Test
    void shouldBindBootstrapProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("xxl.job.db.bootstrap.enabled", "true")
                .withProperty("xxl.job.db.bootstrap.admin-database", "postgres");

        DbBootstrapProperties properties = Binder.get(environment)
                .bind("xxl.job.db.bootstrap", DbBootstrapProperties.class)
                .orElseGet(DbBootstrapProperties::new);

        assertTrue(properties.isEnabled());
        assertEquals("postgres", properties.getAdminDatabase());
    }
}
