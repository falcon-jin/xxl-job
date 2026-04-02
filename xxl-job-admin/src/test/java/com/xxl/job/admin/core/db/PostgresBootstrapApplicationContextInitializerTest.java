package com.xxl.job.admin.core.db;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresBootstrapApplicationContextInitializerTest {

    @Test
    void shouldBootstrapBeforeContextRefreshWhenPostgresIsConfigured() {
        ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("xxl.job.db.bootstrap.enabled", "true")
                .withProperty("xxl.job.db.bootstrap.admin-database", "postgres")
                .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5432/xxl_job")
                .withProperty("spring.datasource.username", "postgres")
                .withProperty("spring.datasource.password", "secret");

        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(environment);

        PostgresBootstrapService bootstrapService = mock(PostgresBootstrapService.class);
        PostgresBootstrapApplicationContextInitializer initializer =
                new PostgresBootstrapApplicationContextInitializer(bootstrapService);

        initializer.initialize(context);

        verify(bootstrapService).bootstrap(
                "jdbc:postgresql://127.0.0.1:5432/xxl_job",
                "postgres",
                "secret",
                PostgresJdbcUrlParser.parse("jdbc:postgresql://127.0.0.1:5432/xxl_job")
        );
    }
}
