package com.xxl.job.admin;

import com.xxl.job.admin.core.conf.DbBootstrapProperties;
import com.xxl.job.admin.core.db.PostgresBootstrapApplicationContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author xuxueli 2018-10-28 00:38:13
 */
@SpringBootApplication
@EnableConfigurationProperties(DbBootstrapProperties.class)
public class XxlJobAdminApplication {

	public static void main(String[] args) {
        createApplication().run(args);
	}

    static SpringApplication createApplication() {
        SpringApplication application = new SpringApplication(XxlJobAdminApplication.class);
        application.addInitializers(new PostgresBootstrapApplicationContextInitializer());
        return application;
    }

}
