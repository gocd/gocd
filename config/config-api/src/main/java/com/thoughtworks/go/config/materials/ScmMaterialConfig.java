/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.FilenameUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import com.thoughtworks.go.util.command.UrlUserInfo;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @understands a source control repository and its configuration
 */
public abstract class ScmMaterialConfig extends AbstractMaterialConfig implements ParamsAttributeAware {

    public static final String URL = "url";
    public static final String USERNAME = "username";
    protected GoCipher goCipher;

    @ConfigSubtag
    private Filter filter;

    @ConfigAttribute(value = "invertFilter", optional = true)
    private boolean invertFilter = false;

    @ConfigAttribute(value = "dest", allowNull = true)
    protected String folder;

    @ConfigAttribute(value = "autoUpdate", optional = true)
    private boolean autoUpdate = true;

    @ConfigAttribute(value = "username", allowNull = true)
    protected String userName;

    @SkipParameterResolution
    @ConfigAttribute(value = "password", allowNull = true)
    protected String password;

    @ConfigAttribute(value = "encryptedPassword", allowNull = true)
    protected String encryptedPassword;

    public static final String PASSWORD = "password";
    public static final String ENCRYPTED_PASSWORD = "encryptedPassword";
    public static final String PASSWORD_CHANGED = "passwordChanged";
    public static final String AUTO_UPDATE = "autoUpdate";

    public static final String FOLDER = "folder";
    public static final String FILTER = "filterAsString";
    public static final String INVERT_FILTER = "invertFilter";

    public ScmMaterialConfig(String typeName) {
        super(typeName);
        this.goCipher = new GoCipher();
    }

    @Override
    protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
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
    @Override
    public boolean matches(String name, String regex) {
        if (regex.startsWith("/")) {
            regex = regex.substring(1);
        }
        return name.matches(regex);
    }

    public final GoCipher getGoCipher() {
        return goCipher;
    }

    public final void setUserName(String userName) {
        this.userName = userName;
    }

    public final String getUserName() {
        return userName;
    }

    /* Needed by rails! */

    /**
     * @deprecated "Use `getUserName` instead"
     */
    @Deprecated
    public String getUsername() {
        return getUserName();
    }

    public final String getPassword() {
        return currentPassword();
    }

    public final void setPassword(String password) {
        resetPassword(password);
    }

    protected void resetPassword(String passwordToSet) {
        if (isBlank(passwordToSet)) {
            encryptedPassword = null;
        }

        setPasswordIfNotBlank(passwordToSet);
    }

    @PostConstruct
    public final void ensureEncrypted() {
        this.userName = stripToNull(this.userName);
        setPasswordIfNotBlank(password);
    }

    private void setPasswordIfNotBlank(String password) {
        this.password = stripToNull(password);
        this.encryptedPassword = stripToNull(encryptedPassword);

        if (this.password == null) {
            return;
        }
        try {
            this.encryptedPassword = this.goCipher.encrypt(password);
        } catch (Exception e) {
            bomb("Password encryption failed. Please verify your cipher key.", e);
        }
        this.password = null;
    }

    public final String currentPassword() {
        try {
            return isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt the password to get the real password", e);
        }
    }

    public final void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public final String getEncryptedPassword() {
        return encryptedPassword;
    }

    public abstract boolean isCheckExternals();

    public abstract String getUrl();

    public abstract void setUrl(String url);

    protected abstract String getLocation();

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

    public Filter rawFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public boolean isInvertFilter() {
        return invertFilter;
    }

    public boolean getInvertFilter() {
        return invertFilter;
    }

    public void setInvertFilter(boolean value) {
        invertFilter = value;
    }

    @Override
    public String getDescription() {
        return getUriForDisplay();
    }

    @Override
    public abstract String getUriForDisplay();

    @Override
    public String getFolder() {
        return folder;
    }

    @Override
    public String getDisplayName() {
        return name == null ? getUriForDisplay() : CaseInsensitiveString.str(name);
    }

    @Override
    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    @Override
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
        return super.equals(that);
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

    public abstract void validateConcreteScmMaterial(ValidationContext validationContext);

    private void validateDestFolderPath() {
        if (isBlank(folder)) {
            return;
        }
        if (!new FilePathTypeValidator().isPathValid(folder)) {
            errors().add(FOLDER, FilePathTypeValidator.errorMessage("directory", getFolder()));
        }
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        if (map.containsKey(FOLDER)) {
            String folder = (String) map.get(FOLDER);
            if (isBlank(folder)) {
                folder = null;
            }
            this.folder = folder;
        }
        this.setAutoUpdate("true".equals(map.get(AUTO_UPDATE)));
        this.setInvertFilter("true".equals(map.get(INVERT_FILTER)));
        if (map.containsKey(FILTER)) {
            String pattern = (String) map.get(FILTER);
            if (!isBlank(pattern)) {
                this.setFilter(Filter.fromDisplayString(pattern));
            } else {
                this.setFilter(null);
            }
        }
    }

    public boolean isAutoUpdateStateMismatch(MaterialConfigs materialAutoUpdateMap) {
        if (materialAutoUpdateMap.size() > 1) {
            for (MaterialConfig otherMaterial : materialAutoUpdateMap) {
                if (otherMaterial.isAutoUpdate() != this.autoUpdate) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setAutoUpdateMismatchErrorWithPipelines(Map<CaseInsensitiveString, Boolean> pipelinesWithThisMaterial) {
        String message = format("The material of type %s (%s) is used elsewhere with a different value for autoUpdate (\"Poll for changes\"). Those values should be the same. Pipelines: %s", getTypeForDisplay(), getDescription(), join(pipelinesWithThisMaterial));
        addError(AUTO_UPDATE, message);
    }

    private String getAutoUpdateStatus(boolean autoUpdate) {
        return autoUpdate ? "auto update enabled" : "auto update disabled";
    }

    private String join(Map<CaseInsensitiveString, Boolean> pipelinesWithThisMaterial) {
        if (pipelinesWithThisMaterial == null || pipelinesWithThisMaterial.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        pipelinesWithThisMaterial.forEach((key, value) -> {
            builder.append(format("%s (%s),\n ", key, getAutoUpdateStatus(value)));
        });

        return builder.delete(builder.lastIndexOf(","), builder.length()).toString();
    }

    public void setDestinationFolderError(String message) {
        addError(FOLDER, message);
    }

    public void validateNotSubdirectoryOf(String otherSCMMaterialFolder) {
        String myDirPath = this.getFolder();
        if (myDirPath == null || otherSCMMaterialFolder == null) {
            return;
        }
        if (FilenameUtil.isNormalizedDirectoryPathInsideNormalizedParentDirectory(myDirPath, otherSCMMaterialFolder)) {
            addError(FOLDER, "Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested.");
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
        if (!(FilenameUtil.isNormalizedPathOutsideWorkingDir(dest))) {
            setDestinationFolderError(format("Dest folder '%s' is not valid. It must be a sub-directory of the working folder.", dest));
        }
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        return false;
    }

    // TODO: Consider renaming this to dest since we use that word in the UI & Config
    public void setFolder(String folder) {
        this.folder = folder;
    }

    protected void validateMaterialUrl(UrlArgument url) {
        if (url == null || isBlank(url.forDisplay())) {
            errors().add(URL, "URL cannot be blank");
            return;
        }
    }

    protected void validateCredentials() {
        try {
            if (isBlank(getUrl()) || isAllBlank(userName, getPassword())) {
                return;
            }

            if (UrlUserInfo.hasUserInfo(getUrl())) {
                errors().add(URL, "Ambiguous credentials, must be provided either in URL or as attributes.");
            }
        } catch (Exception e) {
            //ignore
        }
    }


    protected void validateEncryptedPassword() {
        if (isNotEmpty(getEncryptedPassword())) {
            try {
                getPassword();
            } catch (Exception e) {
                addError("encryptedPassword", format("Encrypted password value for %s with url '%s' is invalid. This usually happens when the cipher text is modified to have an invalid value.", this.getType(), this.getUriForDisplay()));
            }
        }
    }
}
