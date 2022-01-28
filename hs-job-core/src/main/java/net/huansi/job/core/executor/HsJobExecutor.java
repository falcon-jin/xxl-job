package net.huansi.job.core.executor;

import net.huansi.job.core.biz.AdminBiz;
import net.huansi.job.core.biz.client.AdminBizClient;
import net.huansi.job.core.handler.IJobHandler;
import net.huansi.job.core.handler.annotation.HsJob;
import net.huansi.job.core.handler.impl.MethodJobHandler;
import net.huansi.job.core.log.HsJobFileAppender;
import net.huansi.job.core.server.EmbedServer;
import net.huansi.job.core.thread.JobLogFileCleanThread;
import net.huansi.job.core.thread.JobThread;
import net.huansi.job.core.thread.TriggerCallbackThread;
import net.huansi.job.core.util.IpUtil;
import net.huansi.job.core.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 执行器
 * Created by falcon on 2016/3/2 21:14.
 */
public class HsJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HsJobExecutor.class);

    // ---------------------- param ----------------------
    //调度中心地址
    private String adminAddresses;
    //访问令牌
    private String accessToken;
    //执行器名称
    private String appName;
    //执行器地址 默认使用address注册 ，如果地址为空使用 ip:port 注册
    private String address;
    //执行器ip
    private String ip;
    //执行器端口
    private int port;
    //日志路径
    private String logPath;
    //日志保留时间
    private int logRetentionDays;

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }


    // ---------------------- 执行器开始/关闭 ----------------------
    public void start() throws Exception {

        // 初始化日志路径
        HsJobFileAppender.initLogPath(logPath);

        //初始化调度中心，与调度中心建立连接
        initAdminBizList(adminAddresses, accessToken);


        // 初始化 任务日志文件清理线程
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        // 初始化任务执行结果返回给调度中心线程
        TriggerCallbackThread.getInstance().start();

        // 初始化执行器
        initEmbedServer(address, ip, port, appName, accessToken);
    }
    //销毁执行器
    public void destroy(){
        // 停止执行器服务
        stopEmbedServer();

        // 销毁任务线程
        if (jobThreadRepository.size() > 0) {
            for (Map.Entry<Integer, JobThread> item: jobThreadRepository.entrySet()) {
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                // 等待作业线程推送结果到回调队列
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        logger.error(">>>>>>>>>>> xxl-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
            }
            jobThreadRepository.clear();
        }
        jobHandlerRepository.clear();


        // 销毁 任务日志文件清理线程
        JobLogFileCleanThread.getInstance().toStop();

        // 销毁任务执行结果返回给调度中心线程
        TriggerCallbackThread.getInstance().toStop();

    }


    // ---------------------- 调度中心-执行器 通过tpc调度 ----------------------
    //调度中心
    private static List<AdminBiz> adminBizList;
    //初始化调度中心，与调度中心建立连接
    private void initAdminBizList(String adminAddresses, String accessToken) throws Exception {
        if (adminAddresses!=null && adminAddresses.trim().length()>0) {
            //配置了集群服务
            for (String address: adminAddresses.trim().split(",")) {
                if (address!=null && address.trim().length()>0) {
                    AdminBiz adminBiz = new AdminBizClient(address.trim(), accessToken);

                    if (adminBizList == null) {
                        adminBizList = new ArrayList<AdminBiz>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }
    //获取调度中心列表
    public static List<AdminBiz> getAdminBizList(){
        return adminBizList;
    }

    // ---------------------- 执行器客户端操作 ----------------------
    //执行器服务
    private EmbedServer embedServer = null;
    //初始化执行器
    private void initEmbedServer(String address, String ip, int port, String appname, String accessToken) throws Exception {

        // 查找端口
        port = port>0?port: NetUtil.findAvailablePort(9999);
        ip = (ip!=null&&ip.trim().length()>0)?ip: IpUtil.getIp();

        //执行器地址
        if (address==null || address.trim().length()==0) {
            //配置文件没有配置address 默认使用ip加端口号的方式
            String ip_port_address = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        } else {
            String[] split = address.split(":");
            if (split.length == 3) {
                String replace = split[2].replace("/", "");
                try {
                    port = Integer.parseInt(replace);
                } catch (Exception e) {
                    address = split[0] + ":" + split[1] + ":" + port + "/";
                }
            }

        }

        // 访问令牌
        if (accessToken==null || accessToken.trim().length()==0) {
            logger.warn(">>>>>>>>>>> xxl-job accessToken is empty. To ensure system security, please set the accessToken.");
        }

        // 启动执行器
        embedServer = new EmbedServer();
        embedServer.start(address, port, appname, accessToken);
    }
    //停止执行器
    private void stopEmbedServer() {
        //停止执行器
        if (embedServer != null) {
            try {
                embedServer.stop();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    // ---------------------- 执行器仓库 保存所有任务处理器 ----------------------
    private static ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String, IJobHandler>();
    //通过处理器名称获取处理器
    public static IJobHandler loadJobHandler(String name){
        return jobHandlerRepository.get(name);
    }
    //注册任务处理器
    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler){
        logger.info(">>>>>>>>>>> xxl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }
    //注册任务处理器
    protected void registJobHandler(HsJob hsJob, Object bean, Method executeMethod){
        if (hsJob == null) {
            return;
        }

        String name = hsJob.value();
        //make and simplify the variables since they'll be called several times later
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (name.trim().length() == 0) {
            throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }
        if (loadJobHandler(name) != null) {
            throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }

        executeMethod.setAccessible(true);

        // init and destroy
        Method initMethod = null;
        Method destroyMethod = null;
        //定时任务初始化方法
        if (hsJob.init().trim().length() > 0) {
            try {
                initMethod = clazz.getDeclaredMethod(hsJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler initMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }
        //定时任务结束执行方法
        if (hsJob.destroy().trim().length() > 0) {
            try {
                destroyMethod = clazz.getDeclaredMethod(hsJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }

        // registry jobhandler
        registJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));

    }


    // ---------------------- 执行器线程仓库 ----------------------
    private static ConcurrentMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<Integer, JobThread>();
    //注册任务线程 执行任务
    public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason){
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        logger.info(">>>>>>>>>>> xxl-job regist JobThread success, jobId:{}, handler:{}", new Object[]{jobId, handler});

        JobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);	// putIfAbsent | oh my god, map's put method return the old value!!!
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }

        return newJobThread;
    }
    //移除任务线程 任务执行结束
    public static JobThread removeJobThread(int jobId, String removeOldReason){
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();

            return oldJobThread;
        }
        return null;
    }
    //获取任务执行线程
    public static JobThread loadJobThread(int jobId){
        JobThread jobThread = jobThreadRepository.get(jobId);
        return jobThread;
    }

}
