package com.xxl.job.admin.core.db;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostgresBootstrapApplicationListenerTest {

    @Test
    void shouldBootstrapBeforeContextRefreshWhenPostgresIsConfigured() {
        ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("xxl.job.db.bootstrap.enabled", "true")
                .withProperty("xxl.job.db.bootstrap.admin-database", "postgres")
                .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5432/xxl_job")
                .withProperty("spring.datasource.username", "postgres")
                .withProperty("spring.datasource.password", "secret");

        PostgresBootstrapService bootstrapService = mock(PostgresBootstrapService.class);
        PostgresBootstrapApplicationListener listener = new PostgresBootstrapApplicationListener(bootstrapService);

        listener.onApplicationEvent(mock(ApplicationEnvironmentPreparedEvent.class, invocation -> {
            if ("getEnvironment".equals(invocation.getMethod().getName())) {
                return environment;
            }
            return null;
        }));

        verify(bootstrapService).bootstrap(
                "jdbc:postgresql://127.0.0.1:5432/xxl_job",
                "postgres",
                "secret",
                PostgresJdbcUrlParser.parse("jdbc:postgresql://127.0.0.1:5432/xxl_job")
        );
    }
}
