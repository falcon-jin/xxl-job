package com.xxl.job.admin.core.db;

import com.xxl.job.admin.core.conf.DbBootstrapProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class PostgresBootstrapRunner implements ApplicationRunner {

    private final DbBootstrapProperties properties;
    private final PostgresBootstrapService bootstrapService;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public PostgresBootstrapRunner(DbBootstrapProperties properties,
                                   PostgresBootstrapService bootstrapService,
                                   @Value("${spring.datasource.url}") String jdbcUrl,
                                   @Value("${spring.datasource.username}") String username,
                                   @Value("${spring.datasource.password}") String password) {
        this.properties = properties;
        this.bootstrapService = bootstrapService;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql:")) {
            return;
        }

        PostgresJdbcTarget target = PostgresJdbcUrlParser.parse(jdbcUrl);
        bootstrapService.bootstrap(jdbcUrl, username, password, target);
    }
}
