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
