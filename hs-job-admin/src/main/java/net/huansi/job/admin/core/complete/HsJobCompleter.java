package net.huansi.job.admin.core.complete;

import net.huansi.job.admin.core.conf.XxlJobAdminConfig;
import net.huansi.job.admin.core.model.HsJobInfo;
import net.huansi.job.admin.core.model.HsJobLog;
import net.huansi.job.admin.core.thread.JobTriggerPoolHelper;
import net.huansi.job.admin.core.trigger.TriggerTypeEnum;
import net.huansi.job.admin.core.util.I18nUtil;
import net.huansi.job.core.biz.model.ReturnT;
import net.huansi.job.core.context.HsJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * @author falcon 2020-10-30 20:43:10
 */
public class HsJobCompleter {
    private static Logger logger = LoggerFactory.getLogger(HsJobCompleter.class);

    /**
     * common fresh handle entrance (limit only once)
     *
     * @param hsJobLog
     * @return
     */
    public static int updateHandleInfoAndFinish(HsJobLog hsJobLog) {

        // finish
        finishJob(hsJobLog);

        // text最大64kb 避免长度过长
        if (hsJobLog.getHandleMsg().length() > 15000) {
            hsJobLog.setHandleMsg( hsJobLog.getHandleMsg().substring(0, 15000) );
        }

        // fresh handle
        return XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateHandleInfo(hsJobLog);
    }


    /**
     * do somethind to finish job
     */
    private static void finishJob(HsJobLog hsJobLog){

        // 1、handle success, to trigger child job
        String triggerChildMsg = null;
        if (HsJobContext.HANDLE_CODE_SUCCESS == hsJobLog.getHandleCode()) {
            HsJobInfo hsJobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(hsJobLog.getJobId());
            if (hsJobInfo !=null && hsJobInfo.getChildJobId()!=null && hsJobInfo.getChildJobId().trim().length()>0) {
                triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_child_run") +"<<<<<<<<<<< </span><br>";

                String[] childJobIds = hsJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId = (childJobIds[i]!=null && childJobIds[i].trim().length()>0 && isNumeric(childJobIds[i]))?Integer.valueOf(childJobIds[i]):-1;
                    if (childJobId > 0) {

                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

                        // add msg
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i],
                                (triggerChildResult.getCode()==ReturnT.SUCCESS_CODE?I18nUtil.getString("system_success"):I18nUtil.getString("system_fail")),
                                triggerChildResult.getMsg());
                    } else {
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i]);
                    }
                }

            }
        }

        if (triggerChildMsg != null) {
            hsJobLog.setHandleMsg( hsJobLog.getHandleMsg() + triggerChildMsg );
        }

        // 2、fix_delay trigger next
        // on the way

    }

    private static boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
