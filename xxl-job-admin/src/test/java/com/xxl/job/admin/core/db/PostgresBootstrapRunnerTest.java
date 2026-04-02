package com.xxl.job.admin.core.db;

import com.xxl.job.admin.core.conf.DbBootstrapProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PostgresBootstrapRunnerTest {

    @Test
    void shouldInvokeBootstrapWhenEnabledAndPostgresUrl() throws Exception {
        DbBootstrapProperties properties = new DbBootstrapProperties();
        properties.setEnabled(true);

        PostgresBootstrapService service = mock(PostgresBootstrapService.class);
        PostgresBootstrapRunner runner = new PostgresBootstrapRunner(
                properties,
                service,
                "jdbc:postgresql://127.0.0.1:5432/xxl_job",
                "postgres",
                "secret"
        );

        runner.run(null);

        verify(service).bootstrap(
                "jdbc:postgresql://127.0.0.1:5432/xxl_job",
                "postgres",
                "secret",
                PostgresJdbcUrlParser.parse("jdbc:postgresql://127.0.0.1:5432/xxl_job")
        );
    }

    @Test
    void shouldSkipBootstrapWhenDisabled() throws Exception {
        DbBootstrapProperties properties = new DbBootstrapProperties();
        properties.setEnabled(false);

        PostgresBootstrapService service = mock(PostgresBootstrapService.class);
        PostgresBootstrapRunner runner = new PostgresBootstrapRunner(
                properties,
                service,
                "jdbc:postgresql://127.0.0.1:5432/xxl_job",
                "postgres",
                "secret"
        );

        runner.run(null);

        verify(service, never()).bootstrap("jdbc:postgresql://127.0.0.1:5432/xxl_job", "postgres", "secret", PostgresJdbcUrlParser.parse("jdbc:postgresql://127.0.0.1:5432/xxl_job"));
    }
}
