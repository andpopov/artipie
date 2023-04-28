package com.artipie.scripting;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ScriptRunner implements Job {
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final String key = context.getJobDetail().getJobDataMap().getString("key");
    }
}
