package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * @author falcon 2019-05-04 16:44:59
 */
@Mapper
public interface HsJobUserDao {

	public List<HsJobUser> pageList(@Param("offset") int offset,
                                    @Param("pagesize") int pagesize,
                                    @Param("username") String username,
                                    @Param("role") int role);
	public int pageListCount(@Param("offset") int offset,
							 @Param("pagesize") int pagesize,
							 @Param("username") String username,
							 @Param("role") int role);

	public HsJobUser loadByUserName(@Param("username") String username);

	public int save(HsJobUser hsJobUser);

	public int update(HsJobUser hsJobUser);
	
	public int delete(@Param("id") int id);

}
