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

package com.thoughtworks.go.config.materials.tfs;

import com.thoughtworks.go.config.PasswordEncrypter;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.tfs.TfsCommand;
import com.thoughtworks.go.domain.materials.tfs.TfsCommandFactory;
import com.thoughtworks.go.domain.materials.tfs.TfsMaterialInstance;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

public class TfsMaterial extends ScmMaterial implements PasswordAwareMaterial, PasswordEncrypter {
    private static final Logger LOGGER = Logger.getLogger(TfsMaterial.class);

    public static final String TYPE = "TfsMaterial";

    private UrlArgument url;
    private String userName;
    private String domain = "";
    private String password;
    private String encryptedPassword;
    private String projectPath;
    private final GoCipher goCipher;

    public TfsMaterial(GoCipher goCipher) {
        super(TYPE);
        this.goCipher = goCipher;
    }

    public TfsMaterial(GoCipher goCipher, UrlArgument url, String userName, String domain, String password, String projectPath) {
        this(goCipher);
        this.url = url;
        this.userName = userName;
        this.domain = domain;
        setPassword(password);
        this.projectPath = projectPath;
    }

    public TfsMaterial(TfsMaterialConfig config) {
        this(config.getGoCipher(), config.getUrlArgument(), config.getUserName(), config.getDomain(), config.getPassword(), config.getProjectPath());
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.invertFilter = config.getInvertFilter();
        this.folder = config.getFolder();
        this.name = config.getName();
    }

    @Override
    public MaterialConfig config() {
        return new TfsMaterialConfig(url, userName, domain, getPassword(), projectPath, goCipher, autoUpdate, filter, invertFilter, folder, name);
    }

    public String getDomain() {
        return domain;
    }

    @Override public String getUserName() {
        return userName;
    }

    public void setPassword(String password) {
        resetPassword(password);
    }

    @Override public String getPassword() {
        try {
            return StringUtil.isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException("Could not decrypt the password to get the real password", e);
        }
    }

    @Override public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getProjectPath() {
        return projectPath;
    }

    @Override public boolean isCheckExternals() {
        return false;
    }

    @Override public String getUrl() {
        return url == null ? null : url.forCommandline();
    }

    @Override protected UrlArgument getUrlArgument() {
        return url;
    }

    public String getLongDescription() {
       return String.format("URL: %s, Username: %s, Domain: %s, ProjectPath: %s", url.forDisplay(), userName, domain, projectPath);
    }

    @Override protected String getLocation() {
        return url == null ? null : url.forDisplay();
    }

    @Override protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.forCommandline());
        parameters.put(ScmMaterialConfig.USERNAME, userName);
        parameters.put(TfsMaterialConfig.DOMAIN, domain);
        parameters.put(TfsMaterialConfig.PROJECT_PATH, projectPath);
    }

    @Override protected void appendAttributes(Map<String, Object> parameters) {
        appendCriteria(parameters);
    }

    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        Revision revision = revisionContext.getLatestRevision();
        File workingDir = execCtx.isServer() ? baseDir : workingdir(baseDir);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[TFS] Updating to revision: " + revision + " in workingdirectory " + workingDir);
        }
        outputStreamConsumer.stdOutput(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, updatingTarget(), revision.getRevision(), url));
        tfs(execCtx).checkout(workingDir, revision);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[TFS] done with update");
        }
        outputStreamConsumer.stdOutput(format("[%s] Done.\n", GoConstants.PRODUCT_NAME));
    }

    TfsCommand tfs(final SubprocessExecutionContext execCtx) {
        return new TfsCommandFactory().create(execCtx, url, domain, userName, getPassword(), getFingerprint(), projectPath);
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        try {
            tfs(execCtx).checkConnection();
            return ValidationBean.valid();
        } catch (Exception e) {
            LOGGER.error("[TFS] Error during check connection", e);
            return ValidationBean.notValid(e.getMessage());
        }
    }

    public List<Modification> latestModification(File workDir, final SubprocessExecutionContext execCtx) {
        return tfs(execCtx).latestModification(workDir);
    }

    public List<Modification> modificationsSince(File workDir, Revision revision, final SubprocessExecutionContext execCtx) {
        return tfs(execCtx).modificationsSince(workDir, revision);
    }

    public MaterialInstance createMaterialInstance() {
        return new TfsMaterialInstance(url.forCommandline(), userName, domain, projectPath, UUID.randomUUID().toString());
    }

    public String getTypeForDisplay() {
        return "Tfs";
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "tfs");
        Map<String, Object> configurationMap = new HashMap<>();
        if (addSecureFields) {
            configurationMap.put("url", url.forCommandline());
        } else {
            configurationMap.put("url", url.forDisplay());
        }
        configurationMap.put("domain", domain);
        configurationMap.put("username", userName);
        if (addSecureFields) {
            configurationMap.put("password", getPassword());
        }
        configurationMap.put("project-path", projectPath);
        materialMap.put("tfs-configuration", configurationMap);
        return materialMap;
    }

    public Class getInstanceType() {
        return TfsMaterialInstance.class;
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

        TfsMaterial material = (TfsMaterial) o;

        if (projectPath != null ? !projectPath.equals(material.projectPath) : material.projectPath != null) {
            return false;
        }
        if (url != null ? !url.equals(material.url) : material.url != null) {
            return false;
        }
        if (userName != null ? !userName.equals(material.userName) : material.userName != null) {
            return false;
        }
        if (domain != null ? !domain.equals(material.domain) : material.domain != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (projectPath != null ? projectPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE, true);
    }

    @PostConstruct
    public void ensureEncrypted() {
        setPasswordIfNotBlank(password);
    }

    private void resetPassword(String passwordToSet) {
        if (StringUtil.isBlank(passwordToSet)) {
            encryptedPassword = null;
        }

        setPasswordIfNotBlank(passwordToSet);
    }

    private void setPasswordIfNotBlank(String password) {
        if (StringUtil.isBlank(password)) {
            return;
        }
        try {
            this.encryptedPassword = this.goCipher.encrypt(password);
        } catch (Exception e) {
            bomb("Password encryption failed. Please verify your cipher key.", e);
        }
        this.password = null;
    }

    /* Needed although there is a getUserName above */
    public String getUsername() {
        return userName;
    }
}
