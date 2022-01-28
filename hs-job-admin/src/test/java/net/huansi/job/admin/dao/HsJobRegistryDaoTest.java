package net.huansi.job.admin.dao;

import net.huansi.job.admin.core.model.HsJobRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HsJobRegistryDaoTest {

    @Resource
    private HsJobRegistryDao hsJobRegistryDao;

    @Test
    public void test(){
        int ret = hsJobRegistryDao.registryUpdate("g1", "k1", "v1", new Date());
        if (ret < 1) {
            ret = hsJobRegistryDao.registrySave("g1", "k1", "v1", new Date());
        }

        List<HsJobRegistry> list = hsJobRegistryDao.findAll(1, new Date());

        int ret2 = hsJobRegistryDao.removeDead(Arrays.asList(1));
    }

}
