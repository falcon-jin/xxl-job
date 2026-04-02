package com.xxl.job.admin.core.db;

import java.util.Objects;

public class PostgresJdbcTarget {

    private final String database;
    private final String schema;

    public PostgresJdbcTarget(String database, String schema) {
        this.database = database;
        this.schema = schema;
    }

    public String getDatabase() {
        return database;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostgresJdbcTarget)) {
            return false;
        }
        PostgresJdbcTarget that = (PostgresJdbcTarget) o;
        return Objects.equals(database, that.database) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, schema);
    }
}
