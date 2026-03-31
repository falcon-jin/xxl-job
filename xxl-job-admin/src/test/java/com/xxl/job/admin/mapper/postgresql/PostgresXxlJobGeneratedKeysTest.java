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

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Resource
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    @Resource
    private XxlJobUserMapper xxlJobUserMapper;

    @Test
    void insertsBackfillIdsOnPostgres() {
        Date now = new Date();

        XxlJobGroup group = new XxlJobGroup();
        group.setAppname("pg-group");
        group.setTitle("pg");
        group.setAddressType(0);
        group.setAddressList("http://127.0.0.1:9999");
        group.setUpdateTime(now);
        xxlJobGroupMapper.save(group);
        assertThat(group.getId()).isPositive();

        XxlJobInfo info = new XxlJobInfo();
        info.setJobGroup(group.getId());
        info.setJobDesc("pg-job");
        info.setAuthor("pg");
        info.setAlarmEmail("pg@example.com");
        info.setScheduleType("NONE");
        info.setScheduleConf("");
        info.setMisfireStrategy("DO_NOTHING");
        info.setExecutorRouteStrategy("FIRST");
        info.setExecutorHandler("demoJobHandler");
        info.setExecutorParam("");
        info.setExecutorBlockStrategy("SERIAL_EXECUTION");
        info.setExecutorTimeout(0);
        info.setExecutorFailRetryCount(0);
        info.setGlueType("BEAN");
        info.setGlueSource("");
        info.setGlueRemark("pg");
        info.setGlueUpdatetime(now);
        info.setChildJobId("");
        info.setTriggerStatus(0);
        info.setTriggerLastTime(0);
        info.setTriggerNextTime(0);
        info.setAddTime(now);
        info.setUpdateTime(now);
        xxlJobInfoMapper.save(info);
        assertThat(info.getId()).isPositive();

        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(group.getId());
        log.setJobId(info.getId());
        log.setTriggerTime(now);
        log.setTriggerCode(200);
        log.setHandleCode(0);
        xxlJobLogMapper.save(log);
        assertThat(log.getId()).isPositive();

        XxlJobLogGlue glue = new XxlJobLogGlue();
        glue.setJobId(info.getId());
        glue.setGlueType("BEAN");
        glue.setGlueSource("");
        glue.setGlueRemark("pg");
        glue.setAddTime(now);
        glue.setUpdateTime(now);
        xxlJobLogGlueMapper.save(glue);
        assertThat(glue.getId()).isPositive();

        XxlJobUser user = new XxlJobUser();
        user.setUsername("pg-user");
        user.setPassword("secret");
        user.setRole(0);
        user.setPermission("1");
        xxlJobUserMapper.save(user);
        assertThat(user.getId()).isPositive();
    }
}
