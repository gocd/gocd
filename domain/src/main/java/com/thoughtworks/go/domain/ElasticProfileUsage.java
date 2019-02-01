/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.domain;

import java.util.Objects;

public class ElasticProfileUsage {
    private final String pipelineName;
    private final String stageName;
    private final String jobName;
    private final String templateName;
    private final String pipelineConfigOrigin;

    public ElasticProfileUsage(String pipelineName, String stageName, String jobName, String templateName, String pipelineConfigOrigin) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.jobName = jobName;
        this.templateName = templateName;
        this.pipelineConfigOrigin = pipelineConfigOrigin;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public String getJobName() {
        return jobName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String pipelineConfigOrigin() {
        return pipelineConfigOrigin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ElasticProfileUsage)) return false;
        ElasticProfileUsage that = (ElasticProfileUsage) o;
        return Objects.equals(pipelineName, that.pipelineName) &&
                Objects.equals(stageName, that.stageName) &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(templateName, that.templateName) &&
                Objects.equals(pipelineConfigOrigin, that.pipelineConfigOrigin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineName, stageName, jobName, templateName, pipelineConfigOrigin);
    }

    @Override
    public String toString() {
        return "ElasticProfileUsage{" +
                "pipelineName='" + pipelineName + '\'' +
                ", stageName='" + stageName + '\'' +
                ", jobName='" + jobName + '\'' +
                ", templateName='" + templateName + '\'' +
                ", pipelineConfigOrigin=" + pipelineConfigOrigin +
                '}';
    }
}
