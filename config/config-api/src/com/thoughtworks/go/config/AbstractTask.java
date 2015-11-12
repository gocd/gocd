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

import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.ArrayUtil;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractTask implements Task  {
    @ConfigSubtag(label = "RunIfs")
    protected RunIfConfigs runIfConfigs = new RunIfConfigs();

    @ConfigSubtag(label = "OnCancel", optional = true) public OnCancelConfig onCancelConfig = OnCancelConfig.killAllChildProcess();

    public static final String RUN_IF_CONFIGS_PASSED = "runIfConfigsPassed";
    public static final String RUN_IF_CONFIGS_FAILED = "runIfConfigsFailed";
    public static final String RUN_IF_CONFIGS_ANY = "runIfConfigsAny";
    public static final String ON_CANCEL_CONFIG = "onCancelConfig";
    public static final String HAS_CANCEL_TASK = "hasCancelTask";

    protected ConfigErrors errors = new ConfigErrors();

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
        Object[] conditions = ArrayUtil.capitalizeContents(runIfConfigs.toArray());
        return ArrayUtil.join(conditions, ", ");
    }

    public Task cancelTask() {
        return onCancelConfig.getTask();
    }

    public OnCancelConfig onCancelConfig() {
        return onCancelConfig;
    }
    public OnCancelConfig getOnCancelConfig() {
        return onCancelConfig;
    }

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

    public String describe() {
        // for #2398.  sigh
        StringBuilder builder = new StringBuilder();
        ConfigTag configTag = this.getClass().getAnnotation(ConfigTag.class);
        builder.append("<").append(configTag.value()).append(" ");

        GoConfigClassWriter cruiseConfigClass = new GoConfigClassWriter(this.getClass(), new ConfigCache(), null);
        List<GoConfigFieldWriter> fields = cruiseConfigClass.getAllFields(this);
        for (GoConfigFieldWriter field : fields) {
            if (field.isAttribute()) {
                Object value = field.getValue();
                if (!field.isDefault(cruiseConfigClass)) {
                    appendIfNotEmpty(builder, value, field.value());
                }
            } else {
                addDescribeOfArguments(builder, configTag, field);
            }

        }
        if (!(this instanceof ExecTask)) {
            builder.append("/>");
        }
        return builder.toString();
    }

    private void addDescribeOfArguments(StringBuilder builder, ConfigTag configTag, GoConfigFieldWriter field) {
        if (field.isSubtag() && field.getValue() instanceof Arguments) {
            closeConfigTag(builder, field);

            if (field.getValue() instanceof Arguments && ((Arguments) field.getValue()).size() != 0) {
                for (Argument arg : (Arguments) field.getValue()) {
                    builder.append(String.format("<arg>%s</arg>", arg.getValue())).append("\n");
                }
                builder.append("</" + configTag.value() + ">");
            }
        }
    }

    private void closeConfigTag(StringBuilder builder, GoConfigFieldWriter field) {
        if (((Arguments) field.getValue()).size() != 0) {
            builder.append(">").append("\n");
        } else {
            builder.append("/>");
        }
    }

    private void appendIfNotEmpty(StringBuilder builder, Object value, String description) {
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            builder.append(String.format("%s=\"%s\" ", description, value));
        }
    }

    public final void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        Map attributeMap = (Map) attributes;
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
            onCancelConfig = OnCancelConfig.create((Map) attributeMap.get(ON_CANCEL_CONFIG), taskFactory);
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

    protected abstract void setTaskConfigAttributes(Map attributes);

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

    public final ConfigErrors errors() {
        return errors;
    }

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

        if (onCancelConfig != null ? !onCancelConfig.equals(that.onCancelConfig) : that.onCancelConfig != null) {
            return false;
        }
        if (runIfConfigs != null ? !runIfConfigs.equals(that.runIfConfigs) : that.runIfConfigs != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = runIfConfigs != null ? runIfConfigs.hashCode() : 0;
        result = 31 * result + (onCancelConfig != null ? onCancelConfig.hashCode() : 0);
        return result;
    }

}
