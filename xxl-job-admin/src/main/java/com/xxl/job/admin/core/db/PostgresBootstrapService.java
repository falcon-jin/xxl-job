package com.xxl.job.admin.core.db;

import com.xxl.job.admin.core.conf.DbBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;

@Service
public class PostgresBootstrapService {

    private static final Logger logger = LoggerFactory.getLogger(PostgresBootstrapService.class);

    private final DbBootstrapProperties properties;
    private final JdbcConnectionFactory connectionFactory;
    private final PostgresScriptExecutor scriptExecutor;

    public PostgresBootstrapService(DbBootstrapProperties properties,
                                    JdbcConnectionFactory connectionFactory,
                                    PostgresScriptExecutor scriptExecutor) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
        this.scriptExecutor = scriptExecutor;
    }

    public void bootstrap(String jdbcUrl, String username, String password, PostgresJdbcTarget target) {
        String adminJdbcUrl = buildAdminJdbcUrl(jdbcUrl, properties.getAdminDatabase());
        String targetJdbcUrl = stripQuery(jdbcUrl);
        logger.info("Ensuring PostgreSQL database exists. adminUrl={}, targetDatabase={}", adminJdbcUrl, target.getDatabase());
        try (Connection adminConnection = connectionFactory.open(adminJdbcUrl, username, password)) {
            scriptExecutor.ensureDatabase(adminConnection, target.getDatabase());
        } catch (Exception e) {
            throw new PostgresBootstrapException("Failed to ensure PostgreSQL database bootstrap prerequisites", e);
        }

        logger.info("Ensuring PostgreSQL schema exists. targetUrl={}, targetSchema={}", targetJdbcUrl, target.getSchema());
        try (Connection targetConnection = connectionFactory.open(targetJdbcUrl, username, password)) {
            scriptExecutor.ensureSchema(targetConnection, target.getSchema());
            scriptExecutor.setSchema(targetConnection, target.getSchema());
            if (!scriptExecutor.isInitialized(targetConnection, target.getSchema())) {
                logger.info("Schema is not initialized. Running bootstrap SQL scripts.");
                scriptExecutor.runBootstrapScripts(targetConnection);
            } else {
                logger.info("Schema is already initialized. Skipping bootstrap SQL scripts.");
            }
        } catch (Exception e) {
            throw new PostgresBootstrapException("Failed to initialize XXL-JOB schema objects", e);
        }
    }

    String buildAdminJdbcUrl(String jdbcUrl, String adminDatabase) {
        String baseUrl = stripQuery(jdbcUrl);
        int lastSlash = baseUrl.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl);
        }
        return baseUrl.substring(0, lastSlash + 1) + adminDatabase;
    }

    String stripQuery(String jdbcUrl) {
        int queryIndex = jdbcUrl.indexOf('?');
        return queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
    }
}
