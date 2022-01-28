package net.huansi.job.admin.core.alarm;

import net.huansi.job.admin.core.model.HsJobInfo;
import net.huansi.job.admin.core.model.HsJobLog;

/**
 * @author falcon 2020-01-19
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean doAlarm(HsJobInfo info, HsJobLog jobLog);

}
