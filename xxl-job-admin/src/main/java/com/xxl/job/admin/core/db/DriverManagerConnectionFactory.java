package com.xxl.job.admin.core.db;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class DriverManagerConnectionFactory implements JdbcConnectionFactory {

    @Override
    public Connection open(String jdbcUrl, String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
