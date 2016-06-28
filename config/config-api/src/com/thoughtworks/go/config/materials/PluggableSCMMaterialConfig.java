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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@ConfigTag(value = "scm")
public class PluggableSCMMaterialConfig extends AbstractMaterialConfig {
    public static final String TYPE = "PluggableSCMMaterial";
    public static final String SCM_ID = "scmId";
    public static final String FOLDER = "folder";
    public static final String FILTER = "filterAsString";

    @ConfigAttribute(value = "ref")
    private String scmId;

    @IgnoreTraversal
    @ConfigReferenceElement(referenceAttribute = "ref", referenceCollection = "scms")
    private SCM scmConfig;

    @ConfigAttribute(value = "dest", allowNull = true)
    protected String folder;

    @ConfigSubtag
    private Filter filter;

    public PluggableSCMMaterialConfig() {
        super(TYPE);
    }

    public PluggableSCMMaterialConfig(String scmId) {
        this();
        this.scmId = scmId;
    }

    public PluggableSCMMaterialConfig(CaseInsensitiveString name, SCM scmConfig, String folder, Filter filter) {
        super(TYPE);
        this.name = name;
        this.scmId = scmConfig == null ? null : scmConfig.getSCMId();
        this.scmConfig = scmConfig;
        this.folder = folder;
        this.filter = filter;
    }

    public String getScmId() {
        return scmId;
    }

    public void setScmId(String scmId) {
        this.scmId = scmId;
    }

    public SCM getSCMConfig() {
        return scmConfig;
    }

    public void setSCMConfig(SCM scmConfig) {
        this.scmConfig = scmConfig;
        this.scmId = scmConfig == null ? null : scmConfig.getId();
    }

    @Override
    public String getFolder() {
        return folder;
    }

    public File workingdir(File baseFolder) {
        if (getFolder() == null) {
            return baseFolder;
        }
        return new File(baseFolder, getFolder());
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Override
    public Filter filter() {
        if (filter == null) {
            return new Filter();
        }
        return filter;
    }

    public String getFilterAsString() {
        return filter().getStringForDisplay();
    }

    @Override
    public boolean isInvertFilter() {
        return false;
    }

    // most of the material such as git, hg, p4 all print the file from the root without '/'. but svn print it with '/', we standardize it here.
    @Override
    public boolean matches(String name, String regex) {
        if (regex.startsWith("/")) {
            regex = regex.substring(1);
        }
        return name.matches(regex);
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String getPluginId() {
        return scmConfig.getPluginConfiguration().getId();
    }

    @Override
    public String getFingerprint() {
        if (scmConfig == null) {
            return null;
        }
        return scmConfig.getFingerprint();
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put("fingerprint", getFingerprint());
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("scmName", scmConfig.getName());
    }

    @Override
    protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        basicCriteria.put("dest", folder);
    }

    @Override
    public String getTypeForDisplay() {
        String type = scmConfig == null ? null : SCMMetadataStore.getInstance().displayValue(scmConfig.getPluginConfiguration().getId());
        return type == null ? "SCM" : type;
    }

    @Override
    public boolean isAutoUpdate() {
        return scmConfig.isAutoUpdate();
    }

    @Override
    public void setAutoUpdate(boolean autoUpdate) {
        scmConfig.setAutoUpdate(autoUpdate);
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        return Boolean.FALSE;
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        this.scmId = (String) map.get(SCM_ID);
        if (map.containsKey(FOLDER)) {
            String folder = (String) map.get(FOLDER);
            if (StringUtils.isBlank(folder)) {
                folder = null;
            }
            this.folder = folder;
        }
        if (map.containsKey(FILTER)) {
            String pattern = (String) map.get(FILTER);
            if (!StringUtil.isBlank(pattern)) {
                this.setFilter(Filter.fromDisplayString(pattern));
            } else {
                this.setFilter(null);
            }
        }
    }

    private boolean nameIsEmpty() {
        return (name == null || name.isBlank());
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
    public String getDisplayName() {
        CaseInsensitiveString name = getName();
        return name == null || name.isBlank() ? getUriForDisplay() : CaseInsensitiveString.str(name);
    }

    @Override
    public String getDescription() {
        return getDisplayName();
    }

    @Override
    public String getLongDescription() {
        return getUriForDisplay();
    }

    @Override
    public String getUriForDisplay() {
        return scmConfig.getConfigForDisplay();
    }

    @Override
    protected void validateConcreteMaterial(ValidationContext validationContext) {
        validateDestFolderPath();
        validateNotOutsideSandbox();
        validateScmID(validationContext);
    }

    private void validateScmID(ValidationContext validationContext) {
        if (StringUtils.isBlank(scmId)) {
            addError(SCM_ID, "Please select a SCM");
        }
    }

    @Override
    protected void validateExtras(ValidationContext validationContext) {
        if (!StringUtil.isBlank(scmId)) {
            SCM scm = validationContext.findScmById(scmId);
            if (scm == null) {
                addError(SCM_ID, String.format("Could not find SCM for given scm-id: [%s].", scmId));
            } else if (!scm.doesPluginExist()) {
                addError(SCM_ID, String.format("Could not find plugin for scm-id: [%s].", scmId));
            }
        }
    }

    private void validateDestFolderPath() {
        if (StringUtils.isBlank(folder)) {
            return;
        }
        if (!new FilePathTypeValidator().isPathValid(folder)) {
            addError(FOLDER, FilePathTypeValidator.errorMessage("directory", getFolder()));
        }
    }

    private void validateNotOutsideSandbox() {
        String dest = this.getFolder();
        if (dest == null) {
            return;
        }
        if (!(FileUtil.isFolderInsideSandbox(dest))) {
            addError(FOLDER, String.format("Dest folder '%s' is not valid. It must be a sub-directory of the working folder.", dest));
        }
    }

    public void validateNotSubdirectoryOf(String otherSCMMaterialFolder) {
        String myDirPath = this.getFolder();
        if (myDirPath == null || otherSCMMaterialFolder == null) {
            return;
        }
        File myDir = new File(myDirPath);
        File otherDir = new File(otherSCMMaterialFolder);
        try {
            if (FileUtil.isSubdirectoryOf(myDir, otherDir)) {
                addError(FOLDER, "Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested.");
            }
        } catch (IOException e) {
            throw bomb("Dest folder specification is not valid. " + e.getMessage());
        }
    }

    public void validateDestinationDirectoryName(String otherSCMMaterialFolder) {
        if (folder != null && folder.equalsIgnoreCase(otherSCMMaterialFolder)) {
            addError(FOLDER, "The destination directory must be unique across materials.");
        }
    }

    @Override
    public void validateNameUniqueness(Map<CaseInsensitiveString, AbstractMaterialConfig> map) {
        if (StringUtils.isBlank(scmId)) {
            return;
        }
        if (map.containsKey(new CaseInsensitiveString(scmId))) {
            AbstractMaterialConfig material = map.get(new CaseInsensitiveString(scmId));
            material.addError(SCM_ID, "Duplicate SCM material detected!");
            addError(SCM_ID, "Duplicate SCM material detected!");
        } else {
            map.put(new CaseInsensitiveString(scmId), this);
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

        PluggableSCMMaterialConfig that = (PluggableSCMMaterialConfig) o;

        if (folder != null ? !folder.equals(that.folder) : that.folder != null) {
            return false;
        }

        if (scmConfig != null ? !scmConfig.equals(that.scmConfig) : that.scmConfig != null) {
            return false;
        }

        return super.equals(that);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (scmId != null ? scmId.hashCode() : 0);
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("'PluggableSCMMaterial{%s}'", getLongDescription());
    }
}
