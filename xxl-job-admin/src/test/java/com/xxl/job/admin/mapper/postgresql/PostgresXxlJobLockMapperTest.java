package com.xxl.job.admin.mapper.postgresql;

import com.xxl.job.admin.mapper.XxlJobLockMapper;
import com.xxl.job.admin.test.postgresql.AbstractPostgresIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresXxlJobLockMapperTest extends AbstractPostgresIntegrationTest {

    @Resource
    private XxlJobLockMapper xxlJobLockMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Test
    void scheduleLockBlocksUntilFirstTransactionCompletes() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        CountDownLatch firstLockHeld = new CountDownLatch(1);

        CompletableFuture<Void> first = CompletableFuture.runAsync(() ->
                template.executeWithoutResult(status -> {
                    xxlJobLockMapper.scheduleLock();
                    firstLockHeld.countDown();
                    try {
                        Thread.sleep(1_500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));

        assertThat(firstLockHeld.await(5, TimeUnit.SECONDS)).isTrue();

        long start = System.nanoTime();
        template.executeWithoutResult(status -> xxlJobLockMapper.scheduleLock());
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        first.get(5, TimeUnit.SECONDS);
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(1_000);
    }
}
