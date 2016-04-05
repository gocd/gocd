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

package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.config.CruiseConfig;

import java.util.Set;

public class HealthStateType implements Comparable<HealthStateType> {

    private String name;
    private final int httpCode;
    private final HealthStateScope scope;

    private HealthStateType(String name, int httpCode, HealthStateScope scope) {
        this.name = name;
        this.httpCode = httpCode;
        this.scope = scope;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public static HealthStateType general(HealthStateScope scope) {
        return new HealthStateType("GENERAL", 406, scope);
    }

    public static HealthStateType invalidConfig() {
        return new HealthStateType("INVALID_CONFIG", 406, HealthStateScope.GLOBAL);
    }
    public static HealthStateType invalidConfigMerge() {
        return new HealthStateType("INVALID_CONFIG_MERGE", 406, HealthStateScope.GLOBAL);
    }

    public static HealthStateType unauthorisedForPipeline(String pipelineName) {
        return new HealthStateType("UNAUTHORIZED", 401, HealthStateScope.forPipeline(pipelineName));
    }

    public static HealthStateType unauthorisedForGroup(String groupName) {
        return new HealthStateType("UNAUTHORIZED", 401, HealthStateScope.forGroup(groupName));
    }

    public static HealthStateType unauthorised() {
        return new HealthStateType("UNAUTHORIZED", 401, HealthStateScope.GLOBAL);
    }

    public static HealthStateType invalidLicense(HealthStateScope scope) {
        return new HealthStateType("INVALID_LICENSE", 402, scope);
    }

    public static HealthStateType expiredLicense(HealthStateScope scope) {
        return new HealthStateType("EXPIRED_LICENSE", 402, scope);
    }

    public static HealthStateType userLimitExceeded(HealthStateScope scope) {
        return new HealthStateType("USER_LIMIT_EXCEEDED", 402, scope);
    }

    public static HealthStateType exceedsAgentLimit(HealthStateScope scope) {
        return new HealthStateType("EXCEEDS_AGENT_LIMIT", 402, scope);
    }

    public static HealthStateType artifactsDiskFull() {
        return new HealthStateType("ARTIFACTS_DISK_FULL", 400, HealthStateScope.GLOBAL);
    }

    public static HealthStateType databaseDiskFull() {
        return new HealthStateType("DATABASE_DISK_FULL", 400, HealthStateScope.GLOBAL);
    }

    public static HealthStateType artifactsDirChanged() {
        return new HealthStateType("ARTIFACTS_DIR_CHANGED", 406, HealthStateScope.GLOBAL);
    }

    public static HealthStateType autoregisterKeyRequired() {
        return new HealthStateType("AUTO_REGISTER_KEY_REQUIRED", 406, HealthStateScope.GLOBAL);
    }

    public static HealthStateType commandRepositoryAccessibilityIssue() {
        return new HealthStateType("COMMAND_REPOSITORY_ERROR", 406, HealthStateScope.GLOBAL);
    }

    public static HealthStateType commandRepositoryUpgradeIssue() {
        return new HealthStateType("COMMAND_REPOSITORY_UPGRADE_ERROR", 406, HealthStateScope.GLOBAL);
    }

    public static HealthStateType notFound() {
        return new HealthStateType("NOT_FOUND", 404, HealthStateScope.GLOBAL);
    }

    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (that == null) {return false; }
        if (this.getClass() != that.getClass()) { return false; }

        return equals((HealthStateType) that);
    }

    private boolean equals(HealthStateType that) {
        if (!name.equals(that.name)) { return false; }
        if (this.httpCode != that.httpCode) { return false; }
        if (!scope.equals(that.scope)) { return false; }
        return true;
    }

    public int hashCode() {
        int result;
        result = httpCode;
        result = 31 * result + name.hashCode();
        result = 31 * result + scope.hashCode();
        return result;
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


    public static HealthStateType duplicateAgent(HealthStateScope scope) {
        return new HealthStateType("DUPLICATE_AGENT", 406, scope);
    }

    public int compareTo(HealthStateType o) {
        return scope.compareTo(o.scope);
    }

    public Set<String> getPipelineNames(CruiseConfig cruiseConfig) {
        return scope.getPipelineNames(cruiseConfig);
    }
}
