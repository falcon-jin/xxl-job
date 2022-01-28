package net.huansi.job.core.biz.client;

import net.huansi.job.core.biz.ExecutorBiz;
import net.huansi.job.core.biz.model.*;
import net.huansi.job.core.util.HsJobRemotingUtil;

/**
 * 调度中心向客户端发送请求
 *
 * @author falcon 2017-07-28 22:14:52
 */
public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient() {
    }
    public ExecutorBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl ;
    private String accessToken;
    private int timeout = 3;

    /**
     * 调度中心检测执行器是否在线时使用
     * @return
     */
    @Override
    public ReturnT<String> beat() {
        return HsJobRemotingUtil.postBody(addressUrl+"beat", accessToken, timeout, "", String.class);
    }
    /**
     * 调度中心检测指定执行器上指定任务是否忙碌（运行中）时使用
     * @param idleBeatParam
     * @return
     */
    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam){
        return HsJobRemotingUtil.postBody(addressUrl+"idleBeat", accessToken, timeout, idleBeatParam, String.class);
    }
    /**
     * 触发任务执行
     * @param triggerParam
     * @return
     */
    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return HsJobRemotingUtil.postBody(addressUrl + "run", accessToken, timeout, triggerParam, String.class);
    }
    /**
     * 终止任务
     * @param killParam
     * @return
     */
    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return HsJobRemotingUtil.postBody(addressUrl + "kill", accessToken, timeout, killParam, String.class);
    }
    /**
     * 发送日志
     * @param logParam
     * @return
     */
    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return HsJobRemotingUtil.postBody(addressUrl + "log", accessToken, timeout, logParam, LogResult.class);
    }

}
