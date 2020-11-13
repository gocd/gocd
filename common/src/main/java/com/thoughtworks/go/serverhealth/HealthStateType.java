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
package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.config.CruiseConfig;

import java.util.Objects;
import java.util.Set;

public class HealthStateType implements Comparable<HealthStateType> {

    private String name;
    private final int httpCode;
    private final HealthStateScope scope;
    private String subkey;

    private HealthStateType(String name, int httpCode, HealthStateScope scope, String subkey) {
        this.name = name;
        this.httpCode = httpCode;
        this.scope = scope;
        this.subkey = subkey;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public static HealthStateType general(HealthStateScope scope) {
        return new HealthStateType("GENERAL", 406, scope, null);
    }

    public static HealthStateType withSubkey(HealthStateScope scope, String subkey) {
        return new HealthStateType("GENERAL_WITH_SUBKEY", 406, scope, subkey);
    }

    public static HealthStateType invalidConfig() {
        return new HealthStateType("INVALID_CONFIG", 406, HealthStateScope.GLOBAL, null);
    }
    public static HealthStateType invalidConfigMerge() {
        return new HealthStateType("INVALID_CONFIG_MERGE", 406, HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType forbiddenForPipeline(String pipelineName) {
        return new HealthStateType("FORBIDDEN", 403, HealthStateScope.forPipeline(pipelineName), null);
    }

    public static HealthStateType forbiddenForGroup(String groupName) {
        return new HealthStateType("FORBIDDEN", 403, HealthStateScope.forGroup(groupName), null);
    }

    public static HealthStateType forbidden() {
        return new HealthStateType("FORBIDDEN", 403, HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType invalidLicense(HealthStateScope scope) {
        return new HealthStateType("INVALID_LICENSE", 402, scope, null);
    }

    public static HealthStateType artifactsDiskFull() {
        return new HealthStateType("ARTIFACTS_DISK_FULL", 400, HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType databaseDiskFull() {
        return new HealthStateType("DATABASE_DISK_FULL", 400, HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType artifactsDirChanged() {
        return new HealthStateType("ARTIFACTS_DIR_CHANGED", 406, HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType notFound() {
        return new HealthStateType("NOT_FOUND", 404, HealthStateScope.GLOBAL, null);
    }

    public static HealthStateType duplicateAgent(HealthStateScope scope) {
        return new HealthStateType("DUPLICATE_AGENT", 406, scope, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthStateType that = (HealthStateType) o;
        return httpCode == that.httpCode &&
                Objects.equals(name, that.name) &&
                Objects.equals(scope, that.scope) &&
                Objects.equals(subkey, that.subkey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, httpCode, scope, subkey);
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

    public Set<String> getPipelineNames(CruiseConfig cruiseConfig) {
        return scope.getPipelineNames(cruiseConfig);
    }
}
