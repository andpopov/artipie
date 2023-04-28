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
     * Schedule job.
     * Examples of cron expressions:
     * <ul>
     *     <li>"0 25 11 * * ?" means "11:25am every day"</li>
     *     <li>"0 0 11-15 * * ?" means "11AM and 3PM every day"</li>
     *     <li>"0 0 11-15 * * SAT-SUN" means "between 11AM and 3PM on weekends SAT-SUN"</li>
     * </ul>
     * @param job Job details
     * @param cronexp Cron expression in format {@link org.quartz.CronExpression}
     */
    public void scheduleJob(final JobDetail job, final String cronexp) {
        try {
            Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(
                    String.format("trigger-%s", job.getKey()),
                    "cron-group"
                )
                .withSchedule(CronScheduleBuilder.cronSchedule(cronexp))
                .forJob(job)
                .build();
            this.scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Clear all jobs and triggers.
     */
    public void clearAll() {
        try {
            this.scheduler.clear();
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }

    /**
     * Cancel job.
     * @param job job key
     */
    public void cancelJob(final JobKey job) {
        try {
            this.scheduler.deleteJob(job);
        } catch (SchedulerException exc) {
            throw new ArtipieException(exc);
        }
    }
}
