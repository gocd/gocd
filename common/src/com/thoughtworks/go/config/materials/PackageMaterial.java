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

package com.thoughtworks.go.config.materials;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.json.JsonHelper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.util.command.EnvironmentVariableContext.escapeEnvironmentVariable;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.upperCase;

public class PackageMaterial extends AbstractMaterial {
    public static final String TYPE = "PackageMaterial";

    private String packageId;

    @Expose
    @SerializedName("package")
    private PackageDefinition packageDefinition;

    public PackageMaterial() {
        super(TYPE);
    }

    public PackageMaterial(String packageId) {
        this();
        this.packageId = packageId;
    }

    public PackageMaterial(PackageMaterialConfig config) {
        super(TYPE);
        this.name = config.getName();
        this.packageId = config.getPackageId();
        this.packageDefinition = config.getPackageDefinition();
    }

    @Override
    public String toString() {
        return String.format("'PackageMaterial{%s}'", getLongDescription());
    }

    @Override
    public MaterialConfig config() {
        return new PackageMaterialConfig(this.name, this.packageId, this.packageDefinition);
    }

    public String getPluginId() {
        return getPackageDefinition().getRepository().getPluginConfiguration().getId();
    }

    public PackageDefinition getPackageDefinition() {
        return packageDefinition;
    }

    @Override
    public String getFingerprint() {
        if (isEmpty(fingerprint)) {
            return packageDefinition == null ? null : packageDefinition.getFingerprint(FINGERPRINT_DELIMITER);
        }
        return fingerprint;
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
    public String getFolder() {
        return null;
    }

    @Override
    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, SubprocessExecutionContext execCtx) {
        //do nothing
    }

    @Override
    public void toJson(Map jsonMap, Revision revision) {
        jsonMap.put("scmType", getTypeForDisplay());
        jsonMap.put("action", "Modified");
        jsonMap.put("location", getUriForDisplay());
        jsonMap.put("materialName", getDisplayName());
    }

    @Override
    public boolean matches(String name, String regex) {
        return false;
    }

    @Override
    public void emailContent(StringBuilder content, Modification modification) {
        content.append(getTypeForDisplay() + " : " + getDisplayName()).append('\n').append(
                format("revision: %s, completed on %s", modification.getRevision(),
                        modification.getModifiedTime()));
    }

    @Override
    public MaterialInstance createMaterialInstance() {
        return new PackageMaterialInstance(JsonHelper.toJsonString(this), UUID.randomUUID().toString());
    }

    @Override
    public CaseInsensitiveString getName() {
        if (((name == null) || isEmpty(name.toString())) && packageDefinition != null) {
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
    public void populateEnvironmentContext(EnvironmentVariableContext context, MaterialRevision materialRevision, File workingDir) {
        context.setProperty(upperCase(format("GO_PACKAGE_%s_LABEL", escapeEnvironmentVariable(getName().toString()))), materialRevision.getRevision().getRevision(), false);
        for (ConfigurationProperty configurationProperty : getPackageDefinition().getRepository().getConfiguration()) {
            context.setProperty(getEnvironmentVariableKey("GO_REPO_%s_%s", configurationProperty.getConfigurationKey().getName()),
                    configurationProperty.getValue(), configurationProperty.isSecure());
        }
        for (ConfigurationProperty configurationProperty : getPackageDefinition().getConfiguration()) {
            context.setProperty(getEnvironmentVariableKey("GO_PACKAGE_%s_%s", configurationProperty.getConfigurationKey().getName()),
                    configurationProperty.getValue(), configurationProperty.isSecure());
        }
        HashMap<String, String> additionalData = materialRevision.getLatestModification().getAdditionalDataMap();
        if (additionalData != null) {
            for (Map.Entry<String, String> entry : additionalData.entrySet()) {
                boolean isSecure = false;
                for (EnvironmentVariableContext.EnvironmentVariable secureEnvironmentVariable : context.getSecureEnvironmentVariables()) {
                    String urlEncodedValue = null;
                    try {
                        urlEncodedValue = URLEncoder.encode(secureEnvironmentVariable.value(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    boolean isSecureEnvironmentVariableEncoded = !StringUtil.isBlank(urlEncodedValue) && !secureEnvironmentVariable.value().equals(urlEncodedValue);
                    if (isSecureEnvironmentVariableEncoded && entry.getValue().contains(urlEncodedValue)) {
                        isSecure = true;
                        break;
                    }
                }

                String key = entry.getKey();
                String value = entry.getValue();
                context.setProperty(getEnvironmentVariableKey("GO_PACKAGE_%s_%s", key), value, isSecure);
            }
        }
    }

    private String getEnvironmentVariableKey(String keyPattern, String givenKey) {
        return escapeEnvironmentVariable(upperCase(format(keyPattern, getName().toString(), givenKey)));
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
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "package");
        materialMap.put("plugin-id", getPluginId());
        Map<String, String> repositoryConfigurationMap = packageDefinition.getRepository().getConfiguration().getConfigurationAsMap(addSecureFields);
        materialMap.put("repository-configuration", repositoryConfigurationMap);
        Map<String, String> packageConfigurationMap = packageDefinition.getConfiguration().getConfigurationAsMap(addSecureFields);
        materialMap.put("package-configuration", packageConfigurationMap);
        return materialMap;
    }

    @Override
    public boolean isAutoUpdate() {
        return packageDefinition.isAutoUpdate();
    }

    @Override
    public MatchedRevision createMatchedRevision(Modification modification, String searchString) {
        return new MatchedRevision(searchString, modification.getRevision(), modification.getRevision(), modification.getUserName(), modification.getModifiedTime(), modification.getComment());
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        return Boolean.FALSE;
    }

    @Override
    public Class getInstanceType() {
        return PackageMaterialInstance.class;
    }

    @Override
    public Revision oldestRevision(Modifications modifications) {
        if (modifications.isEmpty()) {
            return new NullRevision();
        }
        Modification modification = modifications.get(0);
        return new PackageMaterialRevision(modification.getRevision(), modification.getModifiedTime());
    }

    @Override
    public String getLongDescription() {
        return getUriForDisplay();
    }

    public void setPackageDefinition(PackageDefinition packageDefinition) {
        this.packageDefinition = packageDefinition;
    }

    @Override
    public void updateFromConfig(MaterialConfig materialConfig) {
        super.updateFromConfig(materialConfig);
        this.getPackageDefinition().setConfiguration(((PackageMaterialConfig)materialConfig).getPackageDefinition().getConfiguration());
        this.getPackageDefinition().getRepository().setConfiguration(((PackageMaterialConfig)materialConfig).getPackageDefinition().getRepository().getConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PackageMaterial that = (PackageMaterial) o;

        if (this.getFingerprint() != null ? !this.getFingerprint().equals(that.getFingerprint()) : that.getFingerprint() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (packageId != null ? packageId.hashCode() : 0);
        return result;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
