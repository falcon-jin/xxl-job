package net.huansi.job.adminbiz;

import net.huansi.job.core.biz.AdminBiz;
import net.huansi.job.core.biz.client.AdminBizClient;
import net.huansi.job.core.biz.model.HandleCallbackParam;
import net.huansi.job.core.biz.model.RegistryParam;
import net.huansi.job.core.biz.model.ReturnT;
import net.huansi.job.core.context.HsJobContext;
import net.huansi.job.core.enums.RegistryConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * admin api test
 *
 * @author falcon 2017-07-28 22:14:52
 */
public class AdminBizTest {

    // admin-client
    private static String addressUrl = "http://127.0.0.1:8182/hs-job-admin/";
    private static String accessToken = null;


    @Test
    public void callback() throws Exception {
        AdminBiz adminBiz = new AdminBizClient(addressUrl, accessToken);

        HandleCallbackParam param = new HandleCallbackParam();
        param.setLogId(1);
        param.setHandleCode(HsJobContext.HANDLE_CODE_SUCCESS);

        List<HandleCallbackParam> callbackParamList = Arrays.asList(param);

        ReturnT<String> returnT = adminBiz.callback(callbackParamList);

        assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
    }

    /**
     * registry executor
     *
     * @throws Exception
     */
    @Test
    public void registry() throws Exception {
        AdminBiz adminBiz = new AdminBizClient(addressUrl, accessToken);

        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registry(registryParam);

        assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
    }

    /**
     * registry executor remove
     *
     * @throws Exception
     */
    @Test
    public void registryRemove() throws Exception {
        AdminBiz adminBiz = new AdminBizClient(addressUrl, accessToken);

        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registryRemove(registryParam);

        assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);

    }

}
