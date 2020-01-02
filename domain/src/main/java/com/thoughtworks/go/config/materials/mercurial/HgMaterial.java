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
package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.mercurial.HgCommand;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialInstance;
import com.thoughtworks.go.domain.materials.mercurial.HgVersion;
import com.thoughtworks.go.domain.materials.svn.MaterialUrl;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfFailedToRunCommandLine;
import static com.thoughtworks.go.util.FileUtil.createParentFolderIfNotExist;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @understands configuration for mercurial version control
 */
public class HgMaterial extends ScmMaterial implements PasswordAwareMaterial {
    private static final Pattern HG_VERSION_PATTERN = Pattern.compile(".*\\(.*\\s+(\\d(\\.\\d)+.*)\\)");
    private static final Logger LOGGER = LoggerFactory.getLogger(HgMaterial.class);
    private HgUrlArgument url;

    //TODO: use iBatis to set the type for us, and we can get rid of this field.
    public static final String TYPE = "HgMaterial";
    private static final String ERROR_OLD_VERSION = "Please install Mercurial Version 1.0 or above."
            + " The current installed hg is ";
    private static final String ERR_NO_HG_INSTALLED =
            "Failed to find 'hg' on your PATH. Please ensure 'hg' is executable by the Go Server and on the Go Agents where this material will be used.";

    private final String HG_DEFAULT_BRANCH = "default";
    private String branch;

    private HgMaterial() {
        super(TYPE, new GoCipher());
    }

    public HgMaterial(String url, String folder) {
        this();
        this.url = new HgUrlArgument(url);
        this.folder = folder;
    }

    public HgMaterial(HgMaterialConfig config) {
        this(config.getUrl(), config.getFolder());
        this.userName = config.getUserName();
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.invertFilter = config.getInvertFilter();
        this.name = config.getName();
        this.userName = config.getUserName();
        this.branch = config.getBranch();
        setPassword(config.getPassword());
    }

    @Override
    public MaterialConfig config() {
        HgMaterialConfig hgConfig = new HgMaterialConfig();
        hgConfig.setUrl(this.url.originalArgument());
        hgConfig.setUserName(this.userName);
        hgConfig.setPassword(getPassword());
        hgConfig.setBranchAttribute(this.branch);
        hgConfig.setAutoUpdate(this.autoUpdate);
        hgConfig.setFilter(this.filter);
        hgConfig.setInvertFilter(this.invertFilter);
        hgConfig.setFolder(this.folder);
        hgConfig.setName(this.name);
        return hgConfig;
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        HgCommand hgCommand = getHg(baseDir);
        return hgCommand.latestOneModificationAsModifications();
    }


    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        return getHg(baseDir).modificationsSince(revision);
    }

    @Override
    public MaterialInstance createMaterialInstance() {
        return new HgMaterialInstance(url.originalArgument(), userName, branch, UUID.randomUUID().toString());
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.originalArgument());
        if (isNotBlank(branch)) {
            parameters.put("branch", branch);
        }
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("url", url);
    }

    private HgCommand getHg(File baseDir) {
        InMemoryStreamConsumer output =
                ProcessOutputStreamConsumer.inMemoryConsumer();
        HgCommand hgCommand = null;
        try {
            hgCommand = hg(baseDir, output);
        } catch (Exception e) {
            bomb(e.getMessage() + " " + output.getStdError(), e);
        }

        return hgCommand;
    }

    @Override
    public void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        Revision revision = revisionContext.getLatestRevision();
        try {
            outputStreamConsumer.stdOutput(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, updatingTarget(), revision.getRevision(), url.forDisplay()));
            File workingDir = execCtx.isServer() ? baseDir : workingdir(baseDir);
            hg(workingDir, outputStreamConsumer).updateTo(revision, outputStreamConsumer);
            outputStreamConsumer.stdOutput(format("[%s] Done.\n", GoConstants.PRODUCT_NAME));
        } catch (Exception e) {
            bomb(e);
        }
    }

    public void add(File baseDir, ProcessOutputStreamConsumer outputStreamConsumer, File file) throws Exception {
        hg(baseDir, outputStreamConsumer).add(outputStreamConsumer, file);
    }

    public void commit(File baseDir, ProcessOutputStreamConsumer consumer, String comment, String username)
            throws Exception {
        hg(baseDir, consumer).commit(consumer, comment, username);
    }

    public void push(File baseDir, ProcessOutputStreamConsumer consumer) throws Exception {
        hg(baseDir, consumer).push(consumer);
    }

    boolean isVersionOneDotZeroOrHigher(String hgout) {
        String hgVersion = parseHgVersion(hgout);
        Float aFloat = NumberUtils.createFloat(hgVersion.subSequence(0, 3).toString());
        return aFloat >= 1;
    }

    private String parseHgVersion(String hgOut) {
        String[] lines = hgOut.split("\n");
        String firstLine = lines[0];
        Matcher m = HG_VERSION_PATTERN.matcher(firstLine);
        if (m.matches()) {
            return m.group(1);
        } else {
            throw bomb("can not parse hgout : " + hgOut);
        }
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        HgCommand hgCommand = new HgCommand(null, null, null, null, secrets());
        try {
            hgCommand.checkConnection(new HgUrlArgument(urlForCommandLine()));
            return ValidationBean.valid();
        } catch (Exception e) {
            try {
                return handleException(e, hgCommand.version());
            } catch (Exception ex) {
                return ValidationBean.notValid(ERR_NO_HG_INSTALLED);
            }
        }
    }

    ValidationBean handleException(Exception e, HgVersion version) {
        ValidationBean defaultResponse = ValidationBean.notValid(
                "Repository " + url.forDisplay() + " not found!" + " : \n" + e.getMessage());
        try {
            if (version.isOlderThanOneDotZero()) {
                return ValidationBean.notValid(ERROR_OLD_VERSION + version.toString());
            } else {
                return defaultResponse;
            }
        } catch (Exception e1) {
            LOGGER.debug("Problem validating HG", e);
            return defaultResponse;
        }
    }


    private HgCommand hg(File workingFolder, ConsoleOutputStreamConsumer outputStreamConsumer) throws Exception {
        UrlArgument urlArgument = new HgUrlArgument(urlForCommandLine());
        HgCommand hgCommand = new HgCommand(getFingerprint(), workingFolder, getBranch(), urlArgument.forCommandLine(), secrets());
        if (!isHgRepository(workingFolder) || isRepositoryChanged(hgCommand)) {
            LOGGER.debug("Invalid hg working copy or repository changed. Delete folder: {}", workingFolder);
            FileUtils.deleteQuietly(workingFolder);
        }
        if (!workingFolder.exists()) {
            createParentFolderIfNotExist(workingFolder);
            int returnValue = hgCommand.clone(outputStreamConsumer, urlArgument);
            bombIfFailedToRunCommandLine(returnValue, "Failed to run hg clone command");
        }
        return hgCommand;
    }

    protected List<SecretString> secrets() {
        SecretString secretSubstitution = line -> line.replace(urlForCommandLine(), getUriForDisplay());
        return Collections.singletonList(secretSubstitution);
    }

    private boolean isHgRepository(File workingFolder) {
        return new File(workingFolder, ".hg").isDirectory();
    }

    private boolean isRepositoryChanged(HgCommand hgCommand) {
        ConsoleResult result = hgCommand.workingRepositoryUrl();
        return !MaterialUrl.sameUrl(url.defaultRemoteUrl(), new HgUrlArgument(result.outputAsString()).defaultRemoteUrl());
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getUrl() {
        return url.originalArgument();
    }

    @Override
    public String urlForCommandLine() {
        try {
            if (credentialsAreNotProvided()) {
                return this.url.originalArgument();
            }

            return new URIBuilder(this.url.originalArgument())
                    .setUserInfo(new UrlUserInfo(this.userName, this.passwordForCommandLine()).asString())
                    .build().toString();

        } catch (URISyntaxException e) {
            return this.url.originalArgument();
        }
    }

    private boolean credentialsAreNotProvided() {
        return isAllBlank(this.userName, this.getPassword());
    }

    @Override
    public UrlArgument getUrlArgument() {
        return url;
    }

    public HgUrlArgument getHgUrlArgument() {
        return url;
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s", url.forDisplay());
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

        HgMaterial that = (HgMaterial) o;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        return result;
    }

    @Override
    protected String getLocation() {
        return getUrlArgument().forDisplay();
    }

    @Override
    public String getTypeForDisplay() {
        return "Mercurial";
    }

    @Override
    public String getShortRevision(String revision) {
        if (revision == null) return null;
        if (revision.length() < 12) return revision;
        return revision.substring(0, 12);
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "mercurial");
        Map<String, Object> configurationMap = new HashMap<>();
        if (addSecureFields) {
            configurationMap.put("url", url.forCommandLine());
        } else {
            configurationMap.put("url", url.forDisplay());
        }
        materialMap.put("mercurial-configuration", configurationMap);
        return materialMap;
    }

    @Override
    public Class getInstanceType() {
        return HgMaterialInstance.class;
    }

    @Override
    public String toString() {
        return "HgMaterial{" +
                "url=" + url +
                '}';
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }


    public String getBranch() {
        if (isNotBlank(branch)) {
            return branch;
        }

        return getBranchFromUrl();
    }

    private String getBranchFromUrl() {
        String[] componentsOfUrl = StringUtils.split(url.originalArgument(), HgUrlArgument.DOUBLE_HASH);
        if (componentsOfUrl.length > 1) {
            return componentsOfUrl[1];
        }
        return HG_DEFAULT_BRANCH;
    }

    @Override
    protected void setGoMaterialVariables(EnvironmentVariableContext environmentVariableContext) {
        super.setGoMaterialVariables(environmentVariableContext);
        setVariableWithName(environmentVariableContext, getBranch(), GitMaterial.GO_MATERIAL_BRANCH);
    }
}
