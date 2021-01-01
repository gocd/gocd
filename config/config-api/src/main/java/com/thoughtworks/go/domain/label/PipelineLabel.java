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
package com.thoughtworks.go.domain.label;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.InsecureEnvironmentVariables;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipelineLabel implements Serializable {
    protected String label;
    private InsecureEnvironmentVariables envVars;
    public static final String COUNT = "COUNT";
    public static final String ENV_VAR_PREFIX = "env:";
    public static final String COUNT_TEMPLATE = String.format("${%s}", COUNT);

    public PipelineLabel(String labelTemplate, InsecureEnvironmentVariables insecureEnvironmentVariables) {
        this.label = labelTemplate;
        this.envVars = insecureEnvironmentVariables;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static final Pattern PIPELINE_LABEL_TEMPLATE_PATTERN_FOR_MIGRATION = Pattern.compile("(\\$\\{[^}]*\\})");
    public static final Pattern PATTERN = Pattern.compile("\\$\\{(?<name>[^}\\[]+)(?:\\[:(?<truncation>\\d+)])?}");

    public void updateLabel(Map<CaseInsensitiveString, String> namedRevisions, int pipelineCounter) {
        this.label = interpolateLabel(namedRevisions, pipelineCounter);
        this.label = StringUtils.substring(label, 0, 255);
    }

    private String interpolateLabel(Map<CaseInsensitiveString, String> materialRevisions, int pipelineCounter) {
        final Matcher matcher = PATTERN.matcher(this.label);
        final StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String token = matcher.group("name");
            String value;

            if (COUNT.equalsIgnoreCase(token)) {
                value = Integer.toString(pipelineCounter);
            } else if (token.toLowerCase().startsWith(ENV_VAR_PREFIX)) {
                value = resolveEnvironmentVariable(token);
            } else {
                final String truncate = matcher.group("truncation");
                value = resolveMaterialRevision(materialRevisions, token, truncate);
            }

            if (null == value) {
                value = "\\" + matcher.group(0);
            }

            matcher.appendReplacement(buffer, value);
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String resolveEnvironmentVariable(String variable) {
        variable = variable.substring(ENV_VAR_PREFIX.length());
        return envVars.getInsecureEnvironmentVariableOrDefault(variable, "");
    }

    private String resolveMaterialRevision(Map<CaseInsensitiveString, String> knownRevisions, String name, String truncation) {
        final String revision = knownRevisions.get(new CaseInsensitiveString(name));

        if (StringUtils.isNotBlank(truncation)) {
            int truncationLength = Integer.parseInt(truncation);

            if (null != revision && revision.length() > truncationLength) {
                return revision.substring(0, truncationLength);
            }
        }

        return revision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineLabel label1 = (PipelineLabel) o;

        return label != null ? label.equals(label1.label) : label1.label == null;
    }

    @Override
    public int hashCode() {
        return (label != null ? label.hashCode() : 0);
    }

    public static PipelineLabel create(String labelTemplate, InsecureEnvironmentVariables envVars) {
        if (StringUtils.isBlank(labelTemplate)) {
            return defaultLabel();
        } else {
            return new PipelineLabel(labelTemplate, envVars);
        }
    }

    public static PipelineLabel defaultLabel() {
        return new PipelineLabel(PipelineLabel.COUNT_TEMPLATE, InsecureEnvironmentVariables.EMPTY_ENV_VARS);
    }

    // used for XSLT, needs to be static
    public static String migratePipelineLabelTemplate(String labelTemplate) {
        Matcher matcher = PIPELINE_LABEL_TEMPLATE_PATTERN_FOR_MIGRATION.matcher(labelTemplate);

        while (matcher.find()) {
            String group = matcher.group(1);
            if (!StringUtils.startsWith(group, "${env:")) {
                String replacementText = RegExUtils.replaceFirst(group, "(?<!\\[):", "_");
                labelTemplate = labelTemplate.replace(group, replacementText);
            }
        }

        return labelTemplate;
    }
}
