/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.SecretParamAware;
import com.thoughtworks.go.config.SecretParams;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Map;
import java.util.Optional;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.escapeEnvironmentVariable;
import static org.apache.commons.lang3.StringUtils.isBlank;


/**
 * @understands a source control repository and its configuration
 */
public abstract class ScmMaterial extends AbstractMaterial implements SecretParamAware {

    public static final String GO_REVISION = "GO_REVISION";
    public static final String GO_TO_REVISION = "GO_TO_REVISION";
    public static final String GO_FROM_REVISION = "GO_FROM_REVISION";
    public static final String GO_MATERIAL_URL = "GO_MATERIAL_URL";
    protected final GoCipher goCipher;

    protected Filter filter;
    protected String folder;
    protected boolean autoUpdate = true;
    protected boolean invertFilter = false;
    protected String userName;
    protected String password;
    protected String encryptedPassword;
    protected SecretParams secretParamsForPassword;

    public ScmMaterial(String typeName, GoCipher goCipher) {
        super(typeName);
        this.goCipher = goCipher;
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

    public String updatingTarget() {
        return StringUtils.isEmpty(getFolder()) ? "files" : getFolder();
    }

    @Override
    public void toJson(Map json, Revision revision) {
        json.put("folder", getFolder() == null ? "" : getFolder());
        json.put("scmType", getTypeForDisplay());
        json.put("location", getLocation());
        if (!CaseInsensitiveString.isBlank(getName())) {
            json.put("materialName", CaseInsensitiveString.str(getName()));
        }
        json.put("action", "Modified");
    }

    //most of the material such as hg, git, p4 all print the file from the root without '/'
    //but subversion print it with '/', we standarize it here. look at the implementation of subversion as well.

    @Override
    public boolean matches(String name, String regex) {
        if (regex.startsWith("/")) {
            regex = regex.substring(1);
        }
        return name.matches(regex);
    }

    public void checkout(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        InMemoryStreamConsumer output = ProcessOutputStreamConsumer.inMemoryConsumer();
        this.updateTo(output, baseDir, new RevisionContext(revision), execCtx);
    }

    public String getUserName() {
        return this.userName;
    }

    /* Needed although there is a getUserName above */
    public String getUsername() {
        return userName;
    }

    public final void setPassword(String password) {
        resetPassword(password);
    }

    private void resetPassword(String passwordToSet) {
        if (StringUtils.isBlank(passwordToSet)) {
            encryptedPassword = null;
        }
        setPasswordIfNotBlank(passwordToSet);
    }

    private void setPasswordIfNotBlank(String password) {
        this.password = StringUtils.stripToNull(password);
        this.secretParamsForPassword = SecretParams.parse(password);
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

    @PostConstruct
    public void ensureEncrypted() {
        this.userName = StringUtils.stripToNull(this.userName);
        setPasswordIfNotBlank(password);
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public final void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public final String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getPassword() {
        return currentPassword();
    }

    public String passwordForCommandLine() {
        return secretParamsForPassword == null || secretParamsForPassword.isEmpty() ? getPassword() : secretParamsForPassword.substitute(getPassword());
    }

    @Override
    public boolean hasSecretParams() {
        return this.secretParamsForPassword != null && !this.secretParamsForPassword.isEmpty();
    }

    @Override
    public SecretParams getSecretParams() {
        return secretParamsForPassword;
    }

    public final String currentPassword() {
        try {
            return isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
        } catch (CryptoException e) {
            throw new RuntimeException("Could not decrypt the password to get the real password", e);
        }
    }

    public abstract boolean isCheckExternals();

    public abstract String getUrl();

    public abstract String urlForCommandLine();

    protected abstract UrlArgument getUrlArgument();

    protected abstract String getLocation();

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public void emailContent(StringBuilder content, Modification modification) {
        content.append(getTypeForDisplay() + ": " + getLocation()).append('\n').append(
                String.format("revision: %s, modified by %s on %s", modification.getRevision(),
                        modification.getUserName(), modification.getModifiedTime()))
                .append('\n')
                .append(Optional.ofNullable(modification.getComment()).orElse(""));

    }

    @Override
    public String getDescription() {
        return getUriForDisplay();
    }

    @Override
    public String getUriForDisplay() {
        return this.getUrlArgument().forDisplay();
    }

    @Override
    public void populateEnvironmentContext(EnvironmentVariableContext environmentVariableContext, MaterialRevision materialRevision, File workingDir) {
        String toRevision = materialRevision.getRevision().getRevision();
        String fromRevision = materialRevision.getOldestRevision().getRevision();

        setGoRevisionVariables(environmentVariableContext, fromRevision, toRevision);
        setGoMaterialVariables(environmentVariableContext);
    }

    protected void setGoMaterialVariables(EnvironmentVariableContext environmentVariableContext) {
        setVariableWithName(environmentVariableContext, this.getUrlArgument().withoutCredentials(), GO_MATERIAL_URL);
    }

    private void setGoRevisionVariables(EnvironmentVariableContext environmentVariableContext, String fromRevision, String toRevision) {
        setVariableWithName(environmentVariableContext, toRevision, GO_REVISION);
        setVariableWithName(environmentVariableContext, toRevision, GO_TO_REVISION);
        setVariableWithName(environmentVariableContext, fromRevision, GO_FROM_REVISION);
    }

    protected void setVariableWithName(EnvironmentVariableContext environmentVariableContext, String value, String propertyName) {
        String materialNameForEnvironmentVariable = getMaterialNameForEnvironmentVariable();
        if (StringUtils.isNotBlank(materialNameForEnvironmentVariable)) {
            environmentVariableContext.setProperty(propertyName + "_" + materialNameForEnvironmentVariable, value, false);
        } else {
            environmentVariableContext.setProperty(propertyName, value, false);
        }
    }

    @Override
    public String getMaterialNameForEnvironmentVariable() {
        if (!CaseInsensitiveString.isBlank(this.name)) {
            return escapeEnvironmentVariable(this.name.toUpper());
        }

        return escapeEnvironmentVariable(folder);
    }

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

    public void setAutoUpdate(boolean value) {
        autoUpdate = value;
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

    @Override
    public final MatchedRevision createMatchedRevision(Modification modification, String searchString) {
        return new MatchedRevision(searchString, getShortRevision(modification.getRevision()), modification.getRevision(), modification.getUserName(), modification.getModifiedTime(), modification.getComment());
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

        ScmMaterial that = (ScmMaterial) o;

        return folder != null ? folder.equals(that.folder) : that.folder == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        return result;
    }

    public static String changesetUrl(Modification modification, String baseUrl, final long id) {
        return baseUrl + "/api/materials/" + id + "/changeset/" + modification.getRevision() + ".xml";
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        return false;
    }

    // TODO: Consider renaming this to dest since we use that word in the UI & Config
    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Override
    public Revision oldestRevision(Modifications modifications) {
        return Modification.oldestRevision(modifications);
    }

    @Override
    public boolean supportsDestinationFolder() {
        return true;
    }
}
