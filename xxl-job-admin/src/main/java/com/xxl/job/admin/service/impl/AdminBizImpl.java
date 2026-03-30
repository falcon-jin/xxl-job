package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.cron.CronExpression;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.thread.JobCompleteHelper;
import com.xxl.job.admin.core.thread.JobRegistryHelper;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.dao.XxlJobLogGlueDao;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.JobInfoParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.glue.GlueTypeEnum;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author xuxueli 2017-07-27 21:54:20
 */
@Service
public class AdminBizImpl implements AdminBiz {

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;
    @Resource
    private XxlJobInfoDao xxlJobInfoDao;
    @Resource
    private XxlJobLogDao xxlJobLogDao;
    @Resource
    private XxlJobLogGlueDao xxlJobLogGlueDao;

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }

    @Override
    public ReturnT<Integer> saveJob(JobInfoParam param) {
        // valid
        if (param.getJobGroup() <= 0) {
            // try to find group by appname
            if (param.getAppname() != null && param.getAppname().trim().length() > 0) {
                XxlJobGroup group = xxlJobGroupDao.findByAppname(param.getAppname().trim());
                if (group == null) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, "jobGroup not found for appname: " + param.getAppname());
                }
                param.setJobGroup(group.getId());
            } else {
                return new ReturnT<>(ReturnT.FAIL_CODE, "jobGroup invalid.");
            }
        }
        if (param.getJobDesc() == null || param.getJobDesc().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "jobDesc is required.");
        }
        if (param.getAuthor() == null || param.getAuthor().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "author is required.");
        }
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(param.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "scheduleType invalid.");
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (param.getScheduleConf() == null || !CronExpression.isValidExpression(param.getScheduleConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "cron expression invalid.");
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE) {
            if (param.getScheduleConf() == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "fixRate conf is required.");
            }
            try {
                int fixSecond = Integer.parseInt(param.getScheduleConf());
                if (fixSecond < 1) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, "fixRate must be >= 1.");
                }
            } catch (Exception e) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "fixRate conf invalid.");
            }
        }
        if (GlueTypeEnum.match(param.getGlueType()) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "glueType invalid.");
        }

        // upsert by jobGroup + executorHandler
        XxlJobInfo exists = xxlJobInfoDao.loadByGroupAndHandler(param.getJobGroup(), param.getExecutorHandler());
        if (exists != null) {
            // update
            long nextTriggerTime = exists.getTriggerNextTime();
            boolean scheduleChanged = !param.getScheduleType().equals(exists.getScheduleType())
                    || !param.getScheduleConf().equals(exists.getScheduleConf());
            if (exists.getTriggerStatus() == 1 && scheduleChanged) {
                try {
                    Date nextValidTime = JobScheduleHelper.generateNextValidTime(
                            buildJobInfo(param, exists), new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                    if (nextValidTime == null) {
                        return new ReturnT<>(ReturnT.FAIL_CODE, "scheduleConf invalid, no next valid time.");
                    }
                    nextTriggerTime = nextValidTime.getTime();
                } catch (Exception e) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, "scheduleConf invalid.");
                }
            }
            exists = buildJobInfo(param, exists);
            exists.setTriggerNextTime(nextTriggerTime);
            exists.setUpdateTime(new Date());
            xxlJobInfoDao.update(exists);
            return new ReturnT<>(exists.getId());
        } else {
            // insert
            XxlJobInfo jobInfo = buildJobInfo(param, new XxlJobInfo());
            jobInfo.setAddTime(new Date());
            jobInfo.setUpdateTime(new Date());
            jobInfo.setGlueUpdatetime(new Date());
            // auto-start: compute next trigger time
            long nextTriggerTime = 0;
            try {
                Date nextValidTime = JobScheduleHelper.generateNextValidTime(
                        jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime != null) {
                    nextTriggerTime = nextValidTime.getTime();
                }
            } catch (Exception e) {
                // ignore, job will remain stopped if cron is somehow invalid
            }
            if (nextTriggerTime > 0) {
                jobInfo.setTriggerStatus(1);
                jobInfo.setTriggerLastTime(0);
                jobInfo.setTriggerNextTime(nextTriggerTime);
            } else {
                jobInfo.setTriggerStatus(0);
                jobInfo.setTriggerLastTime(0);
                jobInfo.setTriggerNextTime(0);
            }
            xxlJobInfoDao.save(jobInfo);
            return new ReturnT<>(jobInfo.getId());
        }
    }

    @Override
    public ReturnT<String> removeJob(int id) {
        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
        if (xxlJobInfo == null) {
            return ReturnT.SUCCESS;
        }
        xxlJobInfoDao.delete(id);
        xxlJobLogDao.delete(id);
        xxlJobLogGlueDao.deleteByJobId(id);
        return ReturnT.SUCCESS;
    }

    private XxlJobInfo buildJobInfo(JobInfoParam param, XxlJobInfo target) {
        target.setJobGroup(param.getJobGroup());
        target.setJobDesc(param.getJobDesc());
        target.setAuthor(param.getAuthor());
        target.setAlarmEmail(param.getAlarmEmail());
        target.setScheduleType(param.getScheduleType());
        target.setScheduleConf(param.getScheduleConf());
        target.setMisfireStrategy(param.getMisfireStrategy());
        target.setExecutorRouteStrategy(param.getExecutorRouteStrategy());
        target.setExecutorHandler(param.getExecutorHandler());
        target.setExecutorParam(param.getExecutorParam());
        target.setExecutorBlockStrategy(param.getExecutorBlockStrategy());
        target.setExecutorTimeout(param.getExecutorTimeout());
        target.setExecutorFailRetryCount(param.getExecutorFailRetryCount());
        target.setGlueType(param.getGlueType());
        target.setChildJobId(param.getChildJobId());
        return target;
    }

}

