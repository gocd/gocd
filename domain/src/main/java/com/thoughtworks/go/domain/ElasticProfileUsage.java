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

public class ElasticProfileUsage {
    private final String pipelineName;
    private final String stageName;
    private final String jobName;
    private final String templateName;

    public ElasticProfileUsage(String pipelineName, String stageName, String jobName) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.jobName = jobName;
        this.templateName = null;
    }

    public ElasticProfileUsage(String pipelineName, String stageName, String jobName, String templateName) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.jobName = jobName;
        this.templateName = templateName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElasticProfileUsage that = (ElasticProfileUsage) o;

        if (!pipelineName.equals(that.pipelineName)) return false;
        if (!stageName.equals(that.stageName)) return false;
        if (!jobName.equals(that.jobName)) return false;
        return templateName != null ? templateName.equals(that.templateName) : that.templateName == null;
    }

    @Override
    public int hashCode() {
        int result = pipelineName.hashCode();
        result = 31 * result + stageName.hashCode();
        result = 31 * result + jobName.hashCode();
        result = 31 * result + (templateName != null ? templateName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ElasticProfileUsage{" +
                "pipelineName='" + pipelineName + '\'' +
                ", stageName='" + stageName + '\'' +
                ", jobName='" + jobName + '\'' +
                ", templateName='" + templateName + '\'' +
                '}';
    }
}
