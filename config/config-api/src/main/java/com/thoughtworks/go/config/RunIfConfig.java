/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.io.Serializable;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;

@ConfigTag("runif")
public class RunIfConfig implements Serializable, Validatable{
    @ConfigAttribute(value = "status") @SkipParameterResolution
    private String status;
    public static final RunIfConfig PASSED = new RunIfConfig("passed");
    public static final RunIfConfig FAILED = new RunIfConfig("failed");
    public static final RunIfConfig ANY = new RunIfConfig("any");
    private ConfigErrors configErrors = new ConfigErrors();

    public RunIfConfig() {
    }

    public RunIfConfig(String status) {
        this.status = status;
    }

    public static RunIfConfig fromJobResult(String jobResultString) {
        return new RunIfConfig(jobResultString);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RunIfConfig that = (RunIfConfig) o;

        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (status != null ? status.hashCode() : 0);
    }

    public String toString() {
        return status;
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }
}
