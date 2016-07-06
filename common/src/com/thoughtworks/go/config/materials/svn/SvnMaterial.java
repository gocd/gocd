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

package com.thoughtworks.go.config.materials.svn;

import com.thoughtworks.go.config.PasswordEncrypter;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.svn.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static com.thoughtworks.go.util.FileUtil.createParentFolderIfNotExist;
import static java.lang.String.format;

/**
 * @understands configuration for subversion
 */
public class SvnMaterial extends ScmMaterial implements PasswordEncrypter, PasswordAwareMaterial {
    private static final Logger LOGGER = Logger.getLogger(SvnMaterial.class);
    private UrlArgument url;
    private String userName;
    private String password;
    private String encryptedPassword;
    private boolean checkExternals;
    private transient Subversion svnLazyLoaded;

    private final GoCipher goCipher;
    public static final String TYPE = "SvnMaterial";

    private SvnMaterial(GoCipher goCipher) {
        super("SvnMaterial");
        this.goCipher = goCipher;
    }

    public SvnMaterial(String url, String userName, String password, boolean checkExternals) {
        this(url, userName, password, checkExternals, new GoCipher());
    }

    public SvnMaterial(Subversion svn) {
        this(svn.getUrl().forCommandline(), svn.getUserName(), svn.getPassword(), svn.isCheckExternals());
        this.svnLazyLoaded = svn;
    }

    public SvnMaterial(String url, String userName, String password, boolean checkExternals, GoCipher goCipher) {
        this(goCipher);
        bombIfNull(url, "null url");
        setUrl(url);
        this.userName = userName;
        setPassword(password);
        this.checkExternals = checkExternals;
    }

    public SvnMaterial(String url, String userName, String password, boolean checkExternals, String folder) {
        this(url, userName, password, checkExternals);
        this.folder = folder;
    }

    public SvnMaterial(SvnMaterialConfig config) {
        this(config.getUrl(), config.getUserName(), config.getPassword(), config.isCheckExternals(), config.getGoCipher());
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.invertFilter = config.getInvertFilter();
        this.folder = config.getFolder();
        this.name = config.getName();
    }

    @Override
    public MaterialConfig config() {
        return new SvnMaterialConfig(url, userName, getPassword(), checkExternals, goCipher, autoUpdate, filter, invertFilter, folder, name);
    }

    private Subversion svn() {
        if (svnLazyLoaded == null || !svnLazyLoaded.getUrl().equals(url)) {
            svnLazyLoaded = new SvnCommand(getFingerprint(), url.forCommandline(), userName, getPassword(), checkExternals);
        }
        return svnLazyLoaded;
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        return svn().latestModification();
    }

    public List<Modification> modificationsSince(File workingDirectory, Revision revision, final SubprocessExecutionContext execCtx) {
        return svn().modificationsSince(new SubversionRevision(revision.getRevision()));
    }

    public MaterialInstance createMaterialInstance() {
        return new SvnMaterialInstance(url.forCommandline(), userName, UUID.randomUUID().toString(), checkExternals);
    }

    @Override
    protected void appendCriteria(Map parameters) {
        parameters.put(ScmMaterialConfig.URL, url.forCommandline());
        parameters.put(ScmMaterialConfig.USERNAME, userName);
        parameters.put("checkExternals", checkExternals);
    }

    @Override
    protected void appendAttributes(Map parameters) {
        parameters.put(ScmMaterialConfig.URL, url);
        parameters.put(ScmMaterialConfig.USERNAME, userName);
        parameters.put("checkExternals", checkExternals);
    }

    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        Revision revision = revisionContext.getLatestRevision();
        File workingDir = execCtx.isServer() ? baseDir : workingdir(baseDir);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Updating to revision: " + revision + " in workingdirectory " + workingDir);
        }
        outputStreamConsumer.stdOutput(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, updatingTarget(), revision.getRevision(), url));
        boolean shouldDoFreshCheckout = !workingDir.isDirectory() || isRepositoryChanged(workingDir);
        if (shouldDoFreshCheckout) {
            freshCheckout(outputStreamConsumer, new SubversionRevision(revision), workingDir);
        } else {
            cleanupAndUpdate(outputStreamConsumer, new SubversionRevision(revision), workingDir);
        }
        LOGGER.debug("done with update");
        outputStreamConsumer.stdOutput(format("[%s] Done.\n", GoConstants.PRODUCT_NAME));
    }

    public boolean isRepositoryChanged(File workingFolder) {
        try {
            File file = new File(workingFolder, ".svn");
            if (workingFolder.isDirectory() && file.exists() && file.isDirectory()) {
                String workingUrl = svn().workingRepositoryUrl(workingFolder);
                return !MaterialUrl.sameUrl(url.toString(), workingUrl);
            } else {
                return true;
            }
        } catch (IOException e) {
            return true;
        }
    }

    public void freshCheckout(ProcessOutputStreamConsumer outputStreamConsumer, SubversionRevision revision,
                              File workingFolder) {
        if (workingFolder.isDirectory()) {
            FileUtil.deleteFolder(workingFolder);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking out to revision " + revision + " in " + workingFolder);
        }
        createParentFolderIfNotExist(workingFolder);
        svn().checkoutTo(outputStreamConsumer, workingFolder, revision);
    }

    public void cleanupAndUpdate(ProcessOutputStreamConsumer outputStreamConsumer, SubversionRevision revision,
                                 File workingFolder) {
        try {
            svn().cleanupAndRevert(outputStreamConsumer, workingFolder);
        } catch (Exception e) {
            String message = "Failed to do cleanup and revert in " + workingFolder.getAbsolutePath();
            LOGGER.error(message);
            LOGGER.debug(message, e);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Updating to revision " + revision + " on " + workingFolder);
        }
        svn().updateTo(outputStreamConsumer, workingFolder, revision);
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

        SvnMaterial that = (SvnMaterial) o;

        if (checkExternals != that.checkExternals) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
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
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (checkExternals ? 1 : 0);
        return result;
    }

    protected String getLocation() {
        return url == null ? null : url.forDisplay();
    }

    public String getTypeForDisplay() {
        return "Subversion";
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "svn");
        Map<String, Object> configurationMap = new HashMap<>();
        if (addSecureFields) {
            configurationMap.put("url", url.forCommandline());
        } else {
            configurationMap.put("url", url.forDisplay());
        }
        configurationMap.put("username", userName);
        if (addSecureFields) {
            configurationMap.put("password", getPassword());
        }
        configurationMap.put("check-externals", checkExternals);
        materialMap.put("svn-configuration", configurationMap);
        return materialMap;
    }

    public Class getInstanceType() {
        return SvnMaterialInstance.class;
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        return svn().checkConnection();
    }

    public String getUrl() {
        return url == null ? null : url.forCommandline();
    }

    @Override protected UrlArgument getUrlArgument() {
        return url;
    }

    public String getLongDescription() {
        return String.format("URL: %s, Username: %s, CheckExternals: %s", url.forDisplay(), userName, checkExternals);
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return currentPassword();
    }

    public void setUrl(String url) {
        this.url = new UrlArgument(url);
    }

    public void setPassword(String password) {
        resetPassword(password);
    }

    public boolean isCheckExternals() {
        return checkExternals;
    }

    private String folderFor(String folderForExternal) {
        return getFolder() == null ? folderForExternal : getFolder() + "/" + folderForExternal;
    }

    public void add(ProcessOutputStreamConsumer outputStreamConsumer, File file) {
        svn().add(outputStreamConsumer, file);
    }

    public void commit(ProcessOutputStreamConsumer outputStreamConsumer, File workingDir, String message) {
        svn().commit(outputStreamConsumer, workingDir, message);
    }

    @Override
    public boolean matches(String name, String regex) {
        if (!regex.startsWith("/")) {
            regex = "/" + regex;
        }
        return name.matches(regex);
    }

    @Override public String toString() {
        return "SvnMaterial{" +
                "url=" + url +
                ", userName='" + userName + '\'' +
                ", checkExternals=" + checkExternals +
                '}';
    }

    /**
     * @deprecated used only in tests - we need to disentangle this
     */
    public static SvnMaterial createSvnMaterialWithMock(Subversion svn) {
        return new SvnMaterial(svn);
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

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    @PostConstruct
    public void ensureEncrypted() {
        setPasswordIfNotBlank(password);
    }

    public String currentPassword() {
        try {
            return StringUtil.isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException("Could not decrypt the password to get the real password", e);
        }
    }
}
