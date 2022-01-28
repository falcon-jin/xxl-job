package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobGroup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HsJobGroupDaoTest {

    @Resource
    private HsJobGroupDao hsJobGroupDao;

    @Test
    public void test(){
        List<HsJobGroup> list = hsJobGroupDao.findAll();

        List<HsJobGroup> list2 = hsJobGroupDao.findByAddressType(0);

        HsJobGroup group = new HsJobGroup();
        group.setAppname("setAppName");
        group.setTitle("setTitle");
        group.setAddressType(0);
        group.setAddressList("setAddressList");
        group.setUpdateTime(new Date());

        int ret = hsJobGroupDao.save(group);

        HsJobGroup group2 = hsJobGroupDao.load(group.getId());
        group2.setAppname("setAppName2");
        group2.setTitle("setTitle2");
        group2.setAddressType(2);
        group2.setAddressList("setAddressList2");
        group2.setUpdateTime(new Date());

        int ret2 = hsJobGroupDao.update(group2);

        int ret3 = hsJobGroupDao.remove(group.getId());
    }

}
