package com.xxl.job.admin.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyBatisDatabaseIdConfigTest {

    @Test
    void postgresDriverIsOnClasspath() {
        assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"));
    }

    @Test
    void databaseIdProviderMapsPostgreSqlAndMySql() throws Exception {
        DatabaseIdProvider databaseIdProvider = new MyBatisDatabaseIdConfig().databaseIdProvider();

        DataSource postgres = mock(DataSource.class);
        Connection postgresConnection = mock(Connection.class);
        DatabaseMetaData postgresMetaData = mock(DatabaseMetaData.class);
        when(postgres.getConnection()).thenReturn(postgresConnection);
        when(postgresConnection.getMetaData()).thenReturn(postgresMetaData);
        when(postgresMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        DataSource mysql = mock(DataSource.class);
        Connection mysqlConnection = mock(Connection.class);
        DatabaseMetaData mysqlMetaData = mock(DatabaseMetaData.class);
        when(mysql.getConnection()).thenReturn(mysqlConnection);
        when(mysqlConnection.getMetaData()).thenReturn(mysqlMetaData);
        when(mysqlMetaData.getDatabaseProductName()).thenReturn("MySQL");

        assertThat(databaseIdProvider.getDatabaseId(postgres)).isEqualTo("postgresql");
        assertThat(databaseIdProvider.getDatabaseId(mysql)).isEqualTo("mysql");
    }
}
