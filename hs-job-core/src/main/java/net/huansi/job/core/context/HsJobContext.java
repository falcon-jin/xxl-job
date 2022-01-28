package net.huansi.job.core.context;

/**
 * job上下文
 *
 * @author falcon 2020-05-21
 * [Dear hj]
 */
public class HsJobContext {

    //响应成功状态码
    public static final int HANDLE_CODE_SUCCESS = 200;
    //响应失败状态码
    public static final int HANDLE_CODE_FAIL = 500;
    //响应超时状态码
    public static final int HANDLE_CODE_TIMEOUT = 502;

    // ---------------------- base info ----------------------

    /**
     * 任务id
     */
    private final long jobId;

    /**
     * 任务参数
     */
    private final String jobParam;

    // ---------------------- for log ----------------------

    /**
     * 任务日志文件名称
     */
    private final String jobLogFileName;

    // ---------------------- for shard ----------------------

    /**
     * 分享索引
     */
    private final int shardIndex;

    /**
     * 分享总数
     */
    private final int shardTotal;

    // ---------------------- for handle ----------------------

    /**
     * 处理代码：作业执行结果状态
     *
     *      200 : success
     *      500 : fail
     *      502 : timeout
     *
     */
    private int handleCode;

    /**
     * 处理结果：作业执行的简单日志消息
     */
    private String handleMsg;


    public HsJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;

        this.handleCode = HANDLE_CODE_SUCCESS;  // default success
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public String getJobLogFileName() {
        return jobLogFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    // ---------------------- tool ----------------------

    private static InheritableThreadLocal<HsJobContext> contextHolder = new InheritableThreadLocal<HsJobContext>(); // support for child thread of job handler)

    public static void setXxlJobContext(HsJobContext hsJobContext){
        contextHolder.set(hsJobContext);
    }

    public static HsJobContext getXxlJobContext(){
        return contextHolder.get();
    }

}