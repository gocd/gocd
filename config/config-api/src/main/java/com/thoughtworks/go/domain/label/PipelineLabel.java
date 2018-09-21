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

package com.thoughtworks.go.domain.label;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.InsecureEnvironmentVariables;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipelineLabel implements Serializable {
    protected String label;
    private InsecureEnvironmentVariables envVars;
    public static final String COUNT = "COUNT";
    private static final String ENV_VAR_PREFIX = "env:";
    public static final String COUNT_TEMPLATE = String.format("${%s}", COUNT);

    public PipelineLabel(String labelTemplate, InsecureEnvironmentVariables insecureEnvironmentVariables) {
        this.label = labelTemplate;
        this.envVars = insecureEnvironmentVariables;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }

    public static final Pattern PATTERN = Pattern.compile("(?i)\\$\\{([a-zA-Z:]*[a-zA-Z0-9_\\-\\.!~'#:]+)(\\[:(\\d+)\\])?\\}");

    private String replaceRevisionsInLabel(Map<CaseInsensitiveString, String> materialRevisions) {
        final Matcher matcher = PATTERN.matcher(this.label);
        final StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            final String revision = lookupVariable(matcher, materialRevisions);
            matcher.appendReplacement(buffer, revision);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String lookupVariable(Matcher matcher, Map<CaseInsensitiveString, String> materialRevisions) {
        final CaseInsensitiveString variable = new CaseInsensitiveString(matcher.group(1));

        String valueForMaterialVariable = materialRevisions.get(variable);
        String valueForPrefixedVariable = getValueIfVariableHasValidPrefix(variable);

        String result;

        if (valueForMaterialVariable != null) {
            result = valueForMaterialVariable;
        } else if (valueForPrefixedVariable != null) {
            result = valueForPrefixedVariable;
        } else {
            return "\\" + matcher.group(0);
        }

        final String truncationLengthLiteral = matcher.group(3);
        if (truncationLengthLiteral != null) {
            int truncationLength = Integer.parseInt(truncationLengthLiteral);

            if (result.length() > truncationLength) {
                result = result.substring(0, truncationLength);
            }
        }
        return result;
    }

    public void updateLabel(Map<CaseInsensitiveString, String> namedRevisions) {
        this.label = replaceRevisionsInLabel(namedRevisions);
        this.label = StringUtils.substring(label, 0, 255);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineLabel label1 = (PipelineLabel) o;

        if (label != null ? !label.equals(label1.label) : label1.label != null) {
            return false;
        }

        return true;
    }

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

    public static boolean hasValidPrefix(String value) {
        return value.toLowerCase().startsWith(ENV_VAR_PREFIX);
    }

    private String getValueIfVariableHasValidPrefix(CaseInsensitiveString variable) {
        if (variable.startsWith(ENV_VAR_PREFIX)) {
            return envVars.getInsecureEnvironmentVariableOrDefault(variable.toString().split(":", 2)[1], "");
        }

        return null;
    }
}
