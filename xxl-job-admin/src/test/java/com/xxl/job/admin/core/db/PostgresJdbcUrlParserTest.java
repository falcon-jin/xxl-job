package com.xxl.job.admin.core.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgresJdbcUrlParserTest {

    @Test
    void shouldParseDatabaseOnlyUrl() {
        PostgresJdbcTarget target = PostgresJdbcUrlParser.parse("jdbc:postgresql://127.0.0.1:5432/xxl_job");
        assertEquals("xxl_job", target.getDatabase());
        assertEquals("public", target.getSchema());
    }

    @Test
    void shouldParseCurrentSchemaUrl() {
        PostgresJdbcTarget target = PostgresJdbcUrlParser.parse("jdbc:postgresql://10.118.103.18:5432/mes_ydj?currentSchema=xxl_job");
        assertEquals("mes_ydj", target.getDatabase());
        assertEquals("xxl_job", target.getSchema());
    }
}
