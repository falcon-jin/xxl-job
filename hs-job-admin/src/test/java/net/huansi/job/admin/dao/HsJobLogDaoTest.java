package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobLog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HsJobLogDaoTest {

    @Resource
    private HsJobLogDao hsJobLogDao;

    @Test
    public void test(){
        List<HsJobLog> list = hsJobLogDao.pageList(0, 10, 1, 1, null, null, 1);
        int list_count = hsJobLogDao.pageListCount(0, 10, 1, 1, null, null, 1);

        HsJobLog log = new HsJobLog();
        log.setJobGroup(1);
        log.setJobId(1);

        long ret1 = hsJobLogDao.save(log);
        HsJobLog dto = hsJobLogDao.load(log.getId());

        log.setTriggerTime(new Date());
        log.setTriggerCode(1);
        log.setTriggerMsg("1");
        log.setExecutorAddress("1");
        log.setExecutorHandler("1");
        log.setExecutorParam("1");
        ret1 = hsJobLogDao.updateTriggerInfo(log);
        dto = hsJobLogDao.load(log.getId());


        log.setHandleTime(new Date());
        log.setHandleCode(2);
        log.setHandleMsg("2");
        ret1 = hsJobLogDao.updateHandleInfo(log);
        dto = hsJobLogDao.load(log.getId());


        List<Long> ret4 = hsJobLogDao.findClearLogIds(1, 1, new Date(), 100, 100);

        int ret2 = hsJobLogDao.delete(log.getJobId());

    }

}
