package net.huansi.job.core.thread;

import net.huansi.job.core.biz.model.HandleCallbackParam;
import net.huansi.job.core.biz.model.ReturnT;
import net.huansi.job.core.biz.model.TriggerParam;
import net.huansi.job.core.context.HsJobContext;
import net.huansi.job.core.context.HsJobHelper;
import net.huansi.job.core.executor.HsJobExecutor;
import net.huansi.job.core.handler.IJobHandler;
import net.huansi.job.core.log.HsJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;


/**
 * 处理任务线程
 * @author falcon 2016-1-16 19:52:47
 */
public class JobThread extends Thread{
	private static Logger logger = LoggerFactory.getLogger(JobThread.class);

	private int jobId;
	private IJobHandler handler;
	private LinkedBlockingQueue<TriggerParam> triggerQueue;
	// 避免重复触发相同的 TRIGGER_LOG_ID
	private Set<Long> triggerLogIdSet;
	private volatile boolean toStop = false;
	private String stopReason;
	// 任务执行状态
    private boolean running = false;
	// 失败重试次数
	private int idleTimes = 0;


	public JobThread(int jobId, IJobHandler handler) {
		this.jobId = jobId;
		this.handler = handler;
		this.triggerQueue = new LinkedBlockingQueue<TriggerParam>();
		this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<Long>());

		// assign job thread name
		this.setName("xxl-job, JobThread-"+jobId+"-"+System.currentTimeMillis());
	}
	public IJobHandler getHandler() {
		return handler;
	}

    /**
     * 排队的新触发器
     *
     * @param triggerParam
     * @return
     */
	public ReturnT<String> pushTriggerQueue(TriggerParam triggerParam) {
		// avoid repeat
		if (triggerLogIdSet.contains(triggerParam.getLogId())) {
			logger.info(">>>>>>>>>>> repeate trigger job, logId:{}", triggerParam.getLogId());
			return new ReturnT<String>(ReturnT.FAIL_CODE, "repeate trigger job, logId:" + triggerParam.getLogId());
		}

		triggerLogIdSet.add(triggerParam.getLogId());
		triggerQueue.add(triggerParam);
        return ReturnT.SUCCESS;
	}

    /**
     * 结束任务线程
     *
     * @param stopReason
     */
	public void toStop(String stopReason) {
		/**
		 * Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
		 * 在阻塞出抛出InterruptedException异常,但是并不会终止运行的线程本身；
		 * 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
		 */
		this.toStop = true;
		this.stopReason = stopReason;
	}

    /**
     * is running job
     * @return
     */
    public boolean isRunningOrHasQueue() {
        return running || triggerQueue.size()>0;
    }

    @Override
	public void run() {

    	// init
    	try {
			handler.init();
		} catch (Throwable e) {
    		logger.error(e.getMessage(), e);
		}

		// 执行定时任务
		while(!toStop){
			running = false;
			idleTimes++;

            TriggerParam triggerParam = null;
            try {
				// 要检查停止信号，我们需要循环，所以我们不能使用 queue.take()，而不是 poll(timeout)
				triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
				if (triggerParam!=null) {
					running = true;
					idleTimes = 0;
					triggerLogIdSet.remove(triggerParam.getLogId());
                    // 日志文件名称  "logPath/yyyy-MM-dd/9999.log"
					String logFileName = HsJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
					HsJobContext hsJobContext = new HsJobContext(
							triggerParam.getJobId(),
							triggerParam.getExecutorParams(),
							logFileName,
							triggerParam.getBroadcastIndex(),
							triggerParam.getBroadcastTotal());

					//初始化任务上下文
					HsJobContext.setXxlJobContext(hsJobContext);

					// 开始执行任务
					HsJobHelper.log("<br>----------- xxl-job job execute start -----------<br>----------- Param:" + hsJobContext.getJobParam());
					//是否限制任务执行时间
					if (triggerParam.getExecutorTimeout() > 0) {
						// limit timeout
						Thread futureThread = null;
						try {
							FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
								@Override
								public Boolean call() throws Exception {

									// init job context
									HsJobContext.setXxlJobContext(hsJobContext);

									handler.execute();
									return true;
								}
							});
							futureThread = new Thread(futureTask);
							futureThread.start();

							Boolean tempResult = futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
						} catch (TimeoutException e) {

							HsJobHelper.log("<br>----------- xxl-job job execute timeout");
							HsJobHelper.log(e);

							// handle result
							HsJobHelper.handleTimeout("job execute timeout ");
						} finally {
							futureThread.interrupt();
						}
					} else {
						// 执行定时任务
						handler.execute();
					}

					// 无效的执行结果 定时任务执行失败
					if (HsJobContext.getXxlJobContext().getHandleCode() <= 0) {
						HsJobHelper.handleFail("job handle result lost.");
					} else {
						String tempHandleMsg = HsJobContext.getXxlJobContext().getHandleMsg();
						tempHandleMsg = (tempHandleMsg!=null&&tempHandleMsg.length()>50000)
								?tempHandleMsg.substring(0, 50000).concat("...")
								:tempHandleMsg;
						HsJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);
					}
					HsJobHelper.log("<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode="
							+ HsJobContext.getXxlJobContext().getHandleCode()
							+ ", handleMsg = "
							+ HsJobContext.getXxlJobContext().getHandleMsg()
					);

				} else {
					if (idleTimes > 30) {
						if(triggerQueue.size() == 0) {	// avoid concurrent trigger causes jobId-lost
							HsJobExecutor.removeJobThread(jobId, "excutor idel times over limit.");
						}
					}
				}
			} catch (Throwable e) {
				if (toStop) {
					HsJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
				}

				// handle result
				StringWriter stringWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(stringWriter));
				String errorMsg = stringWriter.toString();

				HsJobHelper.handleFail(errorMsg);

				HsJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- xxl-job job execute end(error) -----------");
			} finally {
                if(triggerParam != null) {
                    // callback handler info
                    if (!toStop) {
                        // commonm
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                        		triggerParam.getLogId(),
								triggerParam.getLogDateTime(),
								HsJobContext.getXxlJobContext().getHandleCode(),
								HsJobContext.getXxlJobContext().getHandleMsg() )
						);
                    } else {
                        // is killed
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                        		triggerParam.getLogId(),
								triggerParam.getLogDateTime(),
								HsJobContext.HANDLE_CODE_FAIL,
								stopReason + " [job running, killed]" )
						);
                    }
                }
            }
        }

		//定时任务停止 销毁定时任务
		while(triggerQueue !=null && triggerQueue.size()>0){
			TriggerParam triggerParam = triggerQueue.poll();
			if (triggerParam!=null) {
				// is killed
				TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
						triggerParam.getLogId(),
						triggerParam.getLogDateTime(),
						HsJobContext.HANDLE_CODE_FAIL,
						stopReason + " [job not executed, in the job queue, killed.]")
				);
			}
		}

		// 销毁定时任务
		try {
			handler.destroy();
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}

		logger.info(">>>>>>>>>>> xxl-job JobThread stoped, hashCode:{}", Thread.currentThread());
	}
}
