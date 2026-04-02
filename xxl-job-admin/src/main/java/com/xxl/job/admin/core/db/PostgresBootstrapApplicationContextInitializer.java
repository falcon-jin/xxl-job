package com.xxl.job.admin.core.db;

import com.xxl.job.admin.core.conf.DbBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class PostgresBootstrapApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(PostgresBootstrapApplicationContextInitializer.class);

    private final PostgresBootstrapService bootstrapService;
    private final boolean useInjectedService;

    public PostgresBootstrapApplicationContextInitializer() {
        this.bootstrapService = null;
        this.useInjectedService = false;
    }

    PostgresBootstrapApplicationContextInitializer(PostgresBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
        this.useInjectedService = true;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        logger.info("Postgres bootstrap initializer triggered.");
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
}
