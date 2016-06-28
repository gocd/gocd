/*
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
 */

package com.thoughtworks.go.config.materials;

import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigReferenceElement;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.IgnoreTraversal;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import org.apache.commons.lang.StringUtils;

@ConfigTag(value = "package")
public class PackageMaterialConfig extends AbstractMaterialConfig {
    public static final String TYPE = "PackageMaterial";
    public static final String PACKAGE_ID = "packageId";

    @ConfigAttribute(value = "ref")
    private String packageId;

    @IgnoreTraversal
    @ConfigReferenceElement(referenceAttribute = "ref", referenceCollection = "packages")
    private com.thoughtworks.go.domain.packagerepository.PackageDefinition packageDefinition;

    public PackageMaterialConfig() {
        super(TYPE);
    }

    public PackageMaterialConfig(String packageId) {
        this();
        this.packageId = packageId;
    }

    public PackageMaterialConfig(CaseInsensitiveString name, String packageId, PackageDefinition packageDefinition) {
        super(TYPE);
        this.name = name;
        this.packageId = packageId;
        this.packageDefinition = packageDefinition;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getPluginId() {
        return getPackageDefinition().getRepository().getPluginConfiguration().getId();
    }

    public PackageDefinition getPackageDefinition() {
        return packageDefinition;
    }

    @Override
    public String toString() {
        return String.format("'PackageMaterial{%s}'", getLongDescription());
    }

    @Override
    public String getFingerprint() {
        if (packageDefinition == null) {
            return null;
        }
        return packageDefinition.getFingerprint(FINGERPRINT_DELIMITER);
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put("fingerprint", getFingerprint());
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("repositoryName", this.getPackageDefinition().getRepository().getName());
        parameters.put("packageName", this.getPackageDefinition().getName());
    }

    @Override
    protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        //do nothing
    }

    @Override
    protected void validateConcreteMaterial(ValidationContext validationContext) {
        if (StringUtils.isBlank(packageId)) {
            addError(PACKAGE_ID, "Please select a repository and package");
        }
    }

    @Override
    protected void validateExtras(ValidationContext validationContext) {
        if (!StringUtils.isBlank(packageId)) {
            PackageRepository packageRepository = validationContext.findPackageById(packageId);
            if (packageRepository == null) {
                addError(PACKAGE_ID, String.format("Could not find repository for given package id:[%s]", packageId));
            } else if (!packageRepository.doesPluginExist()) {
                addError(PACKAGE_ID, String.format("Could not find plugin for given package id:[%s].", packageId));
            }
        }
    }

    @Override
    public String getFolder() {
        return null;
    }

    @Override
    public Filter filter() {
        return new Filter() {

            @Override
            public boolean shouldNeverIgnore() {
                return true;
            }
        };
    }

    @Override
    public boolean isInvertFilter() {
        return false;
    }

    @Override
    public boolean matches(String name, String regex) {
        return false;
    }

    @Override
    public CaseInsensitiveString getName() {
        if (((name == null) || StringUtils.isEmpty(name.toString())) && packageDefinition != null) {
            return new CaseInsensitiveString(getPackageDefinition().getRepository().getName() + ":" + packageDefinition.getName());
        } else {
            return name;
        }
    }

    @Override
    public String getDescription() {
        return getDisplayName();
    }

    @Override
    public String getTypeForDisplay() {
        return "Package";
    }

    @Override
    public String getDisplayName() {
        return ((name == null || name.isBlank()) && getPackageDefinition().getRepository().getName() == null) ? getUriForDisplay() : getName().toString();
    }

    @Override
    public String getUriForDisplay() {
        return packageDefinition.getConfigForDisplay();
    }

    @Override
    public boolean isAutoUpdate() {
        return packageDefinition.isAutoUpdate();
    }

    @Override
    public void setAutoUpdate(boolean autoUpdate) {
        packageDefinition.setAutoUpdate(autoUpdate);
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        return Boolean.FALSE;
    }

    @Override
    public String getLongDescription() {
        return getUriForDisplay();
    }

    public void setPackageDefinition(PackageDefinition packageDefinition) {
        this.packageDefinition = packageDefinition;
        if (packageDefinition != null) {
            this.packageId = packageDefinition.getId();
        } else{
            this.packageId = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PackageMaterialConfig that = (PackageMaterialConfig) o;

        if (packageDefinition != null ? !packageDefinition.equals(that.packageDefinition) : that.packageDefinition != null) {
            return false;
        }
        return super.equals(that);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (packageId != null ? packageId.hashCode() : 0);
        return result;
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        this.packageId = (String) map.get(PACKAGE_ID);
    }

    @Override
    public void validateNameUniqueness(Map<CaseInsensitiveString, AbstractMaterialConfig> map) {
        if (StringUtils.isBlank(packageId)) {
            return;
        }
        if (map.containsKey(new CaseInsensitiveString(packageId))) {
            AbstractMaterialConfig material = map.get(new CaseInsensitiveString(packageId));
            material.addError(PACKAGE_ID, "Duplicate package material detected!");
            addError(PACKAGE_ID, "Duplicate package material detected!");
        } else {
            map.put(new CaseInsensitiveString(packageId), this);
        }
    }
}
