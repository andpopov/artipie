/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

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
        class TestJob implements Job {
            public TestJob() {
                System.out.println();
            }

            @Override
            public void execute(final JobExecutionContext context) throws JobExecutionException {
                ref.set("TestJob is done");
            }
        }
        final ArtipieScheduler scheduler = new ArtipieScheduler();
        scheduler.start();
        scheduler.scheduleJob(
                JobBuilder
                .newJob()
                .ofType(TestJob.class)
                .withIdentity("test-job")
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
}
