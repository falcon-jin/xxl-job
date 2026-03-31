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
    void findLogReportUsesPortableAggregates() {
        Date now = new Date();
        Date from = new Date(now.getTime() - 1_000);
        Date to = new Date(now.getTime() + 1_000);

        xxlJobLogMapper.save(newLog(now, 200, 0));
        xxlJobLogMapper.save(newLog(now, 200, 200));
        xxlJobLogMapper.save(newLog(now, 500, 500));

        Map<String, Object> report = xxlJobLogMapper.findLogReport(from, to);

        assertThat(report.get("triggerDayCount")).isEqualTo(3L);
        assertThat(report.get("triggerDayCountRunning")).isEqualTo(1L);
        assertThat(report.get("triggerDayCountSuc")).isEqualTo(1L);
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
}
