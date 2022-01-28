package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by falcon on 16/9/30.
 */
@Mapper
public interface HsJobGroupDao {

    public List<HsJobGroup> findAll();

    public List<HsJobGroup> findByAddressType(@Param("addressType") int addressType);

    public int save(HsJobGroup hsJobGroup);

    public int update(HsJobGroup hsJobGroup);

    public int remove(@Param("id") int id);

    public HsJobGroup load(@Param("id") int id);

    public List<HsJobGroup> pageList(@Param("offset") int offset,
                                     @Param("pagesize") int pagesize,
                                     @Param("appname") String appname,
                                     @Param("title") String title);

    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("appname") String appname,
                             @Param("title") String title);

}
