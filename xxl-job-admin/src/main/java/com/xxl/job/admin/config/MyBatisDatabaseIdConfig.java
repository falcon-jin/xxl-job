package com.xxl.job.admin.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class MyBatisDatabaseIdConfig {

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties mappings = new Properties();
        mappings.setProperty("MySQL", "mysql");
        mappings.setProperty("PostgreSQL", "postgresql");
        provider.setProperties(mappings);
        return provider;
    }
}
