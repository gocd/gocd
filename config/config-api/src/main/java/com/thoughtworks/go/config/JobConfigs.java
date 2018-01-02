/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.service.TaskFactory;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static java.util.Arrays.asList;

@ConfigTag("jobs")
@ConfigCollection(value = JobConfig.class, minimum = 1)
public class JobConfigs extends BaseCollection<JobConfig> implements Validatable, ParamsAttributeAware {
    private final ConfigErrors configErrors = new ConfigErrors();


    public JobConfigs(JobConfig... plans) {
        super(asList(plans));
    }

    public JobConfigs() {
        super();
    }

    public boolean containsName(final CaseInsensitiveString jobName) {
        for (JobConfig jobConfig : this) {
            if (jobName.equals(jobConfig.name())) {
                return true;
            }
        }
        return false;
    }

    public boolean add(JobConfig jobConfig) {
        verifyUniqueName(jobConfig);
        return addJob(jobConfig);
    }

    private boolean addJob(JobConfig jobConfig) {
        return super.add(jobConfig);
    }

    public JobConfig set(int index, JobConfig jobConfig) {
        verifyUniqueName(jobConfig, index);
        return super.set(index, jobConfig);
    }

    public void add(int index, JobConfig jobConfig) {
        verifyUniqueName(jobConfig);
        super.add(index, jobConfig);
    }

    private void verifyUniqueName(JobConfig jobConfig) {
        bombIf(containsName(jobConfig.name()), String.format("You have defined multiple Jobs called '%s'. Job names are case-insensitive and must be unique.", jobConfig.name()));
    }

    private void verifyUniqueName(JobConfig jobConfig, int index) {
        if (jobConfig.name().equals(super.get(index).name())) {
            return;
        }
        verifyUniqueName(jobConfig);
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();
        for (JobConfig jobConfig : this) {
            isValid = jobConfig.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    public void validate(ValidationContext validationContext) {
        validateNameUniqueness();
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void setConfigAttributes(Object attributes) {
        setConfigAttributes(attributes, null);
    }
    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        this.clear();
        if (attributes != null) {
            for (Object attribute : (List) attributes) {
                JobConfig job = new JobConfig();
                job.setConfigAttributes(attribute, taskFactory);
                this.addJobWithoutValidityAssertion(job);
            }
        }
    }

    public void addJobWithoutValidityAssertion(JobConfig jobConfig) {
        addJob(jobConfig);
    }

    private void validateNameUniqueness() {
        Map<String, JobConfig> nameToConfig = new HashMap<>();
        for (JobConfig jobConfig : this) {
            jobConfig.validateNameUniqueness(nameToConfig);
        }
    }

    public JobConfig getJob(final CaseInsensitiveString job) {
        for (JobConfig jobConfig : this) {
           if (jobConfig.name().equals(job))
               return jobConfig;
        }
        return null;
    }

    public List<CaseInsensitiveString> names() {
        List<CaseInsensitiveString> names = new ArrayList<>();
        for (JobConfig jobConfig : this) {
            names.add(jobConfig.name());
        }
        return names;
    }
}
