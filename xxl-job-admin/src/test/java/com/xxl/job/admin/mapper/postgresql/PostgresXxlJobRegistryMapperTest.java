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
