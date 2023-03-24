package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * job log
 * @author falcon 2016-1-12 18:03:06
 */
@Mapper
public interface HsJobLogDao {

	// exist jobId not use jobGroup, not exist use jobGroup
	public List<HsJobLog> pageList(@Param("offset") int offset,
                                   @Param("pagesize") int pagesize,
                                   @Param("jobGroup") int jobGroup,
                                   @Param("jobId") int jobId,
                                   @Param("triggerTimeStart") Date triggerTimeStart,
                                   @Param("triggerTimeEnd") Date triggerTimeEnd,
                                   @Param("logStatus") int logStatus);
	public int pageListCount(@Param("offset") int offset,
							 @Param("pagesize") int pagesize,
							 @Param("jobGroup") int jobGroup,
							 @Param("jobId") int jobId,
							 @Param("triggerTimeStart") Date triggerTimeStart,
							 @Param("triggerTimeEnd") Date triggerTimeEnd,
							 @Param("logStatus") int logStatus);
	
	public HsJobLog load(@Param("id") long id);

	public long save(HsJobLog hsJobLog);

	public int updateTriggerInfo(HsJobLog hsJobLog);

	public int updateHandleInfo(HsJobLog hsJobLog);
	
	public int delete(@Param("jobId") int jobId);

	public Map<String, Object> findLogReport(@Param("from") Date from,
											 @Param("to") Date to);

	public List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
									  @Param("jobId") int jobId,
									  @Param("clearBeforeTime") Date clearBeforeTime,
									  @Param("clearBeforeNum") int clearBeforeNum,
									  @Param("pagesize") int pagesize);
	public int clearLog(@Param("logIds") List<Long> logIds);

	public List<Long> findFailJobLogIds(@Param("pagesize") int pagesize);

	public int updateAlarmStatus(@Param("logId") long logId,
								 @Param("oldAlarmStatus") int oldAlarmStatus,
								 @Param("newAlarmStatus") int newAlarmStatus);

	public List<Long> findLostJobIds(@Param("losedTime") Date losedTime);

}