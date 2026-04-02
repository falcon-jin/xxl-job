package com.xxl.job.admin;

import com.xxl.job.admin.core.db.PostgresBootstrapApplicationContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XxlJobAdminApplicationTest {

    @Test
    void shouldRegisterPostgresBootstrapInitializer() {
        SpringApplication application = XxlJobAdminApplication.createApplication();
        Collection<ApplicationContextInitializer<?>> initializers = application.getInitializers();

        assertTrue(initializers.stream().anyMatch(PostgresBootstrapApplicationContextInitializer.class::isInstance));
    }
}
