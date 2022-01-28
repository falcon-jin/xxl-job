package net.huansi.job.core.thread;

import net.huansi.job.core.log.HsJobFileAppender;
import net.huansi.job.core.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 任务日志文件清理线程
 *
 * @author falcon 2017-12-29 16:23:43
 */
public class JobLogFileCleanThread {
    private static Logger logger = LoggerFactory.getLogger(JobLogFileCleanThread.class);

    private static JobLogFileCleanThread instance = new JobLogFileCleanThread();
    public static JobLogFileCleanThread getInstance(){
        return instance;
    }

    private Thread localThread;
    private volatile boolean toStop = false;
    public void start(final long logRetentionDays){

        // 文件最少保留三天
        if (logRetentionDays < 2 ) {
            return;
        }

        localThread = new Thread(() -> {
            while (!toStop) {
                try {
                    // 超过日志保留天数，清理日志目录，
                    File[] childDirs = new File(HsJobFileAppender.getLogPath()).listFiles();
                    if (childDirs!=null && childDirs.length>0) {

                        // today
                        Calendar todayCal = Calendar.getInstance();
                        todayCal.set(Calendar.HOUR_OF_DAY,0);
                        todayCal.set(Calendar.MINUTE,0);
                        todayCal.set(Calendar.SECOND,0);
                        todayCal.set(Calendar.MILLISECOND,0);

                        Date todayDate = todayCal.getTime();

                        for (File childFile: childDirs) {

                            // valid
                            if (!childFile.isDirectory()) {
                                continue;
                            }
                            if (childFile.getName().indexOf("-") == -1) {
                                continue;
                            }

                            // file create date
                            Date logFileCreateDate = null;
                            try {
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                logFileCreateDate = simpleDateFormat.parse(childFile.getName());
                            } catch (ParseException e) {
                                logger.error(e.getMessage(), e);
                            }
                            if (logFileCreateDate == null) {
                                continue;
                            }

                            if ((todayDate.getTime()-logFileCreateDate.getTime()) >= logRetentionDays * (24 * 60 * 60 * 1000) ) {
                                FileUtil.deleteRecursively(childFile);
                            }

                        }
                    }

                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }

                }

                try {
                    TimeUnit.DAYS.sleep(1);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, executor JobLogFileCleanThread thread destroy.");

        });
        localThread.setDaemon(true);
        localThread.setName("xxl-job, executor JobLogFileCleanThread");
        localThread.start();
    }

    public void toStop() {
        toStop = true;

        if (localThread == null) {
            return;
        }

        // interrupt and wait
        localThread.interrupt();
        try {
            localThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
