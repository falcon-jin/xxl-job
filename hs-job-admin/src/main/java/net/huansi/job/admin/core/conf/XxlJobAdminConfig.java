package net.huansi.job.admin.core.conf;

import net.huansi.job.admin.core.alarm.JobAlarmer;
import net.huansi.job.admin.core.scheduler.HsJobScheduler;
import net.huansi.job.admin.dao.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 配置类
 *
 * @author falcon 2017-04-28
 */

@Component
public class XxlJobAdminConfig implements InitializingBean, DisposableBean {

    private static XxlJobAdminConfig adminConfig = null;
    public static XxlJobAdminConfig getAdminConfig() {
        return adminConfig;
    }


    // ---------------------- XxlJobScheduler ----------------------

    private HsJobScheduler hsJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;

        hsJobScheduler = new HsJobScheduler();
        hsJobScheduler.init();
    }

    @Override
    public void destroy() throws Exception {
        hsJobScheduler.destroy();
    }


    // ---------------------- 任务调度器配置 ----------------------

    // conf
    @Value("${hs.job.i18n}")
    private String i18n;

    @Value("${hs.job.accessToken}")
    private String accessToken;

    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${hs.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    @Value("${hs.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    @Value("${hs.job.logretentiondays}")
    private int logretentiondays;

    // dao, service

    @Resource
    private HsJobLogDao hsJobLogDao;
    @Resource
    private HsJobInfoDao hsJobInfoDao;
    @Resource
    private HsJobRegistryDao hsJobRegistryDao;
    @Resource
    private HsJobGroupDao hsJobGroupDao;
    @Resource
    private HsJobLogReportDao hsJobLogReportDao;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private DataSource dataSource;
    @Resource
    private JobAlarmer jobAlarmer;


    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            return -1;  // Limit greater than or equal to 7, otherwise close
        }
        return logretentiondays;
    }

    public HsJobLogDao getXxlJobLogDao() {
        return hsJobLogDao;
    }

    public HsJobInfoDao getXxlJobInfoDao() {
        return hsJobInfoDao;
    }

    public HsJobRegistryDao getXxlJobRegistryDao() {
        return hsJobRegistryDao;
    }

    public HsJobGroupDao getXxlJobGroupDao() {
        return hsJobGroupDao;
    }

    public HsJobLogReportDao getXxlJobLogReportDao() {
        return hsJobLogReportDao;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public JobAlarmer getJobAlarmer() {
        return jobAlarmer;
    }

}
