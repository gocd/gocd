/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.io.Serializable;

/**
 * @understands Value of env variable
 */
@ConfigTag("value")
public class VariableValueConfig implements Serializable {
    @ConfigValue
    private String value;

    public VariableValueConfig(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public VariableValueConfig() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VariableValueConfig)) {
            return false;
        }

        VariableValueConfig that = (VariableValueConfig) o;

        return value != null ? value.equals(that.value) : that.value == null;

    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
