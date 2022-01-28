package net.huansi.job.admin.core.route.strategy;

import net.huansi.job.admin.core.route.ExecutorRouter;
import net.huansi.job.core.biz.model.ReturnT;
import net.huansi.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * Created by falcon on 17/3/10.
 */
public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<String>(addressList.get(addressList.size()-1));
    }

}
