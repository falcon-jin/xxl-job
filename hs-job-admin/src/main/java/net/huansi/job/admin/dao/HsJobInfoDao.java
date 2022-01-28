package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * job info
 * @author falcon 2016-1-12 18:03:45
 */
@Mapper
public interface HsJobInfoDao {

	public List<HsJobInfo> pageList(@Param("offset") int offset,
                                    @Param("pagesize") int pagesize,
                                    @Param("jobGroup") int jobGroup,
                                    @Param("triggerStatus") int triggerStatus,
                                    @Param("jobDesc") String jobDesc,
                                    @Param("executorHandler") String executorHandler,
                                    @Param("author") String author);
	public int pageListCount(@Param("offset") int offset,
							 @Param("pagesize") int pagesize,
							 @Param("jobGroup") int jobGroup,
							 @Param("triggerStatus") int triggerStatus,
							 @Param("jobDesc") String jobDesc,
							 @Param("executorHandler") String executorHandler,
							 @Param("author") String author);
	
	public int save(HsJobInfo info);

	public HsJobInfo loadById(@Param("id") int id);
	
	public int update(HsJobInfo hsJobInfo);
	
	public int delete(@Param("id") long id);

	public List<HsJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

	public int findAllCount();

	public List<HsJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize );

	public int scheduleUpdate(HsJobInfo hsJobInfo);


}
