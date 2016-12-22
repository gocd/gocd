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
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialRevision;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
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

public class PluggableSCMMaterial extends AbstractMaterial {
    public static final String TYPE = "PluggableSCMMaterial";

    private String scmId;

    @Expose
    @SerializedName("scm")
    private SCM scmConfig;

    private String folder;

    private Filter filter;

    public PluggableSCMMaterial() {
        super(TYPE);
    }

    public PluggableSCMMaterial(String scmId) {
        this();
        this.scmId = scmId;
    }

    public PluggableSCMMaterial(PluggableSCMMaterialConfig config) {
        this();
        this.name = config.getName();
        this.scmId = config.getScmId();
        this.scmConfig = config.getSCMConfig();
        this.folder = config.getFolder();
        this.filter = config.filter();
    }

    @Override
    public MaterialConfig config() {
        return new PluggableSCMMaterialConfig(name, scmConfig, folder, filter);
    }

    @Override
    public Class getInstanceType() {
        return PluggableSCMMaterialInstance.class;
    }

    @Override
    public MaterialInstance createMaterialInstance() {
        return new PluggableSCMMaterialInstance(JsonHelper.toJsonString(this), UUID.randomUUID().toString());
    }

    public SCM getScmConfig() {
        return scmConfig;
    }

    public void setSCMConfig(SCM scmConfig) {
        this.scmConfig = scmConfig;
    }

    @Override
    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public File workingDirectory(File baseFolder) {
        if (folder == null) {
            return baseFolder;
        }
        return new File(baseFolder, folder);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String getPluginId() {
        return getScmConfig().getPluginConfiguration().getId();
    }

    @Override
    public String getFingerprint() {
        if (isEmpty(fingerprint)) {
            return scmConfig == null ? null : scmConfig.getFingerprint();
        }
        return fingerprint;
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put("fingerprint", getFingerprint());
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("scmName", CaseInsensitiveString.str(getName()));
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        basicCriteria.put("dest", folder);
    }

    @Override
    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, SubprocessExecutionContext execCtx) {
        // do nothing. used in tests.
    }

    @Override
    public void toJson(Map jsonMap, Revision revision) {
        jsonMap.put("scmType", getTypeForDisplay());
        jsonMap.put("materialName", getDisplayName());
        jsonMap.put("location", getUriForDisplay());
        jsonMap.put("folder", getFolder());
        jsonMap.put("action", "Modified");
    }

    // most of the material such as git, hg, p4 all print the file from the root without '/'. but svn print it with '/', we standardize it here.
    @Override
    public boolean matches(String name, String regex) {
        if (regex.startsWith("/")) {
            regex = regex.substring(1);
        }
        return name.matches(regex);
    }

    @Override
    public void emailContent(StringBuilder content, Modification modification) {
        String scmDetails = getTypeForDisplay() + " : " + getDisplayName();
        String revisionDetails = format("revision: %s, completed on %s\n%s", modification.getRevision(), modification.getModifiedTime(), modification.getComment());
        content.append(scmDetails).append('\n').append(revisionDetails);
    }

    private boolean nameIsEmpty() {
        return name == null || name.isBlank();
    }

    private boolean scmNameIsEmpty() {
        return (scmConfig == null || scmConfig.getName() == null || scmConfig.getName().isEmpty());
    }

    @Override
    public CaseInsensitiveString getName() {
        if (nameIsEmpty() && !scmNameIsEmpty()) {
            return new CaseInsensitiveString(scmConfig.getName());
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
        String type = scmConfig == null ? null : SCMMetadataStore.getInstance().displayValue(scmConfig.getPluginConfiguration().getId());
        return type == null ? "SCM" : type;
    }

    @Override
    public String getDisplayName() {
        CaseInsensitiveString name = getName();
        return (name == null || name.isBlank()) ? getUriForDisplay() : name.toString();
    }

    @Override
    public String getUriForDisplay() {
        return scmConfig.getConfigForDisplay();
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "scm");
        materialMap.put("plugin-id", getPluginId());
        Map<String, String> configurationMap = scmConfig.getConfiguration().getConfigurationAsMap(addSecureFields);
        materialMap.put("scm-configuration", configurationMap);
        return materialMap;
    }

    @Override
    public boolean isAutoUpdate() {
        return scmConfig.isAutoUpdate();
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
    public Revision oldestRevision(Modifications modifications) {
        if (modifications.isEmpty()) {
            return new NullRevision();
        }
        Modification modification = modifications.get(modifications.size() - 1);
        return new PluggableSCMMaterialRevision(modification.getRevision(), modification.getModifiedTime());
    }

    @Override
    public String getLongDescription() {
        return getUriForDisplay();
    }

    @Override
    public void populateEnvironmentContext(EnvironmentVariableContext context, MaterialRevision materialRevision, File workingDir) {
        context.setProperty(getEnvironmentVariableKey("GO_SCM_%s_%s", "LABEL"), materialRevision.getRevision().getRevision(), false);
        for (ConfigurationProperty configurationProperty : scmConfig.getConfiguration()) {
            context.setProperty(getEnvironmentVariableKey("GO_SCM_%s_%s", configurationProperty.getConfigurationKey().getName()),
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
                context.setProperty(getEnvironmentVariableKey("GO_SCM_%s_%s", key), value, isSecure);
            }
        }
    }

    private String getEnvironmentVariableKey(String keyPattern, String givenKey) {
        return escapeEnvironmentVariable(upperCase(format(keyPattern, getName().toString(), givenKey)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PluggableSCMMaterial that = (PluggableSCMMaterial) o;

        if (this.getFingerprint() != null ? !this.getFingerprint().equals(that.getFingerprint()) : that.getFingerprint() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.getFingerprint() != null ? this.getFingerprint().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("'PluggableSCMMaterial{%s}'", getLongDescription());
    }

    @Override
    public boolean supportsDestinationFolder() {
        return true;
    }

    @Override
    public void updateFromConfig(MaterialConfig materialConfig) {
        super.updateFromConfig(materialConfig);
        this.getScmConfig().setConfiguration(((PluggableSCMMaterialConfig)materialConfig).getSCMConfig().getConfiguration());
    }
}
