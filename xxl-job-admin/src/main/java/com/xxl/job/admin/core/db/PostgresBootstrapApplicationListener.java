package com.xxl.job.admin.core.db;

import com.xxl.job.admin.core.conf.DbBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class PostgresBootstrapApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(PostgresBootstrapApplicationListener.class);

    private final PostgresBootstrapService bootstrapService;
    private final boolean useInjectedService;

    public PostgresBootstrapApplicationListener() {
        this.bootstrapService = null;
        this.useInjectedService = false;
    }

    PostgresBootstrapApplicationListener(PostgresBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
        this.useInjectedService = true;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        logger.info("Postgres bootstrap listener triggered.");
        if (!environment.getProperty("xxl.job.db.bootstrap.enabled", Boolean.class, true)) {
            logger.info("Postgres bootstrap is disabled.");
            return;
        }

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        logger.info("Postgres bootstrap datasource url: {}", jdbcUrl);
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql:")) {
            logger.info("Skipping bootstrap because datasource is not PostgreSQL.");
            return;
        }

        DbBootstrapProperties properties = new DbBootstrapProperties();
        properties.setEnabled(true);
        properties.setAdminDatabase(environment.getProperty("xxl.job.db.bootstrap.admin-database", "postgres"));

        PostgresBootstrapService service = useInjectedService
                ? bootstrapService
                : new PostgresBootstrapService(properties, new DriverManagerConnectionFactory(), new PostgresScriptExecutor());

        logger.info("Starting PostgreSQL bootstrap for datasource.");
        service.bootstrap(
                jdbcUrl,
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"),
                PostgresJdbcUrlParser.parse(jdbcUrl)
        );
        logger.info("PostgreSQL bootstrap completed.");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
