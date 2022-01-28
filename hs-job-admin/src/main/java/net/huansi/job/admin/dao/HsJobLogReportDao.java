package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * job log
 * @author falcon 2019-11-22
 */
@Mapper
public interface HsJobLogReportDao {

	public int save(HsJobLogReport hsJobLogReport);

	public int update(HsJobLogReport hsJobLogReport);

	public List<HsJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
                                               @Param("triggerDayTo") Date triggerDayTo);

	public HsJobLogReport queryLogReportTotal();

}
