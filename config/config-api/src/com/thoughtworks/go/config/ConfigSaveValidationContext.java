/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;

import java.util.HashMap;

/**
 * @understands providing right state required to validate a given config element
 */
public class ConfigSaveValidationContext implements ValidationContext{
    private final Validatable immediateParent;
    private final ConfigSaveValidationContext parentContext;
    private HashMap<Class, Object> objectOfType;
    private HashMap<String, MaterialConfigs> fingerprintToMaterials = null;

    public ConfigSaveValidationContext(Validatable immediateParent) {
        this(immediateParent, null);
    }

    public ConfigSaveValidationContext(Validatable immediateParent, ConfigSaveValidationContext parentContext) {
        this.immediateParent = immediateParent;
        this.parentContext = parentContext;
        objectOfType = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigSaveValidationContext)) {
            return false;
        }

        ConfigSaveValidationContext that = (ConfigSaveValidationContext) o;

        if (immediateParent != null ? !immediateParent.equals(that.immediateParent) : that.immediateParent != null) {
            return false;
        }
        if (parentContext != null ? !parentContext.equals(that.parentContext) : that.parentContext != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = immediateParent != null ? immediateParent.hashCode() : 0;
        result = 31 * result + (parentContext != null ? parentContext.hashCode() : 0);
        return result;
    }

    @Override
    public ConfigSaveValidationContext withParent(Validatable current) {
        return new ConfigSaveValidationContext(current, this);
    }

    private CruiseConfig getCruiseConfig() {
        return loadFirstOfType(CruiseConfig.class);
    }

    @Override
    public ConfigReposConfig getConfigRepos() {
        return getCruiseConfig().getConfigRepos();
    }

    public JobConfig getJob() {
        return loadFirstOfType(JobConfig.class);
    }

    private <T> T loadFirstOfType(Class<T> klass) {
        T obj = getFirstOfType(klass);
        if (obj == null) {
            throw new IllegalArgumentException(String.format("Could not find an object of type '%s'.", klass.getCanonicalName()));
        }
        return obj;
    }

    private <T> T getFirstOfType(Class<T> klass) {
        Object o = objectOfType.get(klass);
        if (o == null) {
            o = _getFirstOfType(klass);
            objectOfType.put(klass, o);
        }
        return (T) o;
    }

    private <T> T _getFirstOfType(Class<T> klass) {
        if (parentContext != null) {
            T obj = parentContext.getFirstOfType(klass);
            if (obj != null) {
                return obj;
            }
        }

        if (immediateParent == null) {
            return null;
        } else if (immediateParent.getClass().equals(klass)) {
            return (T) immediateParent;
        }
        else
        {
            // added because of higher hierarchy of configuration types.
            // now there are interfaces with more than one implementation
            // so when asking for CruiseConfig there are 2 matching classes - BasicCruiseConfig and MergeCruiseConfig
            Class<?>[] interfacesOfCandidate = immediateParent.getClass().getInterfaces();
            for(Class<?> inter : interfacesOfCandidate)
            {
                if(inter.equals(klass))
                {
                    // candidate implements interface whose instances we are looking for
                    return (T) immediateParent;
                }
            }
        }

        return null;
    }

    public StageConfig getStage() {
        return loadFirstOfType(StageConfig.class);
    }

    public PipelineConfig getPipeline() {
        return loadFirstOfType(PipelineConfig.class);
    }

    public PipelineTemplateConfig getTemplate() {
        return loadFirstOfType(PipelineTemplateConfig.class);
    }

    @Override
    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        return getCruiseConfig().getPipelineConfigByName(pipelineName);
    }

    @Override
    public boolean shouldCheckConfigRepo() {
        return true;
    }

    @Override
    public SecurityConfig getServerSecurityConfig() {
        return getCruiseConfig().server().security();
    }

    @Override
    public boolean doesTemplateExist(CaseInsensitiveString template) {
        return getCruiseConfig().getTemplates().hasTemplateNamed(template);
    }

    @Override
    public SCM findScmById(String scmID) {
        return getCruiseConfig().getSCMs().find(scmID);
    }

    @Override
    public PackageRepository findPackageById(String packageId) {
        return getCruiseConfig().getPackageRepositories().findPackageRepositoryHaving(packageId);
    }

    public String getParentDisplayName() {
        return getParentType().getAnnotation(ConfigTag.class).value();
    }

    private Class<? extends Validatable> getParentType() {
        return getParent().getClass();
    }

    public Validatable getParent() {
        return immediateParent;
    }

    public boolean isWithinTemplates() {
        return hasParentOfType(TemplatesConfig.class);
    }

    public boolean isWithinPipelines() {
        return hasParentOfType(PipelineConfigs.class);
    }

    private <T> boolean hasParentOfType(Class<T> validatable) {
        return getFirstOfType(validatable) != null;
    }

    public PipelineConfigs getPipelineGroup() {
        return loadFirstOfType(PipelineConfigs.class);
    }

    @Override
    public String toString() {
        return "ValidationContext{" +
                "immediateParent=" + immediateParent +
                ", parentContext=" + parentContext +
                '}';
    }

    @Override
    public boolean isValidProfileId(String profileId) {
        return this.getCruiseConfig().server().getElasticConfig().getProfiles().find(profileId) != null;
    }

    public static ConfigSaveValidationContext forChain(Validatable... validatables) {
        ConfigSaveValidationContext tail = new ConfigSaveValidationContext(null);
        for (Validatable validatable : validatables) {
            tail = tail.withParent(validatable);
        }
        return tail;
    }

    public MaterialConfigs getAllMaterialsByFingerPrint(String fingerprint) {
        if (fingerprintToMaterials == null || fingerprintToMaterials.isEmpty()) {
            primeForMaterialValidations();
        }
        MaterialConfigs matchingMaterials = fingerprintToMaterials.get(fingerprint);
        return matchingMaterials == null ? new MaterialConfigs() : matchingMaterials;
    }

    private void primeForMaterialValidations() {
        CruiseConfig cruiseConfig = getCruiseConfig();
        fingerprintToMaterials = new HashMap<>();
        for (PipelineConfig pipelineConfig : cruiseConfig.getAllPipelineConfigs()) {
            for (MaterialConfig material : pipelineConfig.materialConfigs()) {
                String fingerprint = material.getFingerprint();
                if (!fingerprintToMaterials.containsKey(fingerprint)) {
                    fingerprintToMaterials.put(fingerprint, new MaterialConfigs());
                }
                MaterialConfigs materialsForFingerprint = fingerprintToMaterials.get(fingerprint);
                materialsForFingerprint.add(material);
            }
        }
    }
}
