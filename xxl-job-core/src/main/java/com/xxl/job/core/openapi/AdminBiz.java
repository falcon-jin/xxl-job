package com.xxl.job.core.openapi;

import com.xxl.job.core.openapi.model.AutoRegisterRequest;
import com.xxl.job.core.openapi.model.CallbackRequest;
import com.xxl.job.core.openapi.model.RegistryRequest;
import com.xxl.tool.response.Response;

import java.util.List;

/**
 * @author xuxueli 2017-07-27 21:52:49
 */
public interface AdminBiz {


    // ---------------------- callback ----------------------

    /**
     * callback
     *
     * @param callbackRequestList
     * @return
     */
    public Response<String> callback(List<CallbackRequest> callbackRequestList);


    // ---------------------- registry ----------------------

    /**
     * registry
     *
     * @param registryRequest
     * @return
     */
    public Response<String> registry(RegistryRequest registryRequest);

    /**
     * registry remove
     *
     * @param registryRequest
     * @return
     */
    public Response<String> registryRemove(RegistryRequest registryRequest);


    // ---------------------- job operate ----------------------

    // jobAdd

    // jobUpdate

    // jobDelete

    // jobQuery

    // jobStart

    // jobStop

    // jobTrigger

    /**
     * auto register job from executor
     * called on executor startup for every @XxlJob method that has a non-empty cron
     *
     * @param autoRegisterRequest
     * @return
     */
    public Response<String> autoRegisterJob(AutoRegisterRequest autoRegisterRequest);

}
