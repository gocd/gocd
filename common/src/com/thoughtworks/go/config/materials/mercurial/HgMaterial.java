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

package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.mercurial.HgCommand;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialInstance;
import com.thoughtworks.go.domain.materials.svn.MaterialUrl;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfFailedToRunCommandLine;
import static com.thoughtworks.go.util.FileUtil.createParentFolderIfNotExist;
import static java.lang.String.format;

/**
 * @understands configuration for mercurial version control
 */
public class HgMaterial extends ScmMaterial {
    private static final Pattern HG_VERSION_PATTERN = Pattern.compile(".*\\(.*\\s+(\\d(\\.\\d)+.*)\\)");
    private static final Logger LOGGER = Logger.getLogger(HgMaterial.class);
    private HgUrlArgument url;

    //TODO: use iBatis to set the type for us, and we can get rid of this field.
    public static final String TYPE = "HgMaterial";
    private static final String ERROR_OLD_VERSION = "Please install Mercurial Version 1.0 or above."
            + " The current installed hg is ";
    private static final String ERR_NO_HG_INSTALLED =
            "Failed to find 'hg' on your PATH. Please ensure 'hg' is executable by the Go Server and on the Go Agents where this material will be used.";

    private final String HG_DEFAULT_BRANCH = "default";

    private HgMaterial() {
        super(TYPE);
    }

    public HgMaterial(String url, String folder) {
        this();
        this.url = new HgUrlArgument(url);
        this.folder = folder;
    }

    public HgMaterial(HgMaterialConfig config) {
        this(config.getUrl(), config.getFolder());
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.invertFilter = config.getInvertFilter();
        this.name = config.getName();
    }

    @Override
    public MaterialConfig config() {
        return new HgMaterialConfig(url, autoUpdate, filter, invertFilter, folder, name);
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        HgCommand hgCommand = getHg(baseDir);
        return hgCommand.latestOneModificationAsModifications();
    }


    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        return getHg(baseDir).modificationsSince(revision);
    }

    public MaterialInstance createMaterialInstance() {
        return new HgMaterialInstance(url.forCommandline(), UUID.randomUUID().toString());
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.forCommandline());
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

    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
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

    boolean isVersionOnedotZeorOrHigher(String hgout) {
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
        try {
            HgCommand.checkConnection(url);
            return ValidationBean.valid();
        } catch (Exception e) {
            String message = null;
            try {
                message = HgCommand.version();
                return handleException(e, message);
            } catch (Exception ex) {
                return ValidationBean.notValid(ERR_NO_HG_INSTALLED);
            }
        }
    }


    ValidationBean handleException(Exception e, String hgVersionConsoleOut) {
        ValidationBean defaultResponse = ValidationBean.notValid(
                "Repository " + url.forDisplay() + " not found!" + " : \n" + e.getMessage());
        try {
            boolean olderThanHg10 = !isVersionOnedotZeorOrHigher(hgVersionConsoleOut);
            if (olderThanHg10) {
                return ValidationBean.notValid(ERROR_OLD_VERSION + hgVersionConsoleOut);
            } else {
                return defaultResponse;
            }
        } catch (Exception e1) {
            LOGGER.debug("Problem validating HG", e);
            return defaultResponse;
        }
    }


    private HgCommand hg(File workingFolder, ProcessOutputStreamConsumer outputStreamConsumer) throws Exception {
        HgCommand hgCommand = new HgCommand(getFingerprint(), workingFolder, getBranch(), getUrl());
        if (!isHgRepository(workingFolder) || isRepositoryChanged(hgCommand)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid hg working copy or repository changed. Delete folder: " + workingFolder);
            }
            FileUtil.deleteFolder(workingFolder);
        }
        if (!workingFolder.exists()) {
            createParentFolderIfNotExist(workingFolder);
            int returnValue = hgCommand.clone(outputStreamConsumer, url);
            bombIfFailedToRunCommandLine(returnValue, "Failed to run hg clone command");
        }
        return hgCommand;
    }

    private boolean isHgRepository(File workingFolder) {
        return new File(workingFolder, ".hg").isDirectory();
    }

    private boolean isRepositoryChanged(HgCommand hgCommand) {
        ConsoleResult result = hgCommand.workingRepositoryUrl();
        return !MaterialUrl.sameUrl(url.defaultRemoteUrl(), new HgUrlArgument(result.outputAsString()).defaultRemoteUrl());
    }

    public String getUserName() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    @Override public String getEncryptedPassword() {
        return null;
    }

    public boolean isCheckExternals() {
        return false;
    }

    public String getUrl() {
        return url.forCommandline();
    }

    public UrlArgument getUrlArgument() {
        return url;
    }

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

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    protected String getLocation() {
        return getUrlArgument().forDisplay();
    }

    public String getTypeForDisplay() {
        return "Mercurial";
    }

    @Override public String getShortRevision(String revision) {
        if (revision == null) return null;
        if (revision.length()<12) return revision;
        return revision.substring(0,12);
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "mercurial");
        Map<String, Object> configurationMap = new HashMap<>();
        if (addSecureFields) {
            configurationMap.put("url", url.forCommandline());
        } else {
            configurationMap.put("url", url.forDisplay());
        }
        materialMap.put("mercurial-configuration", configurationMap);
        return materialMap;
    }

    public Class getInstanceType() {
        return HgMaterialInstance.class;
    }

    @Override public String toString() {
        return "HgMaterial{" +
                "url=" + url +
                '}';
    }

    String getBranch() {
        return getBranchFromUrl(url.forCommandline());
    }

    private String getBranchFromUrl(String url) {
        String[] componentsOfUrl = StringUtils.split(url.toString(), HgUrlArgument.DOUBLE_HASH);
        if (componentsOfUrl.length > 1) {
            return componentsOfUrl[1];
        }
        return HG_DEFAULT_BRANCH;
    }

}
