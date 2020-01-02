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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ResourceConfig;
import org.apache.commons.lang3.StringUtils;

public class Resource extends PersistentObject implements Comparable<Resource> {
    private String name;
    private long buildId;

    public Resource() {
    }

    public Resource(String name) {
        setName(name);
    }

    public Resource(ResourceConfig resourceConfig) {
        this(resourceConfig.getName());
    }

    public Resource(Resource resource) {
        this(resource.id, resource.name, resource.buildId);
    }

    public Resource(long id, String name, long buildId) {
        this.id = id;
        this.buildId = buildId;
        setName(name);
    }

    public String getName() {
        return StringUtils.trimToNull(name);
    }

    public void setName(String name) {
        this.name = StringUtils.trimToNull(name);
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setBuildId(long id) {
        this.buildId = id;
    }

    @Override
    public int compareTo(Resource other) {
        return name.compareTo(other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;

        Resource resource = (Resource) o;

        return getName() != null ? getName().equalsIgnoreCase(resource.getName()) : resource.getName() == null;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
