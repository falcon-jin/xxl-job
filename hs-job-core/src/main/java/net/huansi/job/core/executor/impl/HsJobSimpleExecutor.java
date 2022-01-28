package net.huansi.job.core.executor.impl;

import net.huansi.job.core.executor.HsJobExecutor;
import net.huansi.job.core.handler.annotation.HsJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * xxl-job executor (for frameless)
 *
 * @author falcon 2020-11-05
 */
public class HsJobSimpleExecutor extends HsJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HsJobSimpleExecutor.class);


    private List<Object> xxlJobBeanList = new ArrayList<>();
    public List<Object> getXxlJobBeanList() {
        return xxlJobBeanList;
    }
    public void setXxlJobBeanList(List<Object> xxlJobBeanList) {
        this.xxlJobBeanList = xxlJobBeanList;
    }


    @Override
    public void start() {

        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(xxlJobBeanList);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }


    private void initJobHandlerMethodRepository(List<Object> xxlJobBeanList) {
        if (xxlJobBeanList==null || xxlJobBeanList.size()==0) {
            return;
        }

        // init job handler from method
        for (Object bean: xxlJobBeanList) {
            // method
            Method[] methods = bean.getClass().getDeclaredMethods();
            if (methods.length == 0) {
                continue;
            }
            for (Method executeMethod : methods) {
                HsJob hsJob = executeMethod.getAnnotation(HsJob.class);
                // registry
                registJobHandler(hsJob, bean, executeMethod);
            }

        }

    }

}
