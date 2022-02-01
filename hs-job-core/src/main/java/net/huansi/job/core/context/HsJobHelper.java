package net.huansi.job.core.context;

import net.huansi.job.core.log.HsJobFileAppender;
import net.huansi.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * 工具类
 *
 * @author falcon 2020-11-05
 */
public class HsJobHelper {

    // ---------------------- base info ----------------------

    /**
     * 获取当前任务id
     *
     * @return
     */
    public static long getJobId() {
        HsJobContext hsJobContext = HsJobContext.getXxlJobContext();
        if (hsJobContext == null) {
            return -1;
        }

        return hsJobContext.getJobId();
    }

    /**
     * 获取当前任务参数
     *
     * @return
     */
    public static String getJobParam() {
        HsJobContext hsJobContext = HsJobContext.getXxlJobContext();
        if (hsJobContext == null) {
            return null;
        }

        return hsJobContext.getJobParam();
    }

    // ---------------------- for log ----------------------

    /**
     * 获取当前任务日志文件名称
     *
     * @return
     */
    public static String getJobLogFileName() {
        HsJobContext hsJobContext = HsJobContext.getXxlJobContext();
        if (hsJobContext == null) {
            return null;
        }

        return hsJobContext.getJobLogFileName();
    }

    // ---------------------- for shard ----------------------

    /**
     * 获取当前分享索引
     *
     * @return
     */
    public static int getShardIndex() {
        HsJobContext hsJobContext = HsJobContext.getXxlJobContext();
        if (hsJobContext == null) {
            return -1;
        }

        return hsJobContext.getShardIndex();
    }

    /**
     * 获取当前总分享数
     *
     * @return
     */
    public static int getShardTotal() {
        HsJobContext hsJobContext = HsJobContext.getXxlJobContext();
        if (hsJobContext == null) {
            return -1;
        }

        return hsJobContext.getShardTotal();
    }

    // ---------------------- 日志工具类 ----------------------

    private static Logger logger = LoggerFactory.getLogger("xxl-job logger");

    /**
     * 用正则模式附加日志
     * @param appendLogPattern  like "aaa {} bbb {} ccc"
     * @param appendLogArguments    like "111, true"
     */
    public static boolean log(String appendLogPattern, Object ... appendLogArguments) {

        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * 附加异常堆栈
     *
     * @param e
     */
    public static boolean log(Throwable e) {

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * 追加日志
     *
     * @param callInfo
     * @param appendLog
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        HsJobContext hsJobContext = HsJobContext.getXxlJobContext();
        if (hsJobContext == null) {
            return false;
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("["+ callInfo.getClassName() + "#" + callInfo.getMethodName() +"]").append("-")
                .append("["+ callInfo.getLineNumber() +"]").append("-")
                .append("["+ Thread.currentThread().getName() +"]").append(" ")
                .append(appendLog!=null?appendLog:"");
        String formatAppendLog = stringBuffer.toString();

        // 获取日志文件名添加日志
        String logFileName = hsJobContext.getJobLogFileName();
        if (logFileName!=null && logFileName.trim().length()>0) {
            HsJobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }

    // ---------------------- 处理结果 返回给服务端 ----------------------

    /**
     * 任务处理成功
     *
     * @return
     */
    public static boolean handleSuccess(){
        return handleResult(HsJobContext.HANDLE_CODE_SUCCESS, null);
    }

    /**
     * 处理成功并且有返回值
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleSuccess(String handleMsg) {
        return handleResult(HsJobContext.HANDLE_CODE_SUCCESS, handleMsg);
    }

    /**
     * 处理失败
     *
     * @return
     */
    public static boolean handleFail(){
        return handleResult(HsJobContext.HANDLE_CODE_FAIL, null);
    }

    /**
     * 处理失败并且有失败消息
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleFail(String handleMsg) {
        return handleResult(HsJobContext.HANDLE_CODE_FAIL, handleMsg);
    }

    /**
     * 处理超时
     *
     * @return
     */
    public static boolean handleTimeout(){
        return handleResult(HsJobContext.HANDLE_CODE_TIMEOUT, null);
    }

    /**
     * 处理超时并且有超时消息
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleTimeout(String handleMsg){
        return handleResult(HsJobContext.HANDLE_CODE_TIMEOUT, handleMsg);
    }

    /**
     * 处理结果
     * @param handleCode
     *
     *      200 : success
     *      500 : fail
     *      502 : timeout
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleResult(int handleCode, String handleMsg) {
        HsJobContext hsJobContext = HsJobContext.getXxlJobContext();
        if (hsJobContext == null) {
            return false;
        }

        hsJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            hsJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }


}
