/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.service.TaskFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractTask implements Task  {
    @ConfigSubtag(label = "RunIfs")
    protected RunIfConfigs runIfConfigs = new RunIfConfigs();

    @ConfigSubtag(label = "OnCancel") public OnCancelConfig onCancelConfig = OnCancelConfig.killAllChildProcess();

    public static final String RUN_IF_CONFIGS_PASSED = "runIfConfigsPassed";
    public static final String RUN_IF_CONFIGS_FAILED = "runIfConfigsFailed";
    public static final String RUN_IF_CONFIGS_ANY = "runIfConfigsAny";
    public static final String ON_CANCEL_CONFIG = "onCancelConfig";
    public static final String HAS_CANCEL_TASK = "hasCancelTask";

    protected ConfigErrors errors = new ConfigErrors();

    @Override
    public RunIfConfigs getConditions() {
        return runIfConfigs;
    }

    public void setConditions(RunIfConfigs runIfConfigs) {
        this.runIfConfigs = runIfConfigs;
    }

    public String getConditionsForDisplay() {
        if (runIfConfigs.isEmpty()) {
            return StringUtils.capitalize(RunIfConfig.PASSED.toString());
        }
        return runIfConfigs.stream().map(f -> StringUtils.capitalize(f.toString())).collect(Collectors.joining(", "));
    }

    @Override
    public Task cancelTask() {
        return onCancelConfig.getTask();
    }

    public OnCancelConfig onCancelConfig() {
        return onCancelConfig;
    }

    public OnCancelConfig getOnCancelConfig() {
        return onCancelConfig;
    }

    @Override
    public boolean hasCancelTask() {
        return onCancelConfig.hasCancelTask();
    }

    public void setCancelTask(Task task) {
        this.onCancelConfig = new OnCancelConfig(task);
    }

    public void setOnCancelConfig(OnCancelConfig onCancelConfig) {
        this.onCancelConfig = onCancelConfig;
    }

    public boolean runIfConfigsPassed() {
        return runIfConfigs.match(RunIfConfig.PASSED);
    }

    public boolean runIfConfigsFailed() {
        return runIfConfigs.match(RunIfConfig.FAILED);
    }

    public boolean runIfConfigsAny() {
        return runIfConfigs.match(RunIfConfig.ANY);
    }

    public abstract String describe();

    @SuppressWarnings("unchecked")
    @Override
    public final void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        Map<String, Object> attributeMap = (Map<String, Object>) attributes;
        if (attributes == null || attributeMap.isEmpty()) {
            return;
        }
        runIfConfigs.clear();
        if (attributeMap.containsKey(RUN_IF_CONFIGS_ANY) && "1".equals(attributeMap.get(RUN_IF_CONFIGS_ANY))) {
            runIfConfigs.add(RunIfConfig.ANY);
        }
        if (attributeMap.containsKey(RUN_IF_CONFIGS_FAILED) && "1".equals(attributeMap.get(RUN_IF_CONFIGS_FAILED))) {
            runIfConfigs.add(RunIfConfig.FAILED);
        }
        if (attributeMap.containsKey(RUN_IF_CONFIGS_PASSED) && "1".equals(attributeMap.get(RUN_IF_CONFIGS_PASSED))) {
            runIfConfigs.add(RunIfConfig.PASSED);
        }
        if ("1".equals(attributeMap.get(HAS_CANCEL_TASK))) {
            onCancelConfig = OnCancelConfig.create(attributeMap.get(ON_CANCEL_CONFIG), taskFactory);
        } else if ("0".equals(attributeMap.get(HAS_CANCEL_TASK))) {
            onCancelConfig = OnCancelConfig.killAllChildProcess();
        }
        setTaskConfigAttributes(attributeMap);
    }

    @Override
    public final void setConfigAttributes(Object attributes) {
        setConfigAttributes(attributes, null);
    }

    @Override
    public boolean hasSameTypeAs(Task task) {
        return this.getTaskType().equals(task.getTaskType());
    }

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return onCancelConfig.validateTree(validationContext) && errors.isEmpty();
    }

    protected abstract void setTaskConfigAttributes(Map<String, ?> attributes);

    @Override
    public final void validate(ValidationContext validationContext) {
        validateTask(validationContext);
        validateNestedOnCancelTask();
    }

    private void validateNestedOnCancelTask() {
        if (cancelTask().hasCancelTask()) {
            errors.add(ON_CANCEL_CONFIG, "Cannot nest 'oncancel' within a cancel task");
        }
    }

    protected abstract void validateTask(ValidationContext validationContext);

    @Override
    public final ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractTask that = (AbstractTask) o;

        return Objects.equals(onCancelConfig, that.onCancelConfig) &&
            Objects.equals(runIfConfigs, that.runIfConfigs);
    }

    @Override
    public int hashCode() {
        int result = runIfConfigs != null ? runIfConfigs.hashCode() : 0;
        result = 31 * result + (onCancelConfig != null ? onCancelConfig.hashCode() : 0);
        result = 31 * result + getTaskType().hashCode();
        return result;
    }

}
