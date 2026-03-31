package com.xxl.job.core.openapi.model;

import java.io.Serializable;

/**
 * Request model for executor-side auto job registration.
 * Sent from executor to admin when a {@code @XxlJob} method
 * carries a non-empty {@code cron} attribute.
 *
 * @author auto-generated
 */
public class AutoRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** executor appname – used to locate the job group on the admin side */
    private String appname;

    /** jobhandler name (value of @XxlJob) */
    private String executorHandler;

    /** cron expression */
    private String cron;

    /** job description */
    private String desc;

    public AutoRegisterRequest() {}

    public AutoRegisterRequest(String appname, String executorHandler, String cron, String desc) {
        this.appname = appname;
        this.executorHandler = executorHandler;
        this.cron = cron;
        this.desc = desc;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "AutoRegisterRequest{" +
                "appname='" + appname + '\'' +
                ", executorHandler='" + executorHandler + '\'' +
                ", cron='" + cron + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }
}
