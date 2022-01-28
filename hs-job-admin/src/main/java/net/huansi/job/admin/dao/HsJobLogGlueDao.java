package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * job log for glue
 * @author falcon 2016-5-19 18:04:56
 */
@Mapper
public interface HsJobLogGlueDao {
	
	public int save(HsJobLogGlue hsJobLogGlue);
	
	public List<HsJobLogGlue> findByJobId(@Param("jobId") int jobId);

	public int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

	public int deleteByJobId(@Param("jobId") int jobId);
	
}
