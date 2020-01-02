/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.artifact.models;

import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/* Provided by an artifact plugin, after fetching artifacts. These environment variables should be
* made available, in context, to the rest of the tasks. */
public class FetchArtifactEnvironmentVariable {
    private final String name;
    private final String value;
    private final boolean secure;

    public FetchArtifactEnvironmentVariable(String name, String value, boolean secure) {
        this.name = name;
        this.value = value;
        this.secure = secure;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public boolean isSecure() {
        return secure;
    }

    public String displayValue() {
        if (isSecure()) {
            return EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE;
        }
        return value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FetchArtifactEnvironmentVariable that = (FetchArtifactEnvironmentVariable) o;

        return new EqualsBuilder()
                .append(secure, that.secure)
                .append(name, that.name)
                .append(value, that.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(value)
                .append(secure)
                .toHashCode();
    }
}
