# PostgreSQL Dual-Database Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add PostgreSQL support to `xxl-job-admin` while preserving MySQL compatibility, covering runtime SQL selection, schema initialization, docker/docs support, and PostgreSQL regression tests.

**Architecture:** Use MyBatis `databaseId` dispatch so the Java mapper interfaces and service layer stay unchanged while dialect-sensitive SQL is split into MySQL and PostgreSQL XML variants. Add a PostgreSQL DDL script plus Testcontainers-backed Spring Boot integration tests that prove upsert, time arithmetic, pagination, generated key backfill, and row locking behavior.

**Tech Stack:** Java 17, Spring Boot 4, MyBatis, HikariCP, Maven, PostgreSQL JDBC, Testcontainers, JUnit Jupiter

---

**Implementation notes:**

- Follow `@test-driven-development` for all code and SQL changes.
- Use `@systematic-debugging` if a mapper still fails after the intended dialect conversion.
- Finish with `@verification-before-completion` before claiming the migration works.
- The root `pom.xml` sets `maven.test.skip=true`, so every Maven test command in this plan must override it with `-Dmaven.test.skip=false`.

### Task 1: Add PostgreSQL Dependency and MyBatis Dialect Selection

**Files:**
- Modify: `pom.xml`
- Modify: `xxl-job-admin/pom.xml`
- Create: `xxl-job-admin/src/main/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfig.java`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfigTest.java`

**Step 1: Write the failing tests**

```java
package com.xxl.job.admin.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MyBatisDatabaseIdConfigTest {

    @Autowired
    private DatabaseIdProvider databaseIdProvider;

    @Test
    void postgresDriverIsOnClasspath() {
        assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"));
    }

    @Test
    void databaseIdProviderMapsPostgreSqlAndMySql() throws Exception {
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
```

**Step 2: Run the tests to verify they fail**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=MyBatisDatabaseIdConfigTest test
```

Expected:

- `postgresDriverIsOnClasspath` fails with `ClassNotFoundException: org.postgresql.Driver`
- Spring context fails or `databaseIdProvider` is missing because no MyBatis `DatabaseIdProvider` bean exists yet

**Step 3: Write the minimal implementation**

Add dependency management in `pom.xml`:

```xml
<properties>
    <postgresql.version>42.7.7</postgresql.version>
    <testcontainers.version>1.21.0</testcontainers.version>
</properties>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>${postgresql.version}</version>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
```

Add module dependencies in `xxl-job-admin/pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

Create `xxl-job-admin/src/main/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfig.java`:

```java
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
```

**Step 4: Run the tests to verify they pass**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=MyBatisDatabaseIdConfigTest test
```

Expected:

- `BUILD SUCCESS`
- `Tests run: 2, Failures: 0, Errors: 0`

**Step 5: Commit**

```bash
git add pom.xml xxl-job-admin/pom.xml xxl-job-admin/src/main/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfig.java xxl-job-admin/src/test/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfigTest.java
git commit -m "feat: add postgres driver and mybatis database id config"
```

### Task 2: Add PostgreSQL DDL and Spring Boot Test Harness

**Files:**
- Create: `doc/db/tables_xxl_job_postgresql.sql`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/test/postgresql/AbstractPostgresIntegrationTest.java`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/test/postgresql/PostgresGroupMapperSmokeTest.java`

**Step 1: Write the failing test and harness**

Create `AbstractPostgresIntegrationTest.java`:

```java
package com.xxl.job.admin.test.postgresql;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractPostgresIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("xxl_job")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @BeforeAll
    static void initSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.FileSystemResource(
                            Path.of("..", "doc", "db", "tables_xxl_job_postgresql.sql")));
        }
    }
}
```

Create `PostgresGroupMapperSmokeTest.java`:

```java
package com.xxl.job.admin.test.postgresql;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresGroupMapperSmokeTest extends AbstractPostgresIntegrationTest {

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Test
    void postgresSchemaLoadsAndSeedDataIsReadable() {
        List<XxlJobGroup> groups = xxlJobGroupMapper.findAll();

        assertThat(groups).isNotEmpty();
        assertThat(groups).extracting(XxlJobGroup::getAppname)
                .contains("xxl-job-executor-sample");
    }
}
```

**Step 2: Run the test to verify it fails**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresGroupMapperSmokeTest test
```

Expected:

- failure because `doc/db/tables_xxl_job_postgresql.sql` does not exist yet
- or schema initialization fails due missing PostgreSQL DDL

**Step 3: Write the minimal implementation**

Create `doc/db/tables_xxl_job_postgresql.sql` with PostgreSQL-native DDL and seed data:

```sql
CREATE TABLE xxl_job_group (
    id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    app_name varchar(64) NOT NULL,
    title varchar(12) NOT NULL,
    address_type smallint NOT NULL DEFAULT 0,
    address_list text,
    update_time timestamp
);

CREATE TABLE xxl_job_registry (
    id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    registry_group varchar(50) NOT NULL,
    registry_key varchar(255) NOT NULL,
    registry_value varchar(255) NOT NULL,
    update_time timestamp,
    CONSTRAINT xxl_job_registry_i_g_k_v UNIQUE (registry_group, registry_key, registry_value)
);

CREATE TABLE xxl_job_info (
    id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    job_group integer NOT NULL,
    job_desc varchar(255) NOT NULL,
    add_time timestamp,
    update_time timestamp,
    author varchar(64),
    alarm_email varchar(255),
    schedule_type varchar(50) NOT NULL DEFAULT 'NONE',
    schedule_conf varchar(128),
    misfire_strategy varchar(50) NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy varchar(50),
    executor_handler varchar(255),
    executor_param varchar(512),
    executor_block_strategy varchar(50),
    executor_timeout integer NOT NULL DEFAULT 0,
    executor_fail_retry_count integer NOT NULL DEFAULT 0,
    glue_type varchar(50) NOT NULL,
    glue_source text,
    glue_remark varchar(128),
    glue_updatetime timestamp,
    child_jobid varchar(255),
    trigger_status smallint NOT NULL DEFAULT 0,
    trigger_last_time bigint NOT NULL DEFAULT 0,
    trigger_next_time bigint NOT NULL DEFAULT 0
);
```

Continue the same file with `xxl_job_logglue`, `xxl_job_log`, `xxl_job_log_report`, `xxl_job_lock`, and `xxl_job_user`, then insert the same seed rows as the MySQL script. After explicit `id` inserts, reset the identities:

```sql
SELECT setval(pg_get_serial_sequence('xxl_job_group', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_group), 1), true);
SELECT setval(pg_get_serial_sequence('xxl_job_info', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_info), 1), true);
SELECT setval(pg_get_serial_sequence('xxl_job_user', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_user), 1), true);
```

Use PostgreSQL dollar-quoted string literals for multi-line JSON and text seed values.

**Step 4: Run the test to verify it passes**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresGroupMapperSmokeTest test
```

Expected:

- PostgreSQL container starts
- schema script executes successfully
- `BUILD SUCCESS`

**Step 5: Commit**

```bash
git add doc/db/tables_xxl_job_postgresql.sql xxl-job-admin/src/test/java/com/xxl/job/admin/test/postgresql/AbstractPostgresIntegrationTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/test/postgresql/PostgresGroupMapperSmokeTest.java
git commit -m "feat: add postgres schema and integration test harness"
```

### Task 3: Convert Registry Mapper Time Arithmetic and Upsert

**Files:**
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobRegistryMapper.xml`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobRegistryMapperTest.java`

**Step 1: Write the failing tests**

```java
package com.xxl.job.admin.mapper.postgresql;

import com.xxl.job.admin.mapper.XxlJobRegistryMapper;
import com.xxl.job.admin.model.XxlJobRegistry;
import com.xxl.job.admin.test.postgresql.AbstractPostgresIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresXxlJobRegistryMapperTest extends AbstractPostgresIntegrationTest {

    @Resource
    private XxlJobRegistryMapper xxlJobRegistryMapper;

    @Test
    void registrySaveOrUpdateUsesPostgresUpsert() {
        Date first = new Date(System.currentTimeMillis() - 10_000);
        Date second = new Date();

        int insert = xxlJobRegistryMapper.registrySaveOrUpdate("EXECUTOR", "app", "addr", first);
        int update = xxlJobRegistryMapper.registrySaveOrUpdate("EXECUTOR", "app", "addr", second);
        List<XxlJobRegistry> live = xxlJobRegistryMapper.findAll(30, new Date());

        assertThat(insert).isPositive();
        assertThat(update).isPositive();
        assertThat(live).filteredOn(item -> "addr".equals(item.getRegistryValue())).hasSize(1);
    }

    @Test
    void findDeadUsesPostgresIntervalArithmetic() {
        Date oldTime = new Date(System.currentTimeMillis() - 120_000);
        xxlJobRegistryMapper.registrySaveOrUpdate("EXECUTOR", "stale", "stale-addr", oldTime);

        List<Integer> deadIds = xxlJobRegistryMapper.findDead(30, new Date());

        assertThat(deadIds).isNotEmpty();
    }
}
```

**Step 2: Run the tests to verify they fail**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobRegistryMapperTest test
```

Expected:

- PostgreSQL syntax errors near `DATE_ADD`
- PostgreSQL syntax errors near `ON DUPLICATE KEY UPDATE`

**Step 3: Write the minimal implementation**

Update `XxlJobRegistryMapper.xml` so the affected statements are duplicated by `databaseId`.

```xml
<select id="findDead" databaseId="mysql" parameterType="java.util.HashMap" resultType="java.lang.Integer">
    SELECT t.id
    FROM xxl_job_registry AS t
    WHERE t.update_time <![CDATA[ < ]]> DATE_ADD(#{nowTime}, INTERVAL -#{timeout} SECOND)
</select>

<select id="findDead" databaseId="postgresql" parameterType="java.util.HashMap" resultType="java.lang.Integer">
    SELECT t.id
    FROM xxl_job_registry AS t
    WHERE t.update_time <![CDATA[ < ]]> (#{nowTime} - (#{timeout} * INTERVAL '1 second'))
</select>

<select id="findAll" databaseId="mysql" parameterType="java.util.HashMap" resultMap="XxlJobRegistry">
    SELECT <include refid="Base_Column_List" />
    FROM xxl_job_registry AS t
    WHERE t.update_time <![CDATA[ > ]]> DATE_ADD(#{nowTime}, INTERVAL -#{timeout} SECOND)
</select>

<select id="findAll" databaseId="postgresql" parameterType="java.util.HashMap" resultMap="XxlJobRegistry">
    SELECT <include refid="Base_Column_List" />
    FROM xxl_job_registry AS t
    WHERE t.update_time <![CDATA[ > ]]> (#{nowTime} - (#{timeout} * INTERVAL '1 second'))
</select>

<insert id="registrySaveOrUpdate" databaseId="mysql">
    INSERT INTO xxl_job_registry (registry_group, registry_key, registry_value, update_time)
    VALUES (#{registryGroup}, #{registryKey}, #{registryValue}, #{updateTime})
    ON DUPLICATE KEY UPDATE
        update_time = #{updateTime}
</insert>

<insert id="registrySaveOrUpdate" databaseId="postgresql">
    INSERT INTO xxl_job_registry (registry_group, registry_key, registry_value, update_time)
    VALUES (#{registryGroup}, #{registryKey}, #{registryValue}, #{updateTime})
    ON CONFLICT (registry_group, registry_key, registry_value) DO UPDATE
    SET update_time = EXCLUDED.update_time
</insert>
```

Keep `registryDelete` and `removeByRegistryGroupAndKey` shared because they are already portable.

**Step 4: Run the tests to verify they pass**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobRegistryMapperTest test
```

Expected:

- both tests pass
- no PostgreSQL syntax errors in mapper execution

**Step 5: Commit**

```bash
git add xxl-job-admin/src/main/resources/mapper/XxlJobRegistryMapper.xml xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobRegistryMapperTest.java
git commit -m "feat: add postgres registry mapper dialect support"
```

### Task 4: Convert Log Report Upsert and Aggregate Metrics

**Files:**
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogReportMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobLogReportMapperTest.java`

**Step 1: Write the failing tests**

```java
package com.xxl.job.admin.mapper.postgresql;

import com.xxl.job.admin.mapper.XxlJobLogMapper;
import com.xxl.job.admin.mapper.XxlJobLogReportMapper;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.model.XxlJobLogReport;
import com.xxl.job.admin.test.postgresql.AbstractPostgresIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresXxlJobLogReportMapperTest extends AbstractPostgresIntegrationTest {

    @Resource
    private XxlJobLogReportMapper xxlJobLogReportMapper;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Test
    void saveOrUpdateUsesPostgresConflictClause() {
        Date triggerDay = new Date();

        XxlJobLogReport report = new XxlJobLogReport();
        report.setTriggerDay(triggerDay);
        report.setRunningCount(1);
        report.setSucCount(2);
        report.setFailCount(3);
        xxlJobLogReportMapper.saveOrUpdate(report);

        report.setRunningCount(4);
        report.setSucCount(5);
        report.setFailCount(6);
        xxlJobLogReportMapper.saveOrUpdate(report);

        XxlJobLogReport total = xxlJobLogReportMapper.queryLogReportTotal();
        assertThat(total.getRunningCount()).isEqualTo(4);
        assertThat(total.getSucCount()).isEqualTo(5);
        assertThat(total.getFailCount()).isEqualTo(6);
    }

    @Test
    void findLogReportUsesCoalesceOnPostgres() {
        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(1);
        log.setJobId(1);
        log.setTriggerTime(new Date());
        log.setTriggerCode(200);
        log.setHandleCode(0);
        xxlJobLogMapper.save(log);

        Map<String, Object> stats = xxlJobLogMapper.findLogReport(
                new Date(System.currentTimeMillis() - 60_000),
                new Date(System.currentTimeMillis() + 60_000));

        assertThat(stats).containsKeys("triggerDayCount", "triggerDayCountRunning", "triggerDayCountSuc");
        assertThat(((Number) stats.get("triggerDayCount")).intValue()).isPositive();
    }
}
```

**Step 2: Run the tests to verify they fail**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobLogReportMapperTest test
```

Expected:

- PostgreSQL syntax error near `ON DUPLICATE KEY UPDATE`
- PostgreSQL function error for `IFNULL`

**Step 3: Write the minimal implementation**

In `XxlJobLogReportMapper.xml`:

```xml
<insert id="saveOrUpdate" databaseId="mysql" parameterType="com.xxl.job.admin.model.XxlJobLogReport" useGeneratedKeys="true" keyProperty="id" keyColumn="id">
    INSERT INTO xxl_job_log_report (trigger_day, running_count, suc_count, fail_count)
    VALUES (#{triggerDay}, #{runningCount}, #{sucCount}, #{failCount})
    ON DUPLICATE KEY UPDATE
        running_count = #{runningCount},
        suc_count = #{sucCount},
        fail_count = #{failCount}
</insert>

<insert id="saveOrUpdate" databaseId="postgresql" parameterType="com.xxl.job.admin.model.XxlJobLogReport" useGeneratedKeys="true" keyProperty="id" keyColumn="id">
    INSERT INTO xxl_job_log_report (trigger_day, running_count, suc_count, fail_count)
    VALUES (#{triggerDay}, #{runningCount}, #{sucCount}, #{failCount})
    ON CONFLICT (trigger_day) DO UPDATE
    SET running_count = EXCLUDED.running_count,
        suc_count = EXCLUDED.suc_count,
        fail_count = EXCLUDED.fail_count
</insert>
```

In `XxlJobLogMapper.xml`, split `findLogReport`:

```xml
<select id="findLogReport" databaseId="mysql" resultType="java.util.Map">
    SELECT
        IFNULL(COUNT(handle_code), 0) triggerDayCount,
        IFNULL(SUM(CASE WHEN (trigger_code in (0, 200) and handle_code = 0) then 1 else 0 end), 0) as triggerDayCountRunning,
        IFNULL(SUM(CASE WHEN handle_code = 200 then 1 else 0 end), 0) as triggerDayCountSuc
    FROM xxl_job_log
    WHERE trigger_time BETWEEN #{from} and #{to}
</select>

<select id="findLogReport" databaseId="postgresql" resultType="java.util.Map">
    SELECT
        COALESCE(COUNT(handle_code), 0) triggerDayCount,
        COALESCE(SUM(CASE WHEN (trigger_code in (0, 200) and handle_code = 0) then 1 else 0 end), 0) as triggerDayCountRunning,
        COALESCE(SUM(CASE WHEN handle_code = 200 then 1 else 0 end), 0) as triggerDayCountSuc
    FROM xxl_job_log
    WHERE trigger_time BETWEEN #{from} and #{to}
</select>
```

**Step 4: Run the tests to verify they pass**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobLogReportMapperTest test
```

Expected:

- both tests pass
- counts are returned without null handling issues

**Step 5: Commit**

```bash
git add xxl-job-admin/src/main/resources/mapper/XxlJobLogReportMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobLogReportMapperTest.java
git commit -m "feat: add postgres log report dialect support"
```

### Task 5: Convert Pagination and Cleanup SQL Variants

**Files:**
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobGroupMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobInfoMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobUserMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogGlueMapper.xml`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobPaginationMapperTest.java`

**Step 1: Write the failing tests**

```java
package com.xxl.job.admin.mapper.postgresql;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobLogGlueMapper;
import com.xxl.job.admin.mapper.XxlJobLogMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.test.postgresql.AbstractPostgresIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresXxlJobPaginationMapperTest extends AbstractPostgresIntegrationTest {

    @Resource private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource private XxlJobInfoMapper xxlJobInfoMapper;
    @Resource private XxlJobUserMapper xxlJobUserMapper;
    @Resource private XxlJobLogMapper xxlJobLogMapper;
    @Resource private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    @Test
    void pageQueriesUseLimitOffsetSyntax() {
        assertThat(xxlJobGroupMapper.pageList(0, 10, null, null)).isNotNull();
        assertThat(xxlJobInfoMapper.pageList(0, 10, 0, -1, null, null, null)).isNotNull();
        assertThat(xxlJobUserMapper.pageList(0, 10, null, -1)).isNotNull();
        assertThat(xxlJobLogMapper.pageList(0, 10, 1, 1, null, null, -1)).isNotNull();
    }

    @Test
    void cleanupQueriesUsePortableSubqueryLimitSyntax() {
        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(1);
        log.setJobId(1);
        log.setTriggerTime(new Date(System.currentTimeMillis() - 120_000));
        log.setTriggerCode(200);
        log.setHandleCode(200);
        xxlJobLogMapper.save(log);

        List<Long> ids = xxlJobLogMapper.findClearLogIds(1, 1, new Date(), 1, 10);

        assertThat(ids).isNotNull();
        xxlJobLogGlueMapper.removeOld(1, 1);
    }
}
```

**Step 2: Run the tests to verify they fail**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobPaginationMapperTest test
```

Expected:

- PostgreSQL syntax errors near `LIMIT 0, ...`

**Step 3: Write the minimal implementation**

For each affected mapper, duplicate only the pagination-sensitive statements.

Example for `XxlJobGroupMapper.xml`:

```xml
<select id="pageList" databaseId="mysql" parameterType="java.util.HashMap" resultMap="XxlJobGroup">
    SELECT <include refid="Base_Column_List" />
    FROM xxl_job_group AS t
    <trim prefix="WHERE" prefixOverrides="AND | OR">
        <if test="appname != null and appname != ''">
            AND t.app_name like CONCAT(CONCAT('%', #{appname}), '%')
        </if>
        <if test="title != null and title != ''">
            AND t.title like CONCAT(CONCAT('%', #{title}), '%')
        </if>
    </trim>
    ORDER BY t.app_name, t.title, t.id ASC
    LIMIT #{offset}, #{pagesize}
</select>

<select id="pageList" databaseId="postgresql" parameterType="java.util.HashMap" resultMap="XxlJobGroup">
    SELECT <include refid="Base_Column_List" />
    FROM xxl_job_group AS t
    <trim prefix="WHERE" prefixOverrides="AND | OR">
        <if test="appname != null and appname != ''">
            AND t.app_name like CONCAT(CONCAT('%', #{appname}), '%')
        </if>
        <if test="title != null and title != ''">
            AND t.title like CONCAT(CONCAT('%', #{title}), '%')
        </if>
    </trim>
    ORDER BY t.app_name, t.title, t.id ASC
    LIMIT #{pagesize} OFFSET #{offset}
</select>
```

Apply the same pattern to:

- `XxlJobInfoMapper.xml` `pageList`
- `XxlJobUserMapper.xml` `pageList`
- `XxlJobLogMapper.xml` `pageList`
- `XxlJobLogMapper.xml` `findClearLogIds`
- `XxlJobLogMapper.xml` `findFailJobLogIds`
- `XxlJobLogGlueMapper.xml` `removeOld`

For PostgreSQL cleanup queries, remove the `LIMIT 0, #{n}` form and keep `LIMIT #{n}` only.

**Step 4: Run the tests to verify they pass**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobPaginationMapperTest test
```

Expected:

- page queries execute successfully
- cleanup subqueries execute without PostgreSQL syntax errors

**Step 5: Commit**

```bash
git add xxl-job-admin/src/main/resources/mapper/XxlJobGroupMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobInfoMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobUserMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogGlueMapper.xml xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobPaginationMapperTest.java
git commit -m "feat: add postgres pagination and cleanup mapper variants"
```

### Task 6: Verify Generated Key Backfill and Row Locking

**Files:**
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobGroupMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobInfoMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogGlueMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogReportMapper.xml`
- Modify: `xxl-job-admin/src/main/resources/mapper/XxlJobUserMapper.xml`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobGeneratedKeysTest.java`
- Create: `xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobLockMapperTest.java`

**Step 1: Write the failing tests**

```java
package com.xxl.job.admin.mapper.postgresql;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobLogGlueMapper;
import com.xxl.job.admin.mapper.XxlJobLogMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.model.XxlJobLogGlue;
import com.xxl.job.admin.model.XxlJobUser;
import com.xxl.job.admin.test.postgresql.AbstractPostgresIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresXxlJobGeneratedKeysTest extends AbstractPostgresIntegrationTest {

    @Resource private XxlJobGroupMapper xxlJobGroupMapper;
    @Resource private XxlJobInfoMapper xxlJobInfoMapper;
    @Resource private XxlJobLogMapper xxlJobLogMapper;
    @Resource private XxlJobLogGlueMapper xxlJobLogGlueMapper;
    @Resource private XxlJobUserMapper xxlJobUserMapper;

    @Test
    void insertsBackfillIdsOnPostgres() {
        XxlJobGroup group = new XxlJobGroup();
        group.setAppname("pg-group");
        group.setTitle("pg");
        group.setAddressType(0);
        group.setUpdateTime(new Date());
        xxlJobGroupMapper.save(group);
        assertThat(group.getId()).isNotNull();

        XxlJobInfo info = new XxlJobInfo();
        info.setJobGroup(group.getId());
        info.setJobDesc("pg-job");
        info.setAuthor("pg");
        info.setScheduleType("NONE");
        info.setMisfireStrategy("DO_NOTHING");
        info.setGlueType("BEAN");
        info.setAddTime(new Date());
        info.setUpdateTime(new Date());
        info.setGlueUpdatetime(new Date());
        xxlJobInfoMapper.save(info);
        assertThat(info.getId()).isNotNull();

        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(group.getId());
        log.setJobId(info.getId());
        log.setTriggerTime(new Date());
        log.setTriggerCode(200);
        log.setHandleCode(0);
        xxlJobLogMapper.save(log);
        assertThat(log.getId()).isNotNull();

        XxlJobLogGlue glue = new XxlJobLogGlue();
        glue.setJobId(info.getId());
        glue.setGlueType("BEAN");
        glue.setGlueRemark("pg");
        glue.setAddTime(new Date());
        glue.setUpdateTime(new Date());
        xxlJobLogGlueMapper.save(glue);
        assertThat(glue.getId()).isNotNull();

        XxlJobUser user = new XxlJobUser();
        user.setUsername("pg-user");
        user.setPassword("secret");
        user.setRole(0);
        xxlJobUserMapper.save(user);
        assertThat(user.getId()).isNotNull();
    }
}
```

```java
package com.xxl.job.admin.mapper.postgresql;

import com.xxl.job.admin.mapper.XxlJobLockMapper;
import com.xxl.job.admin.test.postgresql.AbstractPostgresIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresXxlJobLockMapperTest extends AbstractPostgresIntegrationTest {

    @Resource private XxlJobLockMapper xxlJobLockMapper;
    @Resource private PlatformTransactionManager transactionManager;

    @Test
    void scheduleLockBlocksUntilFirstTransactionCompletes() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        CompletableFuture<Void> first = CompletableFuture.runAsync(() ->
                template.executeWithoutResult(status -> {
                    xxlJobLockMapper.scheduleLock();
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));

        Thread.sleep(250);

        long start = System.nanoTime();
        template.executeWithoutResult(status -> xxlJobLockMapper.scheduleLock());
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        first.get(5, TimeUnit.SECONDS);
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(1000);
    }
}
```

**Step 2: Run the tests to verify they fail**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobGeneratedKeysTest,PostgresXxlJobLockMapperTest test
```

Expected:

- one or more insert tests fail if PostgreSQL does not backfill generated keys with the current mapper metadata
- lock test may expose transaction or blocking issues

**Step 3: Write the minimal implementation**

Ensure every generated-key insert explicitly declares `keyColumn="id"` alongside the existing `useGeneratedKeys="true"` and `keyProperty="id"`.

Example:

```xml
<insert id="save" parameterType="com.xxl.job.admin.model.XxlJobGroup"
        useGeneratedKeys="true" keyProperty="id" keyColumn="id">
    INSERT INTO xxl_job_group (app_name, title, address_type, address_list, update_time)
    VALUES (#{appname}, #{title}, #{addressType}, #{addressList}, #{updateTime})
</insert>
```

Apply the same `keyColumn="id"` addition to:

- `XxlJobGroupMapper.xml` `save`
- `XxlJobInfoMapper.xml` `save`
- `XxlJobLogMapper.xml` `save`
- `XxlJobLogGlueMapper.xml` `save`
- `XxlJobLogReportMapper.xml` `saveOrUpdate`
- `XxlJobUserMapper.xml` `save`

Do not change `XxlJobLockMapper.xml` unless the locking test proves the SQL itself is wrong.

**Step 4: Run the tests to verify they pass**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresXxlJobGeneratedKeysTest,PostgresXxlJobLockMapperTest test
```

Expected:

- all insert IDs are backfilled
- lock test shows the second transaction waits for the first lock holder

**Step 5: Commit**

```bash
git add xxl-job-admin/src/main/resources/mapper/XxlJobGroupMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobInfoMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogGlueMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogReportMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobUserMapper.xml xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobGeneratedKeysTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobLockMapperTest.java
git commit -m "feat: verify postgres generated keys and row locking"
```

### Task 7: Update Runtime Configuration, Docker, and Documentation

**Files:**
- Modify: `xxl-job-admin/src/main/resources/application.properties`
- Modify: `docker-compose.yml`
- Modify: `README.md`
- Modify: `doc/XXL-JOB官方文档.md`
- Modify: `doc/XXL-JOB-English-Documentation.md`

**Step 1: Write the failing verification checks**

Run the current compose validation and note the missing PostgreSQL path:

```bash
docker compose config
```

Expected:

- the file validates structurally
- but there is no PostgreSQL service, no PostgreSQL admin example, and no documentation describing PostgreSQL startup

**Step 2: Write the minimal implementation**

Update `xxl-job-admin/src/main/resources/application.properties` comments so both databases are documented. Keep the existing MySQL sample as the default runtime value, and add a PostgreSQL example block like:

```properties
### xxl-job, datasource
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=root_pwd
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

### postgresql example
# spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/xxl_job
# spring.datasource.username=postgres
# spring.datasource.password=postgres_pwd
# spring.datasource.driver-class-name=org.postgresql.Driver
```

Update `docker-compose.yml` to add a PostgreSQL service and a second admin service example or commented alternative:

```yaml
postgresql:
  image: postgres:16-alpine
  container_name: xxl-job-postgresql
  environment:
    POSTGRES_DB: xxl_job
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres_pwd
  ports:
    - "5432:5432"
  volumes:
    - ./doc/db/tables_xxl_job_postgresql.sql:/docker-entrypoint-initdb.d/tables_xxl_job_postgresql.sql:ro
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres -d xxl_job"]
    timeout: 20s
    retries: 10
```

Add a PostgreSQL admin environment example:

```yaml
PARAMS: >-
  --spring.datasource.url=jdbc:postgresql://postgresql:5432/xxl_job
  --spring.datasource.username=postgres
  --spring.datasource.password=postgres_pwd
  --spring.datasource.driver-class-name=org.postgresql.Driver
```

Update `README.md`, `doc/XXL-JOB官方文档.md`, and `doc/XXL-JOB-English-Documentation.md` so each includes:

- supported databases: MySQL and PostgreSQL
- which schema script to use for each
- datasource examples for each
- docker startup notes for PostgreSQL

**Step 3: Run the verification checks**

Run:

```bash
docker compose config
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=PostgresGroupMapperSmokeTest,PostgresXxlJobRegistryMapperTest,PostgresXxlJobLogReportMapperTest,PostgresXxlJobPaginationMapperTest,PostgresXxlJobGeneratedKeysTest,PostgresXxlJobLockMapperTest test
```

Expected:

- `docker compose config` succeeds
- representative PostgreSQL tests still pass after config and documentation changes

**Step 4: Commit**

```bash
git add xxl-job-admin/src/main/resources/application.properties docker-compose.yml README.md doc/XXL-JOB官方文档.md doc/XXL-JOB-English-Documentation.md
git commit -m "docs: add postgres runtime and deployment guidance"
```

### Task 8: Final Verification and Integration Check

**Files:**
- Verify: `pom.xml`
- Verify: `xxl-job-admin/pom.xml`
- Verify: `xxl-job-admin/src/main/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfig.java`
- Verify: `doc/db/tables_xxl_job_postgresql.sql`
- Verify: `xxl-job-admin/src/main/resources/mapper/XxlJobRegistryMapper.xml`
- Verify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogReportMapper.xml`
- Verify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml`
- Verify: `xxl-job-admin/src/main/resources/mapper/XxlJobGroupMapper.xml`
- Verify: `xxl-job-admin/src/main/resources/mapper/XxlJobInfoMapper.xml`
- Verify: `xxl-job-admin/src/main/resources/mapper/XxlJobLogGlueMapper.xml`
- Verify: `xxl-job-admin/src/main/resources/mapper/XxlJobUserMapper.xml`
- Verify: `xxl-job-admin/src/main/resources/application.properties`
- Verify: `docker-compose.yml`
- Verify: `README.md`
- Verify: `doc/XXL-JOB官方文档.md`
- Verify: `doc/XXL-JOB-English-Documentation.md`

**Step 1: Run the full targeted verification suite**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -Dtest=MyBatisDatabaseIdConfigTest,PostgresGroupMapperSmokeTest,PostgresXxlJobRegistryMapperTest,PostgresXxlJobLogReportMapperTest,PostgresXxlJobPaginationMapperTest,PostgresXxlJobGeneratedKeysTest,PostgresXxlJobLockMapperTest test
```

Expected:

- `BUILD SUCCESS`
- all targeted PostgreSQL regression tests pass

**Step 2: Run a package build without skipping compilation**

Run:

```bash
mvn -pl xxl-job-admin -am -Dmaven.test.skip=false -DskipTests package
```

Expected:

- `BUILD SUCCESS`
- `xxl-job-admin` packages cleanly with the new dependencies and MyBatis configuration

**Step 3: Run docker validation one last time**

Run:

```bash
docker compose config
```

Expected:

- merged compose config renders successfully

**Step 4: Commit**

```bash
git add pom.xml xxl-job-admin/pom.xml xxl-job-admin/src/main/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfig.java doc/db/tables_xxl_job_postgresql.sql xxl-job-admin/src/main/resources/mapper/XxlJobRegistryMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogReportMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobGroupMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobInfoMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobLogGlueMapper.xml xxl-job-admin/src/main/resources/mapper/XxlJobUserMapper.xml xxl-job-admin/src/main/resources/application.properties docker-compose.yml README.md doc/XXL-JOB官方文档.md doc/XXL-JOB-English-Documentation.md xxl-job-admin/src/test/java/com/xxl/job/admin/config/MyBatisDatabaseIdConfigTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/test/postgresql/AbstractPostgresIntegrationTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/test/postgresql/PostgresGroupMapperSmokeTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobRegistryMapperTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobLogReportMapperTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobPaginationMapperTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobGeneratedKeysTest.java xxl-job-admin/src/test/java/com/xxl/job/admin/mapper/postgresql/PostgresXxlJobLockMapperTest.java
git commit -m "feat: add dual mysql and postgres support"
```
