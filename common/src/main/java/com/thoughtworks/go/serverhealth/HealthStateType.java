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
package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.config.CruiseConfig;

import java.util.Objects;

public class HealthStateType implements Comparable<HealthStateType> {

    private final String name;
    private final HealthStateScope scope;
    private final String subkey;

    private HealthStateType(String name, HealthStateScope scope, String subkey) {
        this.name = name;
        this.scope = scope;
        this.subkey = subkey;
    }

    public static HealthStateType general(HealthStateScope scope) {
        return new HealthStateType("GENERAL", scope, null);
    }

    public static HealthStateType withSubkey(HealthStateScope scope, String subkey) {
        return new HealthStateType("GENERAL_WITH_SUBKEY", scope, subkey);
    }

    public static HealthStateType invalidConfig() {
        return new HealthStateType("INVALID_CONFIG", HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType forbiddenForPipeline(String pipelineName) {
        return new HealthStateType("FORBIDDEN", HealthStateScope.forPipeline(pipelineName), null);
    }

    public static HealthStateType forbiddenForGroup(String groupName) {
        return new HealthStateType("FORBIDDEN", HealthStateScope.forGroup(groupName), null);
    }

    public static HealthStateType forbidden() {
        return new HealthStateType("FORBIDDEN", HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType artifactsDiskFull() {
        return new HealthStateType("ARTIFACTS_DISK_FULL", HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType databaseDiskFull() {
        return new HealthStateType("DATABASE_DISK_FULL", HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType artifactsDirChanged() {
        return new HealthStateType("ARTIFACTS_DIR_CHANGED", HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType notFound() {
        return new HealthStateType("NOT_FOUND", HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType duplicateAgent(HealthStateScope scope) {
        return new HealthStateType("DUPLICATE_AGENT", scope, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HealthStateType that = (HealthStateType) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(scope, that.scope) &&
                Objects.equals(subkey, that.subkey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, scope, subkey);
    }

    public HealthStateScope getScope() {
        return scope;
    }

    public boolean isSameScope(HealthStateScope scope) {
        return this.scope.equals(scope);
    }

    @Override public String toString() {
        return "<HealthStateType " + name + " " + scope + ">";
    }

    public boolean isRemovedFromConfig(CruiseConfig cruiseConfig) {
        return scope.isRemovedFromConfig(cruiseConfig);
    }

    @Override
    public int compareTo(HealthStateType o) {
        return scope.compareTo(o.scope);
    }
}
