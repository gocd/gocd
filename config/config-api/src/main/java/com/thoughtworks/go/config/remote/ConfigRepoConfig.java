/*
 * Copyright 2023 Thoughtworks, Inc.
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
package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.MaterialErrors;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMetadataStore;
import com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.materials.ScmMaterialConfig.AUTO_UPDATE;
import static com.thoughtworks.go.config.rules.SupportedEntity.*;
import static java.lang.String.format;
import static java.util.Objects.isNull;

/**
 * Defines single source of remote configuration and name of plugin to interpret it.
 * This goes to standard static xml configuration.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@ConfigTag("config-repo")
@NoArgsConstructor
public class ConfigRepoConfig extends RuleAwarePluginProfile {
    private List<String> allowedActions = List.of("refer");
    private List<String> allowedTypes = unmodifiableListOf(PIPELINE, PIPELINE_GROUP, ENVIRONMENT);
    // defines source of configuration. Any will fit
    @ConfigSubtag(optional = false)
    private MaterialConfig repo;

    public static ConfigRepoConfig createConfigRepoConfig(MaterialConfig repo, String pluginId, String id) {
        return (ConfigRepoConfig) new ConfigRepoConfig().setRepo(repo).setPluginId(pluginId).setId(id);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        super.validate(validationContext);
        this.validateUniquenessOfId(validationContext);
        this.validateRepoIsSet();
        this.validateMaterial(validationContext);
        if (isValidMaterial()) {
            this.validateAutoUpdateIsConsistentAmongSimilarMaterials(validationContext);
            this.validateMaterialUniqueness(validationContext);
        }
    }

    private void validateMaterialUniqueness(ValidationContext validationContext) {
        if (getRepo() != null) {
            if (!validationContext.getCruiseConfig().getConfigRepos().isUniqueMaterial(getRepo().getFingerprint())) {
                this.errors.add("material", format(
                        "You have defined multiple configuration repositories with the same repository - '%s'.",
                        this.repo.getDisplayName()));
            }
        }
    }

    private void validateUniquenessOfId(ValidationContext validationContext) {
        if (!validationContext.getCruiseConfig().getConfigRepos().isUniqueId(id)) {
            this.errors.add(ID, format("You have defined multiple configuration repositories with the same id - '%s'.", id));
        }
    }

    private void validateMaterial(ValidationContext validationContext) {
        MaterialConfig materialConfig = getRepo();

        if (materialConfig != null) {
            materialConfig.validateTree(validationContext);
        }
    }

    private boolean isValidMaterial() {
        MaterialConfig materialConfig = getRepo();
        if (materialConfig == null) {
            return false;
        }

        return materialConfig.errors().isEmpty();
    }

    private void validateRepoIsSet() {
        if (getRepo() == null) {
            this.errors.add("material", "Configuration repository material not specified.");
        }
    }

    public boolean hasSameMaterial(MaterialConfig config) {
        return getRepo().getFingerprint().equals(config.getFingerprint());
    }

    public boolean hasMaterialWithFingerprint(String fingerprint) {
        return getRepo().getFingerprint().equals(fingerprint);
    }

    private void validateAutoUpdateIsConsistentAmongSimilarMaterials(ValidationContext validationContext) {
        if (validationContext == null)
            return;

        MaterialConfig material = getRepo();
        if (material != null) {
            MaterialConfigs allMaterialsByFingerPrint = validationContext.getAllMaterialsByFingerPrint(material.getFingerprint());
            if (allMaterialsByFingerPrint.stream().anyMatch(m -> material.isAutoUpdate() != m.isAutoUpdate())) {
                String message = format(
                    "The material of type %s (%s) is used elsewhere with a different value for autoUpdate (poll for changes). " +
                        "All copies of this material must have the same autoUpdate setting or configuration repository must be removed.\n" +
                        "Config Repository: %s (%s).\n",
                    material.getTypeForDisplay(),
                    material.getDescription(),
                    getId(), MaterialErrors.autoUpdateDisplayStatus(material.isAutoUpdate()));
                Map<CaseInsensitiveString, Boolean> pipelinesWithMaterial = validationContext.getPipelineToMaterialAutoUpdateMapByFingerprint(material.getFingerprint());
                if (!pipelinesWithMaterial.isEmpty()) {
                    message = message.concat(format("Pipelines:\n%s", MaterialErrors.autoUpdatePipelineErrorDisplay(pipelinesWithMaterial)));
                }
                getRepo().errors().add(AUTO_UPDATE, message);
            }
        }
    }

    @Override
    public List<String> allowedActions() {
        return allowedActions;
    }

    @Override
    public List<String> allowedTypes() {
        return allowedTypes;
    }

    @Override
    protected String getObjectDescription() {
        return "Configuration repository";
    }

    @Override
    protected boolean isSecure(String key) {
        ConfigRepoPluginInfo pluginInfo = this.metadataStore().getPluginInfo(getPluginId());

        if (pluginInfo == null
                || pluginInfo.getPluginSettings() == null
                || pluginInfo.getPluginSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getPluginSettings().getConfiguration(key).isSecure();
    }

    @Override
    protected boolean hasPluginInfo() {
        return !isNull(this.metadataStore().getPluginInfo(getPluginId()));
    }

    private ConfigRepoMetadataStore metadataStore() {
        return ConfigRepoMetadataStore.instance();
    }
}
