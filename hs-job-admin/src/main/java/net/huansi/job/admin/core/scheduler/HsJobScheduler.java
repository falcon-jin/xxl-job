package net.huansi.job.admin.core.scheduler;

import net.huansi.job.admin.core.conf.XxlJobAdminConfig;
import net.huansi.job.admin.core.thread.*;
import net.huansi.job.admin.core.util.I18nUtil;
import net.huansi.job.core.biz.ExecutorBiz;
import net.huansi.job.core.biz.client.ExecutorBizClient;
import net.huansi.job.core.enums.ExecutorBlockStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author falcon 2018-10-28 00:18:17
 */

public class HsJobScheduler {
    private static final Logger logger = LoggerFactory.getLogger(HsJobScheduler.class);


    public void init() throws Exception {
        // 初始化 i18n
        initI18n();

        // 初始化触发器线程池
        JobTriggerPoolHelper.toStart();

        // 启动注册监听线程池
        JobRegistryHelper.getInstance().start();

        //任务执行失败监听
        JobFailMonitorHelper.getInstance().start();

        //任务完成监听
        JobCompleteHelper.getInstance().start();

        // 日志分析报表监听
        JobLogReportHelper.getInstance().start();

        // 启动任务调度池
        JobScheduleHelper.getInstance().start();

        logger.info(">>>>>>>>> init xxl-job admin success.");
    }

    
    public void destroy() throws Exception {

        // 停止调度
        JobScheduleHelper.getInstance().toStop();

        // 日志分析报表监听停止
        JobLogReportHelper.getInstance().toStop();

        // 任务完成监听 停止
        JobCompleteHelper.getInstance().toStop();

        // 任务执行失败监听 停止
        JobFailMonitorHelper.getInstance().toStop();

        // 启动注册监听线程池 停止
        JobRegistryHelper.getInstance().toStop();

        //初始化触发器线程池 停止
        JobTriggerPoolHelper.toStop();

    }

    // ---------------------- I18n ----------------------

    private void initI18n(){
        for (ExecutorBlockStrategyEnum item:ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- executor-client ----------------------
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<String, ExecutorBiz>();
    public static ExecutorBiz getExecutorBiz(String address) throws Exception {
        // 检验配置
        if (address==null || address.trim().length()==0) {
            return null;
        }

        // 加载缓存
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        // 设置缓存
        executorBiz = new ExecutorBizClient(address, XxlJobAdminConfig.getAdminConfig().getAccessToken());

        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }

}
