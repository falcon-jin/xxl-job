package com.xxl.job.admin.core.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnectionFactory {

    Connection open(String jdbcUrl, String username, String password) throws SQLException;
}
