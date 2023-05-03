/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scheduler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.settings.YamlSettings;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.*;

/**
 * Test for ArtipieScheduler.
 *
 * @since 0.30
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ArtipieSchedulerTest {
    /**
     * Temp dir.
     */
    @TempDir
    Path temp;

    /**
     * Test data storage.
     */
    private BlockingStorage data;

    /**
     * Before each method creates test data storage instance.
     */
    @BeforeEach
    void init() {
        this.data = new BlockingStorage(new FileStorage(this.temp));
    }

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
            ref.get(),
            new IsEqual<>("TestJob is done")
        );
    }

    @Test
    void runCronJob() throws IOException {
        final YamlSettings settings = new YamlSettings(
                Yaml.createYamlInput(
                        String.join(
                                System.lineSeparator(),
                                "meta:",
                                "  storage:",
                                "    type: fs",
                                String.format("    path: %s", this.temp.toString()),
                                "  crontab:",
                                "    - key: scripts/script.groovy",
                                "      cronexp: */3 * * * * ?"
                        )
                ).readYamlMapping()
        );
        final String filename = temp.resolve("scripts/result.txt").toString();
        final String script = String.join(
                System.lineSeparator(),
                String.format("File file = new File('%s')", filename.replace("\\", "\\\\")),
                "file.write 'Hello world'"
        );
        data.save(new Key.From("scripts/script.groovy"), script.getBytes());
        final ArtipieScheduler scheduler = new ArtipieScheduler();
        scheduler.start();
        scheduler.loadCrontab(settings);
        final Key result = new Key.From("scripts/result.txt");
        Awaitility.waitAtMost(1, TimeUnit.MINUTES).until(() -> data.exists(result));
        scheduler.stop();
        MatcherAssert.assertThat(
                new String(data.value(result)),
                new IsEqual<>("Hello world")
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
