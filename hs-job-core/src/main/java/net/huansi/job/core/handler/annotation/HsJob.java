package net.huansi.job.core.handler.annotation;

import java.lang.annotation.*;

/**
 * annotation for method jobhandler
 * 处理类还需继承HsJobHandler接口 这样可以快速从spring里面获取加了 HsJob 注解的方法
 * 方法不能有参数 可使用HsJobHelper.getJobParam() 获取定时任务设置的参数
 * @author falcon 2019-12-11 20:50:13
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface HsJob {

    /**
     * jobhandler name
     */
    String value();

    /**
     * init handler, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy handler, invoked when JobThread destroy
     */
    String destroy() default "";

}
