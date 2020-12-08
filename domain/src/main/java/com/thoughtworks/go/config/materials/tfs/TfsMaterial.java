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
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TfsMaterial extends ScmMaterial implements PasswordAwareMaterial, PasswordEncrypter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TfsMaterial.class);

    public static final String TYPE = "TfsMaterial";
    public static final String GO_MATERIAL_DOMAIN = "GO_MATERIAL_DOMAIN";

    private UrlArgument url;
    private String domain = "";
    private String projectPath;

    public TfsMaterial() {
        super(TYPE);
    }

    public TfsMaterial(UrlArgument url, String userName, String domain, String password, String projectPath) {
        this();
        this.url = url;
        this.userName = userName;
        this.domain = domain;
        setPassword(password);
        this.projectPath = projectPath;
    }

    public TfsMaterial(TfsMaterialConfig config) {
        this(new UrlArgument(config.getUrl()), config.getUserName(), config.getDomain(), config.getPassword(), config.getProjectPath());
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.invertFilter = config.getInvertFilter();
        this.folder = config.getFolder();
        this.name = config.getName();
    }

    @Override
    public MaterialConfig config() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig();
        tfsMaterialConfig.setUrl(this.url.originalArgument());
        tfsMaterialConfig.setUserName(this.userName);
        tfsMaterialConfig.setDomain(this.domain);
        tfsMaterialConfig.setPassword(getPassword());
        tfsMaterialConfig.setProjectPath(this.projectPath);
        tfsMaterialConfig.setAutoUpdate(this.autoUpdate);
        tfsMaterialConfig.setFilter(this.filter);
        tfsMaterialConfig.setInvertFilter(this.invertFilter);
        tfsMaterialConfig.setFolder(this.folder);
        tfsMaterialConfig.setName(this.name);
        return tfsMaterialConfig;
    }

    public String getDomain() {
        return domain;
    }

    public String getProjectPath() {
        return projectPath;
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getUrl() {
        return url == null ? null : url.originalArgument();
    }

    @Override
    public String urlForCommandLine() {
        return url.forCommandLine();
    }

    @Override
    protected UrlArgument getUrlArgument() {
        return url;
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s, Username: %s, Domain: %s, ProjectPath: %s", url.forDisplay(), userName, domain, projectPath);
    }

    @Override
    protected String getLocation() {
        return url == null ? null : url.forDisplay();
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.originalArgument());
        parameters.put(ScmMaterialConfig.USERNAME, userName);
        parameters.put(TfsMaterialConfig.DOMAIN, domain);
        parameters.put(TfsMaterialConfig.PROJECT_PATH, projectPath);
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        appendCriteria(parameters);
    }

    @Override
    public void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        Revision revision = revisionContext.getLatestRevision();
        File workingDir = execCtx.isServer() ? baseDir : workingdir(baseDir);
        LOGGER.debug("[TFS] Updating to revision: {} in workingdirectory {}", revision, workingDir);
        outputStreamConsumer.stdOutput(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, updatingTarget(), revision.getRevision(), url));
        tfs(execCtx).checkout(workingDir, revision);
        LOGGER.debug("[TFS] done with update");
        outputStreamConsumer.stdOutput(format("[%s] Done.\n", GoConstants.PRODUCT_NAME));
    }

    TfsCommand tfs(final SubprocessExecutionContext execCtx) {
        return new TfsCommandFactory().create(execCtx, url, domain, userName, passwordForCommandLine(), getFingerprint(), projectPath);
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

    @Override
    public MaterialInstance createMaterialInstance() {
        return new TfsMaterialInstance(url.originalArgument(), userName, domain, projectPath, UUID.randomUUID().toString());
    }

    @Override
    public String getTypeForDisplay() {
        return "Tfs";
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "tfs");
        Map<String, Object> configurationMap = new HashMap<>();
        if (addSecureFields) {
            configurationMap.put("url", url.originalArgument());
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

    @Override
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
        if (domain != null ? !domain.equals(material.domain) : material.domain != null) {
            return false;
        }
        if (userName != null ? !userName.equals(material.userName) : material.userName != null) {
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

    @Override
    protected void setGoMaterialVariables(EnvironmentVariableContext environmentVariableContext) {
        super.setGoMaterialVariables(environmentVariableContext);
        if (isNotBlank(domain)) {
            setVariableWithName(environmentVariableContext, domain, GO_MATERIAL_DOMAIN);
        }
    }
}
