package net.huansi.job.core.biz.client;

import net.huansi.job.core.biz.AdminBiz;
import net.huansi.job.core.biz.model.HandleCallbackParam;
import net.huansi.job.core.biz.model.RegistryParam;
import net.huansi.job.core.biz.model.ReturnT;
import net.huansi.job.core.util.HsJobRemotingUtil;

import java.util.List;

/**
 * 注册执行器 客户端操作
 *
 * @author falcon 2017-07-28 22:14:52
 */
public class AdminBizClient implements AdminBiz {

    public AdminBizClient() {
    }
    public AdminBizClient(String addressUrl, String accessToken) {
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
     * “执行器”在接收到任务执行请求后，执行任务，在执行结束之后会将执行结果回调通知“调度中心”：
     * @param callbackParamList
     * @return
     */
    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return HsJobRemotingUtil.postBody(addressUrl+"api/callback", accessToken, timeout, callbackParamList, String.class);
    }
    /**
     * “执行器”注册到“调度中心”：
     * @param registryParam
     * @return
     */
    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return HsJobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }
    /**
     * “调度中心”移除执行器：
     * @param registryParam
     * @return
     */
    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return HsJobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }

}
