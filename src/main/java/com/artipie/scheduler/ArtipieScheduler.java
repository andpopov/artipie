/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.scheduler;

import com.artipie.ArtipieException;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Scheduler.
 */
public final class ArtipieScheduler {
    /**
     * Scheduler.
     */
    private Scheduler scheduler;

    /**
     * Ctor.
     */
    public ArtipieScheduler() {
    }

    /**
     * Start scheduler.
     */
    public void start() {
        try {
            final StdSchedulerFactory factory = new StdSchedulerFactory();
            this.scheduler = factory.getScheduler();
            scheduler.start();
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Stop scheduler.
     */
    public void stop() {
        try {
            this.scheduler.shutdown(true);
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Schedule.
     */
    public void scheduleJob(final JobDetail job, final String cronExpression) {
        try {
            Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(
                    String.format("trigger-%s", job.getKey()),
                    "cron-group"
                )
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .forJob(job)
                .build();
            this.scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Schedule.
     */
    public void clearJobs() {
        try {
            this.scheduler.clear();
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Cancel job.
     */
    public void cancelJob(final JobKey job) {
        try {
            this.scheduler.deleteJob(job);
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }
}
