/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scheduler;

import java.sql.Ref;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.artipie.asto.Key;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.quartz.*;

/**
 * Test for ArtipieScheduler.
 *
 * @since 0.28.0
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ArtipieSchedulerTest {

    @Test
    void scheduleJob() {
        final AtomicReference<String> ref = new AtomicReference<>();
        final ArtipieScheduler scheduler = new ArtipieScheduler();
        scheduler.start();
        final JobDataMap data = new JobDataMap();
        data.put("ref", ref);
        scheduler.scheduleJob(
                JobBuilder
                .newJob()
                .ofType(TestJob.class)
                .withIdentity("test-job")
                .setJobData(data)
                .build()
            ,
            "0/5 * * * * ?"
        );
        Awaitility.waitAtMost(1, TimeUnit.MINUTES).until(() -> ref.get() != null);
        scheduler.stop();
        MatcherAssert.assertThat(
            "Returns port and endpoint",
            ref.get(),
            new IsEqual<>("TestJob is done")
        );
    }

    public static class TestJob implements Job {
        @SuppressWarnings("unchecked")
        @Override
        public void execute(final JobExecutionContext context) throws JobExecutionException {
            final AtomicReference<String> ref = (AtomicReference<String>) context.getJobDetail().getJobDataMap().get("ref");
            ref.set("TestJob is done");
        }
    }
}
