package net.huansi.job.core.executor.impl;

import net.huansi.job.core.executor.HsJobExecutor;
import net.huansi.job.core.glue.GlueFactory;
import net.huansi.job.core.handler.HsJobHandler;
import net.huansi.job.core.handler.annotation.HsJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;


/**
 * springboot执行器
 *
 * @author falcon 2018-11-01 09:24:52
 */
public class HsJobSpringExecutor extends HsJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(HsJobSpringExecutor.class);


    // 在单例对象加载完毕后执行
    @Override
    public void afterSingletonsInstantiated() {


        // 初始化 JobHandler 存储库（用于方法）
        initJobHandlerMethodRepository(applicationContext);

        // 刷新 GlueFactory
        GlueFactory.refreshInstance(1);

        // 启动父类
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 销毁
    @Override
    public void destroy() {
        super.destroy();
    }


    //在spring实例化单例对象后 遍历所有对象 然后遍历所有对象方法 找到加了@HsJob注解的方法 保存下来
    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
        // 初始化定时任务
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(HsJobHandler.class, false, true);
        Arrays.stream(beanDefinitionNames).parallel().forEach(beanDefinitionName->{
            Object bean = applicationContext.getBean(beanDefinitionName);

            Map<Method, HsJob> annotatedMethods = null;   // referred to ：org.springframework.context.event.EventListenerMethodProcessor.processBean
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        (MethodIntrospector.MetadataLookup<HsJob>) method -> AnnotatedElementUtils.findMergedAnnotation(method, HsJob.class));
            } catch (Throwable ex) {
                logger.error("xxl-job method-jobhandler resolve error for bean[" + beanDefinitionName + "].", ex);
            }
            if (!(annotatedMethods==null || annotatedMethods.isEmpty())) {
                for (Map.Entry<Method, HsJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                    Method executeMethod = methodXxlJobEntry.getKey();
                    HsJob hsJob = methodXxlJobEntry.getValue();
                    // 注册定时任务处理方法
                    registJobHandler(hsJob, bean, executeMethod);
                }
            }


        });
    }

    // ---------------------- applicationContext ----------------------
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        HsJobSpringExecutor.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}