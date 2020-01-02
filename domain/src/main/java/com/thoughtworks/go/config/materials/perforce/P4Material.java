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
package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.config.PasswordEncrypter;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.perforce.P4Client;
import com.thoughtworks.go.domain.materials.perforce.P4MaterialInstance;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class P4Material extends ScmMaterial implements PasswordEncrypter, PasswordAwareMaterial {
    private String serverAndPort;
    private Boolean useTickets = false;
    private P4MaterialView view;

    // Database stuff
    //TODO: use iBatis to set the type for us, and we can get rid of this field.
    public static final String TYPE = "P4Material";

    private P4Material(GoCipher goCipher) {
        super(TYPE, goCipher);
    }

    public P4Material(String serverAndPort, String view, GoCipher goCipher) {
        this(goCipher);
        bombIfNull(serverAndPort, "null serverAndPort");
        this.serverAndPort = serverAndPort;
        setView(view);
    }

    public P4Material(String serverAndPort, String view) {
        this(serverAndPort, view, new GoCipher());
    }

    public P4Material(String url, String view, String userName) {
        this(url, view);
        this.userName = userName;
    }

    public P4Material(String url, String view, String userName, String folder) {
        this(url, view, userName, folder, new GoCipher());
    }

    public P4Material(P4MaterialConfig config) {
        this(config.getUrl(), config.getView(), config.getUserName(), config.getFolder(), config.getGoCipher());
        this.name = config.getName();
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.invertFilter = config.getInvertFilter();
        setPassword(config.getPassword());
        this.useTickets = config.getUseTickets();
    }

    private P4Material(String serverAndPort, String view, String userName, String folder, GoCipher goCipher) {
        this(goCipher);
        bombIfNull(serverAndPort, "null serverAndPort");
        this.serverAndPort = serverAndPort;
        setView(view);
        this.userName = userName;
        this.folder = folder;
    }

    @Override
    public MaterialConfig config() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig();
        p4MaterialConfig.setServerAndPort(this.serverAndPort);
        p4MaterialConfig.setUserName(this.userName);
        p4MaterialConfig.setPassword(getPassword());
        p4MaterialConfig.setUseTickets(this.useTickets);
        p4MaterialConfig.setView(view == null ? null : view.getValue());
        p4MaterialConfig.setName(this.name);
        p4MaterialConfig.setAutoUpdate(this.autoUpdate);
        p4MaterialConfig.setFilter(this.filter);
        p4MaterialConfig.setInvertFilter(this.invertFilter);
        p4MaterialConfig.setFolder(this.folder);
        return p4MaterialConfig;
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        P4Client p4 = getP4(execCtx.isServer() ? baseDir : workingdir(baseDir));
        return p4.latestChange();
    }

    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        P4Client p4 = getP4(execCtx.isServer() ? baseDir : workingdir(baseDir));
        return p4.changesSince(revision);
    }

    @Override
    public MaterialInstance createMaterialInstance() {
        return new P4MaterialInstance(serverAndPort, userName, view.getValue(), useTickets, UUID.randomUUID().toString());
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, serverAndPort);
        parameters.put(ScmMaterialConfig.USERNAME, userName);
        parameters.put("view", view.getValue());
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        appendCriteria(parameters);
    }

    protected P4Client getP4(File baseDir) {
        InMemoryStreamConsumer outputConsumer = inMemoryConsumer();
        P4Client p4 = null;
        try {
            p4 = p4(baseDir, outputConsumer);
        } catch (Exception e) {
            bomb(e.getMessage() + " " + outputConsumer.getStdError(), e);
        }
        return p4;
    }

    @Override
    public void updateTo(ConsoleOutputStreamConsumer outputConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        File workingDir = execCtx.isServer() ? baseDir : workingdir(baseDir);
        boolean cleaned = cleanDirectoryIfRepoChanged(workingDir, outputConsumer);
        String revision = revisionContext.getLatestRevision().getRevision();
        try {
            outputConsumer.stdOutput(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, updatingTarget(), revision, serverAndPort));
            p4(workingDir, outputConsumer).sync(parseLong(revision), cleaned, outputConsumer);
            outputConsumer.stdOutput(format("[%s] Done.\n", GoConstants.PRODUCT_NAME));
        } catch (Exception e) {
            bomb(e);
        }
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        File baseDir = new TempFiles().createUniqueFolder("for-p4");
        try {
            getP4(baseDir).checkConnection();
            return ValidationBean.valid();
        } catch (Exception e) {
            return ValidationBean.notValid("Unable to connect to server " + serverAndPort + " : \n" + e.getMessage());
        } finally {
            FileUtils.deleteQuietly(baseDir);
        }
    }

    public String getServerAndPort() {
        return serverAndPort;
    }

    public String getView() {
        return view == null ? null : view.getValue();
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getUrl() {
        return serverAndPort;
    }

    @Override
    public String urlForCommandLine() {
        return serverAndPort;
    }

    @Override
    protected UrlArgument getUrlArgument() {
        return new UrlArgument(serverAndPort);
    }

    @Override
    public String getLongDescription() {
        return format("URL: %s, View: %s, Username: %s", serverAndPort, view.getValue(), userName);
    }

    P4Client p4(File baseDir, ConsoleOutputStreamConsumer consumer) throws Exception {
        return _p4(baseDir, consumer, true);
    }

    /**
     * not for use externally, created for testing convenience
     */
    P4Client _p4(File workDir, ConsoleOutputStreamConsumer consumer, boolean failOnError) throws Exception {
        String clientName = clientName(workDir);
        return P4Client.fromServerAndPort(getFingerprint(), serverAndPort, userName, passwordForCommandLine(), clientName, this.useTickets, workDir, p4view(clientName), consumer, failOnError);
    }

    @Override
    public void populateAgentSideEnvironmentContext(EnvironmentVariableContext environmentVariableContext, File baseDir) {
        super.populateAgentSideEnvironmentContext(environmentVariableContext, baseDir);
        setVariableWithName(environmentVariableContext, clientName(workingdir(baseDir)), "GO_P4_CLIENT");
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "perforce");
        Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put("url", serverAndPort);
        configurationMap.put("username", userName);
        if (addSecureFields) {
            configurationMap.put("password", getPassword());
        }
        configurationMap.put("view", getView());
        configurationMap.put("use-tickets", useTickets);
        materialMap.put("perforce-configuration", configurationMap);
        return materialMap;
    }

    @Override
    public Class getInstanceType() {
        return P4MaterialInstance.class;
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

        P4Material that = (P4Material) o;

        if (serverAndPort != null ? !serverAndPort.equals(that.serverAndPort) : that.serverAndPort != null) {
            return false;
        }
        if (useTickets != null ? !useTickets.equals(that.useTickets) : that.useTickets != null) {
            return false;
        }
        if (view != null ? !view.equals(that.view) : that.view != null) {
            return false;
        }

        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (serverAndPort != null ? serverAndPort.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (useTickets != null ? useTickets.hashCode() : 0);
        result = 31 * result + (view != null ? view.hashCode() : 0);
        return result;
    }

    @Override
    protected String getLocation() {
        return getServerAndPort();
    }

    @Override
    public String getTypeForDisplay() {
        return "Perforce";
    }

    public String p4view(String clientName) {
        return view.viewUsing(clientName);
    }

    public String clientName(File workingDir) {
        String hash = FileUtil.filesystemSafeFileHash(workingDir);
        return "cruise-" + SystemUtil.getLocalhostName()
                + "-" + workingDir.getName()
                + "-" + hash;
    }

    private boolean cleanDirectoryIfRepoChanged(File workingDirectory, ConsoleOutputStreamConsumer outputConsumer) {
        boolean cleaned = false;
        try {
            String p4RepoId = p4RepoId();
            File file = new File(workingDirectory, ".cruise_p4repo");
            if (!file.exists()) {
                FileUtils.writeStringToFile(file, p4RepoId, UTF_8);
                return true;
            }

            String existingRepoId = FileUtils.readFileToString(file, UTF_8);
            if (!p4RepoId.equals(existingRepoId)) {
                outputConsumer.stdOutput(format("[%s] Working directory has changed. Deleting and re-creating it.", GoConstants.PRODUCT_NAME));
                FileUtils.deleteDirectory(workingDirectory);
                workingDirectory.mkdirs();
                FileUtils.writeStringToFile(file, p4RepoId, UTF_8);
                cleaned = true;
            }
            return cleaned;
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    private String p4RepoId() {
        return hasUser() ? userName + "@" + serverAndPort : serverAndPort;
    }

    private boolean hasUser() {
        return userName != null && !userName.trim().isEmpty();
    }

    public boolean getUseTickets() {
        return this.useTickets;
    }

    public void setUseTickets(boolean useTickets) {
        this.useTickets = useTickets;
    }

    @Override
    public String toString() {
        return "P4Material{" +
                "serverAndPort='" + serverAndPort + '\'' +
                ", userName='" + userName + '\'' +
                ", view=" + view.getValue() +
                '}';
    }

    public void setUsername(String userName) {
        this.userName = userName;
    }

    private void setView(String viewStr) {
        this.view = new P4MaterialView(viewStr);
    }

}
