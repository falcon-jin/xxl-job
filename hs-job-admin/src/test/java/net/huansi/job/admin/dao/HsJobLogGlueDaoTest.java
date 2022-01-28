package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobLogGlue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HsJobLogGlueDaoTest {

    @Resource
    private HsJobLogGlueDao hsJobLogGlueDao;

    @Test
    public void test(){
        HsJobLogGlue logGlue = new HsJobLogGlue();
        logGlue.setJobId(1);
        logGlue.setGlueType("1");
        logGlue.setGlueSource("1");
        logGlue.setGlueRemark("1");

        logGlue.setAddTime(new Date());
        logGlue.setUpdateTime(new Date());
        int ret = hsJobLogGlueDao.save(logGlue);

        List<HsJobLogGlue> list = hsJobLogGlueDao.findByJobId(1);

        int ret2 = hsJobLogGlueDao.removeOld(1, 1);

        int ret3 = hsJobLogGlueDao.deleteByJobId(1);
    }

}
