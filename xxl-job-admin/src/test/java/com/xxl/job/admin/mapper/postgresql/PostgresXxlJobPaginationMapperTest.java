package com.xxl.job.admin.mapper.postgresql;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.mapper.XxlJobLogGlueMapper;
import com.xxl.job.admin.mapper.XxlJobLogMapper;
import com.xxl.job.admin.mapper.XxlJobUserMapper;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.model.XxlJobLogGlue;
import com.xxl.job.admin.test.postgresql.AbstractPostgresIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresXxlJobPaginationMapperTest extends AbstractPostgresIntegrationTest {

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    @Test
    void pageQueriesUsePostgresLimitOffsetSyntax() {
        assertThat(xxlJobGroupMapper.pageList(0, 10, null, null)).isNotNull();
        assertThat(xxlJobInfoMapper.pageList(0, 10, 0, -1, null, null, null)).isNotNull();
        assertThat(xxlJobUserMapper.pageList(0, 10, null, -1)).isNotNull();
        assertThat(xxlJobLogMapper.pageList(0, 10, 1, 1, null, null, -1)).isNotNull();
    }

    @Test
    void cleanupQueriesUsePortableSubquerySyntax() {
        Date now = new Date();

        xxlJobLogMapper.save(newLog(new Date(now.getTime() - 20_000), 200, 200));
        xxlJobLogMapper.save(newLog(new Date(now.getTime() - 10_000), 500, 500));

        List<Long> clearIds = xxlJobLogMapper.findClearLogIds(1, 1, now, 1, 10);
        List<Long> failIds = xxlJobLogMapper.findFailJobLogIds(10);

        xxlJobLogGlueMapper.save(newGlue(new Date(now.getTime() - 20_000)));
        xxlJobLogGlueMapper.save(newGlue(new Date(now.getTime() - 10_000)));
        int removed = xxlJobLogGlueMapper.removeOld(1, 1);

        assertThat(clearIds).hasSize(1);
        assertThat(failIds).hasSize(1);
        assertThat(removed).isPositive();
        assertThat(xxlJobLogGlueMapper.findByJobId(1)).hasSize(1);
    }

    private XxlJobLog newLog(Date triggerTime, int triggerCode, int handleCode) {
        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(1);
        log.setJobId(1);
        log.setTriggerTime(triggerTime);
        log.setTriggerCode(triggerCode);
        log.setHandleCode(handleCode);
        return log;
    }

    private XxlJobLogGlue newGlue(Date updateTime) {
        XxlJobLogGlue glue = new XxlJobLogGlue();
        glue.setJobId(1);
        glue.setGlueType("BEAN");
        glue.setGlueSource("demo");
        glue.setGlueRemark("demo");
        glue.setAddTime(updateTime);
        glue.setUpdateTime(updateTime);
        return glue;
    }
}
