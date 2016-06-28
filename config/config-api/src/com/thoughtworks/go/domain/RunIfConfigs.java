/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;

import java.util.Arrays;

@ConfigCollection(value = RunIfConfig.class)
public class RunIfConfigs extends BaseCollection<RunIfConfig> implements Validatable {
    public static final RunIfConfigs CONFIGS = new RunIfConfigs();
    private ConfigErrors configErrors = new ConfigErrors();

    static {
        CONFIGS.add(RunIfConfig.ANY);
        CONFIGS.add(RunIfConfig.FAILED);
        CONFIGS.add(RunIfConfig.PASSED);
    }

    public RunIfConfigs() {
    }

    public RunIfConfigs(RunIfConfig... configs) {
        this.addAll(Arrays.asList(configs));
    }

    public boolean match(RunIfConfig currentStatus) {
        if (this.isEmpty() && currentStatus.equals(RunIfConfig.PASSED)) {
            return true;
        }

        if (this.contains(RunIfConfig.ANY)) {
            return true;
        }

        return this.contains(currentStatus);
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public RunIfConfig resolveToSingle() {
        if (this.contains(RunIfConfig.ANY)) {
            return RunIfConfig.ANY;
        }

        if (this.contains(RunIfConfig.PASSED)) {
            if (this.contains(RunIfConfig.FAILED)) {
                return RunIfConfig.ANY;
            }
            return RunIfConfig.PASSED;
        }

        if (this.contains(RunIfConfig.FAILED)) {
            return RunIfConfig.FAILED;
        }

        return RunIfConfig.PASSED;
    }
}

