package com.xxl.job.admin.core.db;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresBootstrapSqlResourcesTest {

    @Test
    void shouldProvideRuntimeBootstrapSqlWithoutCreateDatabase() throws IOException {
        String schemaSql = StreamUtils.copyToString(
                new ClassPathResource("db/init/postgres/schema.sql").getInputStream(),
                StandardCharsets.UTF_8
        );
        assertFalse(schemaSql.contains("CREATE DATABASE"));
        assertTrue(schemaSql.contains("CREATE TABLE xxl_job_lock"));
    }
}
