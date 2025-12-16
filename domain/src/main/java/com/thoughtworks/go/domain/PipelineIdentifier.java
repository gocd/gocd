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
package com.thoughtworks.go.domain;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

public class PipelineIdentifier implements Serializable {
    private String name;
    private Integer counter;
    private String label;

    public PipelineIdentifier(String name, @NotNull Integer counter) {
        bombIfNull(counter, "Pipeline Identifier cannot be created without a counter");
        this.name = name;
        this.counter = counter;
    }

    public PipelineIdentifier(String name, Integer counter, String label) {
        this(name, counter);
        this.label = label;
    }

    // for ibatis
    protected PipelineIdentifier() {
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Integer getCounter() {
        return counter;
    }

    public boolean hasCounter() {
        return counter != null && counter >0;
    }

    public String pipelineLocator() {
        return String.format("%s/%s", name, instanceIdentifier());
    }

    public String pipelineLocatorByLabelOrCounter() {
        return String.format("%s/%s", name, hasCounter() ? String.valueOf(counter) : label);
    }

    public String instanceIdentifier() {
        return String.valueOf(counter);
    }

    public String pipelineLocatorForDisplay() {
        return String.format("%s/%s", name, label);
    }

    @Override
    public String toString() {
        return String.format("Pipeline[name=%s, counter=%s, label=%s]", name, counter, label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineIdentifier that = (PipelineIdentifier) o;

        return Objects.equals(counter, that.counter) &&
            Objects.equals(label, that.label) &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (counter != null ? counter.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    public String asURN() {
        return String.format("urn:x-go.studios.thoughtworks.com:job-id:%s:%s", name, counter);
    }
}
