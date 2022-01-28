package net.huansi.job.core.enums;

/**
 * 注册配置
 * Created by falcon on 17/5/10.
 */
public class RegistryConfig {

    //心跳时间
    public static final int BEAT_TIMEOUT = 30;
    //心跳失败重试时间
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType{
        /**
         *  执行器客户端
         */
        EXECUTOR,
        /**
         * 管理员 调度中心
         */
        ADMIN
    }

}
