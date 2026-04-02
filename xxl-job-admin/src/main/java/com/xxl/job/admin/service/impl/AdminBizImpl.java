package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobGroupMapper;
import com.xxl.job.admin.mapper.XxlJobInfoMapper;
import com.xxl.job.admin.model.XxlJobGroup;
import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.scheduler.config.XxlJobAdminBootstrap;
import com.xxl.job.admin.scheduler.cron.CronExpression;
import com.xxl.job.core.constant.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.openapi.AdminBiz;
import com.xxl.job.core.openapi.model.AutoRegisterRequest;
import com.xxl.job.core.openapi.model.CallbackRequest;
import com.xxl.job.core.openapi.model.RegistryRequest;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.Response;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author xuxueli 2017-07-27 21:54:20
 */
@Service
public class AdminBizImpl implements AdminBiz {
    private static final Logger logger = LoggerFactory.getLogger(AdminBizImpl.class);

    @Resource
    private XxlJobGroupMapper xxlJobGroupMapper;

    @Resource
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Override
    public Response<String> callback(List<CallbackRequest> callbackRequestList) {
        return XxlJobAdminBootstrap.getInstance().getJobCompleteHelper().callback(callbackRequestList);
    }

    @Override
    public Response<String> registry(RegistryRequest registryRequest) {
        return XxlJobAdminBootstrap.getInstance().getJobRegistryHelper().registry(registryRequest);
    }

    @Override
    public Response<String> registryRemove(RegistryRequest registryRequest) {
        return XxlJobAdminBootstrap.getInstance().getJobRegistryHelper().registryRemove(registryRequest);
    }

    @Override
    public Response<String> autoRegisterJob(AutoRegisterRequest req) {
        // valid
        if (req == null
                || StringTool.isBlank(req.getAppname())
                || StringTool.isBlank(req.getExecutorHandler())
                || StringTool.isBlank(req.getCron())) {
            return Response.ofFail("autoRegisterJob: illegal argument.");
        }
        if (!CronExpression.isValidExpression(req.getCron())) {
            return Response.ofFail("autoRegisterJob: invalid cron expression [" + req.getCron() + "].");
        }

        // find job group by appname
        List<XxlJobGroup> groups = xxlJobGroupMapper.findAll();
        XxlJobGroup matchGroup = null;
        for (XxlJobGroup g : groups) {
            if (req.getAppname().equals(g.getAppname())) {
                matchGroup = g;
                break;
            }
        }
        if (matchGroup == null) {
            // auto-create group
            XxlJobGroup newGroup = new XxlJobGroup();
            newGroup.setAppname(req.getAppname());
            newGroup.setTitle(req.getAppname());
            newGroup.setAddressType(0);
            newGroup.setAddressList("");
            newGroup.setUpdateTime(new Date());
            xxlJobGroupMapper.save(newGroup);
            matchGroup = newGroup;
            logger.info("autoRegisterJob: auto-created job group for appname={}", req.getAppname());
        }

        String desc = StringTool.isBlank(req.getDesc()) ? req.getExecutorHandler() : req.getDesc();

        // check if job already exists
        XxlJobInfo existJob = xxlJobInfoMapper.loadByGroupAndHandler(matchGroup.getId(), req.getExecutorHandler());
        if (existJob != null) {
            // update cron and desc only if changed
            if (!req.getCron().equals(existJob.getScheduleConf())
                    || !desc.equals(existJob.getJobDesc())) {
                existJob.setScheduleConf(req.getCron());
                existJob.setJobDesc(desc);
                existJob.setUpdateTime(new Date());
                xxlJobInfoMapper.update(existJob);
                logger.info("autoRegisterJob: updated job handler={}, cron={}", req.getExecutorHandler(), req.getCron());
            }
            return Response.ofSuccess(String.valueOf(existJob.getId()));
        }

        // create new job (stopped by default, triggerStatus=0)
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(matchGroup.getId());
        jobInfo.setJobDesc(desc);
        jobInfo.setAuthor("auto");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf(req.getCron());
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("ROUND");
        jobInfo.setExecutorHandler(req.getExecutorHandler());
        jobInfo.setExecutorParam("");
        jobInfo.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.name());
        jobInfo.setExecutorTimeout(0);
        jobInfo.setExecutorFailRetryCount(0);
        jobInfo.setGlueType(GlueTypeEnum.BEAN.name());
        jobInfo.setGlueSource("");
        jobInfo.setGlueRemark("auto registered");
        jobInfo.setGlueUpdatetime(new Date());
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerLastTime(0);
        jobInfo.setTriggerNextTime(0);
        jobInfo.setAddTime(new Date());
        jobInfo.setUpdateTime(new Date());

        xxlJobInfoMapper.save(jobInfo);
        logger.info("autoRegisterJob: registered new job handler={}, cron={}, id={}", req.getExecutorHandler(), req.getCron(), jobInfo.getId());

        return Response.ofSuccess(String.valueOf(jobInfo.getId()));
    }

}
