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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class HealthStateScope implements Comparable<HealthStateScope> {
    public static final HealthStateScope GLOBAL = new HealthStateScope(ScopeType.GLOBAL, "GLOBAL");
    private final ScopeType type;
    private final String scope;

    private HealthStateScope(ScopeType type, String scope) {
        this.type = type;
        this.scope = scope;
    }

    public static HealthStateScope forGroup(String groupName) {
        return new HealthStateScope(ScopeType.GROUP, groupName);
    }

    public static HealthStateScope forPipeline(String pipelineName) {
        return new HealthStateScope(ScopeType.PIPELINE, pipelineName);
    }

    public static HealthStateScope forFanin(String pipelineName) {
        return new HealthStateScope(ScopeType.FANIN, pipelineName);
    }

    public static HealthStateScope forStage(String pipelineName, String stageName) {
        return new HealthStateScope(ScopeType.STAGE, pipelineName + "/" + stageName);
    }

    public static HealthStateScope forJob(String pipelineName, String stageName, String jobName) {
        return new HealthStateScope(ScopeType.JOB, pipelineName + "/" + stageName + "/" + jobName);
    }

    public static HealthStateScope forMaterial(Material material) {
        return new HealthStateScope(ScopeType.MATERIAL, material.getSqlCriteria().toString());
    }

    public static HealthStateScope forMaterialUpdate(Material material) {
        return new HealthStateScope(ScopeType.MATERIAL_UPDATE, material.getFingerprint());
    }

    public static HealthStateScope forMaterialConfig(MaterialConfig materialConfig) {
        return new HealthStateScope(ScopeType.MATERIAL, materialConfig.getSqlCriteria().toString());
    }

    public static HealthStateScope forMaterialConfigUpdate(MaterialConfig materialConfig) {
        return new HealthStateScope(ScopeType.MATERIAL_UPDATE, materialConfig.getFingerprint());
    }

    public static HealthStateScope forConfigRepo(String operation) {
        return new HealthStateScope(ScopeType.CONFIG_REPO, operation);
    }

    public static HealthStateScope forPartialConfigRepo(ConfigRepoConfig repoConfig) {
        return new HealthStateScope(ScopeType.CONFIG_PARTIAL, repoConfig.getMaterialConfig().getFingerprint());
    }

    public static HealthStateScope forPartialConfigRepo(String fingerprint) {
        return new HealthStateScope(ScopeType.CONFIG_PARTIAL, fingerprint);
    }

    public boolean isSame(String scope) {
        return StringUtils.endsWithIgnoreCase(this.scope, scope);
    }

    public boolean isForPipeline() {
        return type == ScopeType.PIPELINE;
    }

    public boolean isForGroup() {
        return type == ScopeType.GROUP;
    }

    public boolean isForMaterial() {
        return type == ScopeType.MATERIAL;
    }

    ScopeType getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    public String toString() {
        return String.format("LogScope[%s, scope=%s]", type, scope);
    }

    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (that == null) { return false; }
        if (getClass() != that.getClass()) { return false; }
        return equals((HealthStateScope) that);
    }

    private boolean equals(HealthStateScope that) {
        if (type != that.type) { return false; }
        if (!scope.equals(that.scope)) { return false; }
        return true;
    }

    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }

    public boolean isRemovedFromConfig(CruiseConfig cruiseConfig) {
        return type.isRemovedFromConfig(cruiseConfig, scope);
    }

    public static HealthStateScope forAgent(String cookie) {
        return new HealthStateScope(ScopeType.GLOBAL, cookie);
    }

    public static HealthStateScope forInvalidConfig() {
        return new HealthStateScope(ScopeType.GLOBAL, "global");
    }

    public int compareTo(HealthStateScope o) {
        int comparison;
        comparison = type.compareTo(o.type);
        if (comparison != 0) {
            return comparison;
        }
        comparison = scope.compareTo(o.scope);
        if (comparison != 0) {
            return comparison;
        }
        return 0;
    }

    public static HealthStateScope forPlugin(String symbolicName) {
        return new HealthStateScope(ScopeType.PLUGIN, symbolicName);
    }

    public Set<String> getPipelineNames(CruiseConfig config) {
        HashSet<String> pipelineNames = new HashSet<>();
        switch (type) {
            case PIPELINE:
            case FANIN:
                pipelineNames.add(scope);
                break;
            case STAGE:
            case JOB:
                pipelineNames.add(scope.split("/")[0]);
                break;
            case MATERIAL:
                for (PipelineConfig pc : config.getAllPipelineConfigs()) {
                    for (MaterialConfig mc : pc.materialConfigs()) {
                        String scope = HealthStateScope.forMaterialConfig(mc).getScope();
                        if (scope.equals(this.scope)) {
                            pipelineNames.add(pc.name().toString());
                        }
                    }
                }
                break;
            case MATERIAL_UPDATE:
                for (PipelineConfig pc : config.getAllPipelineConfigs()) {
                    for (MaterialConfig mc : pc.materialConfigs()) {
                        String scope = HealthStateScope.forMaterialConfigUpdate(mc).getScope();
                        if (scope.equals(this.scope)) {
                            pipelineNames.add(pc.name().toString());
                        }
                    }
                }
                break;
        }

        return pipelineNames;
    }

    public boolean isForConfigPartial() {
        return type == ScopeType.CONFIG_PARTIAL;
    }

    enum ScopeType {

        GLOBAL,
        CONFIG_REPO,
        GROUP {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String group) {
                return !cruiseConfig.hasPipelineGroup(group);
            }
        },
        MATERIAL {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String materialScope) {
                for (MaterialConfig materialConfig : cruiseConfig.getAllUniqueMaterials()) {
                    if (HealthStateScope.forMaterialConfig(materialConfig).getScope().equals(materialScope)) {
                        return false;
                    }
                }
                return true;
            }
        },
        MATERIAL_UPDATE {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String materialScope) {
                for (MaterialConfig materialConfig : cruiseConfig.getAllUniqueMaterials()) {
                    if (HealthStateScope.forMaterialConfigUpdate(materialConfig).getScope().equals(materialScope)) {
                        return false;
                    }
                }
                return true;
            }
        },
        CONFIG_PARTIAL {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String materialScope) {
                for (ConfigRepoConfig configRepoConfig : cruiseConfig.getConfigRepos()) {
                    if (HealthStateScope.forPartialConfigRepo(configRepoConfig).getScope().equals(materialScope)) {
                        return false;
                    }
                }
                return true;
            }
        },
        PIPELINE {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String pipeline) {
                return !cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipeline));
            }
        },
        FANIN {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String pipeline) {
                return !cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipeline));
            }
        },
        STAGE {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String pipelineStage) {
                String[] parts = pipelineStage.split("/");
                return !cruiseConfig.hasStageConfigNamed(new CaseInsensitiveString(parts[0]), new CaseInsensitiveString(parts[1]), true);
            }
        },
        JOB {
            public boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String pipelineStageJob) {
                String[] parts = pipelineStageJob.split("/");
                return !cruiseConfig.hasBuildPlan(new CaseInsensitiveString(parts[0]), new CaseInsensitiveString(parts[1]), parts[2], true);
            }
        }, PLUGIN;


        protected boolean isRemovedFromConfig(CruiseConfig cruiseConfig, String scope) {
            return false;
        };

    }
}
