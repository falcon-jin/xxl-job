package com.xxl.job.admin.core.db;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class PostgresScriptExecutor {

    public void ensureDatabase(Connection connection, String database) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_database WHERE datname = ?")) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    try (Statement createStatement = connection.createStatement()) {
                        createStatement.execute("CREATE DATABASE \"" + database.replace("\"", "\"\"") + "\"");
                    }
                }
            }
        }
    }

    public void ensureSchema(Connection connection, String schema) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?")) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    try (Statement createStatement = connection.createStatement()) {
                        createStatement.execute("CREATE SCHEMA \"" + schema.replace("\"", "\"\"") + "\"");
                    }
                }
            }
        }
    }

    public void setSchema(Connection connection, String schema) throws SQLException {
        connection.setSchema(schema);
    }

    public boolean isInitialized(Connection connection, String schema) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
            statement.setString(1, schema);
            statement.setString(2, "xxl_job_lock");
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void runBootstrapScripts(Connection connection) throws SQLException {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/init/postgres/schema.sql"));
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/init/postgres/data.sql"));
    }
}
