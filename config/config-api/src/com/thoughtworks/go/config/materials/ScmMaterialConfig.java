/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.StringUtils;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands a source control repository and its configuration
 */
public abstract class ScmMaterialConfig extends AbstractMaterialConfig implements ParamsAttributeAware {

    public static final String URL = "url";
    public static final String USERNAME = "username";

    @ConfigSubtag
    private Filter filter;

    @ConfigAttribute(value = "dest", allowNull = true)
    protected String folder;

    @ConfigAttribute(value = "autoUpdate", optional = true)
    private boolean autoUpdate = true;

    public static final String PASSWORD = "password";
    public static final String ENCRYPTED_PASSWORD = "encryptedPassword";
    public static final String PASSWORD_CHANGED = "passwordChanged";
    public static final String AUTO_UPDATE = "autoUpdate";

    public static final String FOLDER = "folder";
    public static final String FILTER = "filterAsString";

    public ScmMaterialConfig(String typeName) {
        super(typeName);
    }

    public ScmMaterialConfig(CaseInsensitiveString name, Filter filter, String folder, boolean autoUpdate, String typeName, ConfigErrors errors) {
        super(typeName, name, errors);
        this.filter = filter;
        this.folder = folder;
        this.autoUpdate = autoUpdate;
    }

    @Override protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        basicCriteria.put("dest", folder);
    }

    public File workingdir(File baseFolder) {
        if (getFolder() == null) {
            return baseFolder;
        }
        return new File(baseFolder, getFolder());
    }

    //most of the material such as hg, git, p4 all print the file from the root without '/'
    //but subverion print it with '/', we standarize it here. look at the implementation of subversion as well.
    public boolean matches(String name, String regex) {
        if (regex.startsWith("/")) {
            regex = regex.substring(1);
        }
        return name.matches(regex);
    }

    public abstract String getUserName();

    public abstract String getPassword();

    public abstract String getEncryptedPassword();

    public abstract boolean isCheckExternals();

    public abstract String getUrl();

    protected abstract UrlArgument getUrlArgument();

    protected abstract String getLocation();

    public Filter filter() {
        if (filter == null) {
            return new Filter();
        }
        return filter;
    }

    public String getFilterAsString() {
        return filter().getStringForDisplay();
    }

    public Filter rawFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String getDescription() {
        return getUriForDisplay();
    }

    public String getUriForDisplay() {
        return getUrlArgument().forDisplay();
    }

    public String getFolder() {
        return folder;
    }

    public String getDisplayName() {
        return name == null ? getUriForDisplay() : CaseInsensitiveString.str(name);
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean value) {
        autoUpdate = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ScmMaterialConfig that = (ScmMaterialConfig) o;

        if (folder != null ? !folder.equals(that.folder) : that.folder != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        return result;
    }

    @Override
    protected final void validateConcreteMaterial(ValidationContext validationContext) {
        validateNotOutsideSandbox();
        validateDestFolderPath();
        validateConcreteScmMaterial(validationContext);
    }

    protected abstract void validateConcreteScmMaterial(ValidationContext validationContext);

    private void validateDestFolderPath() {
        if (StringUtils.isBlank(folder)) {
            return;
        }
        if (!new FilePathTypeValidator().isPathValid(folder)) {
            errors().add(FOLDER, FilePathTypeValidator.errorMessage("directory", getFolder()));
        }
    }

    public void setConfigAttributes(Object attributes) {
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        if (map.containsKey(FOLDER)) {
            String folder = (String) map.get(FOLDER);
            if (StringUtils.isBlank(folder)) {
                folder = null;
            }
            this.folder = folder;
        }
        this.setAutoUpdate("true".equals(map.get(AUTO_UPDATE)));
        if (map.containsKey(FILTER)) {
            String pattern = (String) map.get(FILTER);
            if (!StringUtil.isBlank(pattern)) {
                this.setFilter(Filter.fromDisplayString(pattern));
            } else {
                this.setFilter(null);
            }
        }
    }

    public boolean isAutoUpdateStateMismatch(MaterialConfigs materialAutoUpdateMap) {
        for (MaterialConfig otherMaterial : materialAutoUpdateMap) {
            if (otherMaterial.isAutoUpdate() != this.autoUpdate) {
                return true;
            }
        }
        return false;
    }

    public void setAutoUpdateMismatchError() {
        addError(AUTO_UPDATE, String.format("Material of type %s (%s) is specified more than once in the configuration with different values for the autoUpdate attribute."
                + " All copies of the a material should have the same value for this attribute.", getTypeForDisplay(), getDescription()));
    }
    public void setAutoUpdateMismatchErrorWithConfigRepo() {
        addError(AUTO_UPDATE, String.format("Material of type %s (%s) is specified as a configuration repository and pipeline material with disabled autoUpdate."
                + " All copies of the a material must have autoUpdate enabled or configuration repository must be removed", getTypeForDisplay(), getDescription()));
    }

    public void setDestinationFolderError(String message) {
        addError(FOLDER, message);
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

    private void validateNotOutsideSandbox() {
        String dest = this.getFolder();
        if (dest == null) {
            return;
        }
        if (!(FileUtil.isFolderInsideSandbox(dest))) {
            setDestinationFolderError(String.format("Dest folder '%s' is not valid. It must be a sub-directory of the working folder.", dest));
        }
    }

    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        return false;
    }

    // TODO: Consider renaming this to dest since we use that word in the UI & Config
    public void setFolder(String folder) {
        this.folder = folder;
    }


}
