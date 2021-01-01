/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.service.TaskFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FetchTaskAdapter implements Task {

    private FetchTask fetchTask = new FetchTask();
    private FetchPluggableArtifactTask fetchPluggableArtifactTask = new FetchPluggableArtifactTask();
    private String selectedTaskType = "gocd";
    private transient String pluginId;

    public FetchTaskAdapter() {
    }

    public FetchTaskAdapter(FetchTask fetchTask) {
        this.fetchTask = fetchTask;
        this.selectedTaskType = "gocd";
    }

    public FetchTaskAdapter(FetchPluggableArtifactTask fetchPluggableArtifactTask) {
        this.fetchPluggableArtifactTask = fetchPluggableArtifactTask;
        this.selectedTaskType = "external";
    }

    public AbstractFetchTask getAppropriateTask() {
        return isExternal() ? fetchPluggableArtifactTask : fetchTask;
    }

    public FetchTask getFetchTask() {
        return fetchTask;
    }

    public FetchPluggableArtifactTask getFetchPluggableArtifactTask() {
        return fetchPluggableArtifactTask;
    }

    public String getSelectedTaskType() {
        return selectedTaskType;
    }

    public void setSelectedTaskType(String selectedTaskType) {
        this.selectedTaskType = selectedTaskType;
    }

    public boolean runIfConfigsPassed() {
        return oneOf(fetchPluggableArtifactTask::runIfConfigsPassed, fetchTask::runIfConfigsPassed);
    }

    public boolean runIfConfigsFailed() {
        return oneOf(fetchPluggableArtifactTask::runIfConfigsFailed, fetchTask::runIfConfigsFailed);
    }

    public boolean runIfConfigsAny() {
        return oneOf(fetchPluggableArtifactTask::runIfConfigsAny, fetchTask::runIfConfigsAny);
    }

    public CaseInsensitiveString getPipelineName() {
        return oneOf(fetchPluggableArtifactTask::getPipelineName, fetchTask::getPipelineName);
    }

    public CaseInsensitiveString getStage() {
        return oneOf(fetchPluggableArtifactTask::getStage, fetchTask::getStage);
    }

    public CaseInsensitiveString getJob() {
        return oneOf(fetchPluggableArtifactTask::getJob, fetchTask::getJob);
    }

    public void setPipelineName(CaseInsensitiveString pipelineName) {
        if (isExternal()) {
            fetchPluggableArtifactTask.setPipelineName(pipelineName);
        } else {
            fetchTask.setPipelineName(pipelineName);
        }
    }

    public void setStage(CaseInsensitiveString stage) {
        oneOf(fetchPluggableArtifactTask::setStage, fetchTask::setStage, stage);
    }

    public void setJob(CaseInsensitiveString job) {
        oneOf(fetchPluggableArtifactTask::setJob, fetchTask::setJob, job);
    }

    protected File destOnAgent(String pipelineName) {
        return isExternal() ? fetchPluggableArtifactTask.destOnAgent(pipelineName) : fetchTask.destOnAgent(pipelineName);
    }

    protected void validateAttributes(ValidationContext validationContext) {
        oneOf(fetchPluggableArtifactTask::validateAttributes, fetchTask::validateAttributes, validationContext);
    }

    protected void setFetchTaskAttributes(Map attributeMap) {
        oneOf(fetchPluggableArtifactTask::setFetchTaskAttributes, fetchTask::setFetchTaskAttributes, attributeMap);
    }

    @Override
    public RunIfConfigs getConditions() {
        return isExternal() ? fetchPluggableArtifactTask.getConditions() : fetchTask.getConditions();
    }

    @Override
    public Task cancelTask() {
        return isExternal() ? fetchPluggableArtifactTask.cancelTask() : fetchTask.cancelTask();
    }

    @Override
    public boolean hasCancelTask() {
        return isExternal() ? fetchPluggableArtifactTask.hasCancelTask() : fetchTask.hasCancelTask();
    }

    @Override
    public String getTaskType() {
        return "fetch";
    }

    @Override
    public String getTypeForDisplay() {
        return isExternal() ? fetchPluggableArtifactTask.getTypeForDisplay() : fetchTask.getTypeForDisplay();
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        return isExternal() ? fetchPluggableArtifactTask.getPropertiesForDisplay() : fetchTask.getPropertiesForDisplay();
    }

    @Override
    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        if (isExternal()) {
            fetchPluggableArtifactTask.setConfigAttributes(attributes, taskFactory);
        } else {
            fetchTask.setConfigAttributes(attributes, taskFactory);
        }
    }

    @Override
    public boolean hasSameTypeAs(Task task) {
        return isExternal() ? fetchPluggableArtifactTask.hasSameTypeAs(task) : fetchTask.hasSameTypeAs(task);
    }

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        return isExternal() ? fetchPluggableArtifactTask.validateTree(validationContext) : fetchTask.validateTree(validationContext);
    }

    public String getArtifactId() {
        return fetchPluggableArtifactTask.getArtifactId();
    }

    public Configuration getConfiguration() {
        return fetchPluggableArtifactTask.getConfiguration();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        oneOf(fetchPluggableArtifactTask::validate, fetchTask::validate, validationContext);
    }

    @Override
    public ConfigErrors errors() {
        return oneOf(fetchPluggableArtifactTask::errors, fetchTask::errors);
    }

    @Override
    public void addError(String fieldName, String message) {
        if (isExternal()) {
            fetchPluggableArtifactTask.addError(fieldName, message);
        } else {
            fetchTask.addError(fieldName, message);
        }
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        oneOf(fetchPluggableArtifactTask::setConfigAttributes, fetchTask::setConfigAttributes, attributes);
    }

    public String describe() {
        return oneOf(fetchPluggableArtifactTask::describe, fetchTask::describe);
    }

    public OnCancelConfig onCancelConfig() {
        return oneOf(fetchPluggableArtifactTask::onCancelConfig, fetchTask::onCancelConfig);
    }

    public OnCancelConfig getOnCancelConfig() {
        return oneOf(fetchPluggableArtifactTask::getOnCancelConfig, fetchTask::onCancelConfig);
    }

    public void setCancelTask(Task task) {
        oneOf(fetchPluggableArtifactTask::setCancelTask, fetchTask::setCancelTask, task);
    }

    public void setOnCancelConfig(OnCancelConfig onCancelConfig) {
        oneOf(fetchPluggableArtifactTask::setOnCancelConfig, fetchTask::setOnCancelConfig, onCancelConfig);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FetchTaskAdapter that = (FetchTaskAdapter) o;
        if (fetchTask != null ? !fetchTask.equals(that.fetchTask) : that.fetchTask != null) return false;
        if (fetchPluggableArtifactTask != null ? !fetchPluggableArtifactTask.equals(that.fetchPluggableArtifactTask) : that.fetchPluggableArtifactTask != null)
            return false;
        if (selectedTaskType != null ? !selectedTaskType.equals(that.selectedTaskType) : that.selectedTaskType != null)
            return false;
        return pluginId != null ? pluginId.equals(that.pluginId) : that.pluginId == null;
    }

    @Override
    public int hashCode() {
        int result = fetchTask != null ? fetchTask.hashCode() : 0;
        result = 31 * result + (fetchPluggableArtifactTask != null ? fetchPluggableArtifactTask.hashCode() : 0);
        result = 31 * result + (selectedTaskType != null ? selectedTaskType.hashCode() : 0);
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FetchTaskAdapter{" +
                "fetchTask=" + fetchTask +
                ", fetchPluggableArtifactTask=" + fetchPluggableArtifactTask +
                ", selectedTaskType='" + selectedTaskType + '\'' +
                ", pluginId='" + pluginId + '\'' +
                '}';
    }

    //---- delegates to fetchTask ----
    public boolean isSourceAFile() {
        return fetchTask.isSourceAFile();
    }

    public String getDest() {
        return fetchTask.getDest();
    }

    public String getSrcdir() {
        return fetchTask.getSrcdir();
    }

    public String getSrcfile() {
        return fetchTask.getSrcfile();
    }

    public void setSrcfile(String srcfile) {
        fetchTask.setSrcfile(srcfile);
    }

    public String getSrc() {
        return fetchTask.getSrc();
    }

    public void setSrcdir(String srcdir) {
        fetchTask.setSrcdir(srcdir);
    }

    public String getRawSrcdir() {
        return fetchTask.getRawSrcdir();
    }

    public String getRawSrcfile() {
        return fetchTask.getRawSrcfile();
    }

    public void setDest(String dest) {
        fetchTask.setDest(dest);
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    private boolean isExternal() {
        return "external".equals(selectedTaskType);
    }

    private <T> T oneOf(Supplier<T> getterForExternalArtifact, Supplier<T> getterForGocdArtifact) {
        return isExternal() ? getterForExternalArtifact.get() : getterForGocdArtifact.get();
    }

    private <T> void oneOf(Consumer<T> setterForExternalArtifact, Consumer<T> setterForGocdArtifact, T value) {
        if (isExternal()) {
            setterForExternalArtifact.accept(value);
        } else {
            setterForGocdArtifact.accept(value);
        }
    }

    public String getConditionsForDisplay() {
        return isExternal() ? fetchPluggableArtifactTask.getConditionsForDisplay() : fetchTask.getConditionsForDisplay();
    }
}
