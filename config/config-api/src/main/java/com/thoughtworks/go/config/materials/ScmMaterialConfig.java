/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.FilenameUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
        this(typeName, new GoCipher());
    }

    public ScmMaterialConfig(String typeName, GoCipher goCipher) {
        super(typeName);
        this.goCipher = goCipher;
    }

    public ScmMaterialConfig(CaseInsensitiveString name, Filter filter, boolean invertFilter, String folder, boolean autoUpdate, String typeName, ConfigErrors errors) {
        super(typeName, name, errors);
        this.goCipher = new GoCipher();
        this.filter = filter;
        this.invertFilter = invertFilter;
        this.folder = folder;
        this.autoUpdate = autoUpdate;
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
        if (StringUtils.isBlank(passwordToSet)) {
            encryptedPassword = null;
        }

        setPasswordIfNotBlank(passwordToSet);
    }

    @PostConstruct
    public final void ensureEncrypted() {
        this.userName = StringUtils.stripToNull(this.userName);
        setPasswordIfNotBlank(password);

        if (encryptedPassword != null) {
            setEncryptedPassword(goCipher.maybeReEncryptForPostConstructWithoutExceptions(encryptedPassword));
        }
    }

    private void setPasswordIfNotBlank(String password) {
        this.password = StringUtils.stripToNull(password);
        this.encryptedPassword = StringUtils.stripToNull(encryptedPassword);

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
            return StringUtils.isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
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

    public boolean isInvertFilter() {
        return invertFilter;
    }

    public boolean getInvertFilter() {
        return invertFilter;
    }

    public void setInvertFilter(boolean value) {
        invertFilter = value;
    }

    public String getDescription() {
        return getUriForDisplay();
    }

    public abstract String getUriForDisplay();

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
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }
        return super.equals(that);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
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
        this.setInvertFilter("true".equals(map.get(INVERT_FILTER)));
        if (map.containsKey(FILTER)) {
            String pattern = (String) map.get(FILTER);
            if (!StringUtils.isBlank(pattern)) {
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

    public void setAutoUpdateMismatchError() {
        addError(AUTO_UPDATE, String.format("Material of type %s (%s) is specified more than once in the configuration with different values for the autoUpdate attribute."
                + " All copies of this material must have the same value for this attribute.", getTypeForDisplay(), getDescription()));
    }

    public void setAutoUpdateMismatchErrorWithConfigRepo() {
        addError(AUTO_UPDATE, String.format("Material of type %s (%s) is specified as a configuration repository and pipeline material with disabled autoUpdate."
                + " All copies of this material must have autoUpdate enabled or configuration repository must be removed", getTypeForDisplay(), getDescription()));
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

    protected void validateMaterialUrl(UrlArgument url, ValidationContext validationContext) {
        if (url == null || isBlank(url.forDisplay())) {
            errors().add(URL, "URL cannot be blank");
            return;
        }
    }

    protected void validatePassword(ValidationContext validationContext) {
        if (isNotEmpty(getEncryptedPassword())) {
            try {
                validateSecretParamsConfig("encryptedPassword", SecretParams.parse(getPassword()), validationContext);
            } catch (Exception e) {
                addError("encryptedPassword", format("Encrypted password value for %s with url '%s' is invalid. This usually happens when the cipher text is modified to have an invalid value.", this.getType(), this.getUriForDisplay()));
            }
        }
    }

    protected void validateSecretParamsConfig(String key, SecretParams secretParams, ValidationContext validationContext) {
        if (!secretParams.hasSecretParams()) {
            return;
        }

        final List<String> missingSecretConfigs = secretParams.stream()
                .filter(secretParam -> validationContext.getCruiseConfig().getSecretConfigs().find(secretParam.getSecretConfigId()) == null)
                .map(SecretParam::getSecretConfigId)
                .collect(Collectors.toList());

        if (!missingSecretConfigs.isEmpty()) {
            addError(key, String.format("Secret config with ids `%s` does not exist.", String.join(", ", missingSecretConfigs)));
        }
    }
}
