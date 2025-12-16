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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ValidationContext {
    ConfigReposConfig getConfigRepos();

    boolean isWithinPipelines();

    PipelineConfig getPipeline();

    MaterialConfigs getAllMaterialsByFingerPrint(String fingerprint);

    StageConfig getStage();

    boolean isWithinTemplates();

    String getParentDisplayName();

    Validatable getParent();

    JobConfig getJob();

    boolean isWithinEnvironment();

    PipelineConfigs getPipelineGroup();

    PipelineTemplateConfig getTemplate();

    PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName);

    boolean shouldCheckConfigRepo();

    SecurityConfig getServerSecurityConfig();

    boolean doesTemplateExist(CaseInsensitiveString template);

    SCM findScmById(String scmID);

    PackageRepository findPackageById(String packageId);

    ValidationContext withParent(Validatable validatable);

    EnvironmentConfig getEnvironment();

    boolean isValidProfileId(String profileId);

    ClusterProfiles getClusterProfiles();

    boolean shouldNotCheckRole();

    ArtifactStores artifactStores();

    CruiseConfig getCruiseConfig();

    RulesValidationContext getRulesValidationContext();

    default PolicyValidationContext getPolicyValidationContext() {
        return null;
    }

    default Map<CaseInsensitiveString, Boolean> getPipelineToMaterialAutoUpdateMapByFingerprint(String fingerprint) {
        return getCruiseConfig().getAllPipelineConfigs().stream()
            .flatMap(pipeline -> pipeline.materialConfigs().stream()
                .filter(materialConfig -> fingerprint.equals(materialConfig.getFingerprint()))
                .findFirst()
                .map(materialConfig -> Map.entry(pipeline.name(), materialConfig.isAutoUpdate()))
                .stream()
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    record RulesValidationContext(List<String> allowedActions, List<String> allowedTypes) {}
    record PolicyValidationContext(List<String> allowedActions, List<String> allowedTypes) {}
}

