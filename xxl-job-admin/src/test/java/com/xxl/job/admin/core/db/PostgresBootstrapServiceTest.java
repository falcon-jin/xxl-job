package com.xxl.job.admin.core.db;

import com.xxl.job.admin.core.conf.DbBootstrapProperties;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresBootstrapServiceTest {

    @Test
    void shouldCreateSchemaWhenMissingForCurrentSchemaTarget() throws Exception {
        DbBootstrapProperties properties = new DbBootstrapProperties();
        properties.setAdminDatabase("postgres");

        JdbcConnectionFactory connectionFactory = mock(JdbcConnectionFactory.class);
        PostgresScriptExecutor scriptExecutor = mock(PostgresScriptExecutor.class);
        Connection adminConnection = mock(Connection.class);
        Connection targetConnection = mock(Connection.class);

        when(connectionFactory.open(anyString(), anyString(), anyString()))
                .thenReturn(adminConnection, targetConnection);

        PostgresBootstrapService service = new PostgresBootstrapService(properties, connectionFactory, scriptExecutor);
        PostgresJdbcTarget target = PostgresJdbcUrlParser.parse("jdbc:postgresql://10.118.103.18:5432/mes_ydj?currentSchema=xxl_job");

        service.bootstrap("jdbc:postgresql://10.118.103.18:5432/mes_ydj?currentSchema=xxl_job", "postgres", "secret", target);

        verify(connectionFactory).open(eq("jdbc:postgresql://10.118.103.18:5432/postgres"), eq("postgres"), eq("secret"));
        verify(connectionFactory).open(eq("jdbc:postgresql://10.118.103.18:5432/mes_ydj"), eq("postgres"), eq("secret"));
        verify(scriptExecutor).ensureDatabase(adminConnection, "mes_ydj");
        verify(scriptExecutor).ensureSchema(targetConnection, "xxl_job");
        verify(scriptExecutor).runBootstrapScripts(targetConnection);
    }

    @Test
    void shouldSkipBootstrapWhenCoreTableAlreadyExists() throws Exception {
        DbBootstrapProperties properties = new DbBootstrapProperties();
        properties.setAdminDatabase("postgres");

        JdbcConnectionFactory connectionFactory = mock(JdbcConnectionFactory.class);
        PostgresScriptExecutor scriptExecutor = mock(PostgresScriptExecutor.class);
        Connection adminConnection = mock(Connection.class);
        Connection targetConnection = mock(Connection.class);

        when(connectionFactory.open(anyString(), anyString(), anyString()))
                .thenReturn(adminConnection, targetConnection);
        when(scriptExecutor.isInitialized(targetConnection, "public")).thenReturn(true);

        PostgresBootstrapService service = new PostgresBootstrapService(properties, connectionFactory, scriptExecutor);
        PostgresJdbcTarget target = PostgresJdbcUrlParser.parse("jdbc:postgresql://127.0.0.1:5432/xxl_job");

        service.bootstrap("jdbc:postgresql://127.0.0.1:5432/xxl_job", "postgres", "secret", target);

        verify(scriptExecutor).ensureDatabase(adminConnection, "xxl_job");
        verify(scriptExecutor).setSchema(targetConnection, "public");
        verify(scriptExecutor, never()).runBootstrapScripts(targetConnection);
    }
}
