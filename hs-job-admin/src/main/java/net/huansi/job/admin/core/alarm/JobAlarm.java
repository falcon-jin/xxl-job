package net.huansi.job.admin.core.alarm;

import net.huansi.job.admin.core.model.HsJobInfo;
import net.huansi.job.admin.core.model.HsJobLog;

/**
 * 任务报警接口
 * 所有继承栏这个接口的类 在触发警报之后都会调用
 * @author falcon 2020-01-19
 */
public interface JobAlarm {

    /**
     * 任务警报
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean doAlarm(HsJobInfo info, HsJobLog jobLog);

}
