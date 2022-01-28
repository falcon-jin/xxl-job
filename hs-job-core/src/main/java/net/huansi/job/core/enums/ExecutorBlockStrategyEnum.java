package net.huansi.job.core.enums;

/**
 * 阻塞处理策略枚举类
 * Created by falcon on 17/5/9.
 */
public enum ExecutorBlockStrategyEnum {
    //单机串行执行
    SERIAL_EXECUTION("Serial execution"),
    /*CONCURRENT_EXECUTION("并行"),*/
    //丢弃后续调度
    DISCARD_LATER("Discard Later"),
    //覆盖之前调度
    COVER_EARLY("Cover Early");

    private String title;
    private ExecutorBlockStrategyEnum (String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item:ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
