package com.thoughtworks.go.presentation;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.StageConfig;

import java.util.ArrayList;
import java.util.List;

public class Stage {
    private String name;

    public String getName() {
        return name;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    private List<Job> jobs;

    public Stage(StageConfig stageConfig) {
        this.name = stageConfig.name().toString();
        this.jobs = jobsFrom(stageConfig.getJobs());
    }

    private List<Job> jobsFrom(JobConfigs jobConfigs) {
        ArrayList<Job> jobs = new ArrayList<>();
        for(JobConfig jobConfig : jobConfigs) {
            jobs.add(new Job(jobConfig.name().toString()));
        }
        return jobs;
    }
}
