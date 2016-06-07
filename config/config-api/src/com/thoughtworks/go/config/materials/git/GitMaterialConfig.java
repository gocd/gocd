/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.PasswordEncrypter;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.annotation.PostConstruct;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@ConfigTag("git")
public class GitMaterialConfig extends ScmMaterialConfig implements PasswordEncrypter {
    private final GoCipher goCipher;

    @ConfigAttribute(value = "url")
    private UrlArgument url;

    @ConfigAttribute(value = "branch")
    private String branch = DEFAULT_BRANCH;

    @ConfigAttribute(value = "shallowClone")
    private boolean shallowClone;

    private String submoduleFolder;

    @SkipParameterResolution
    @ConfigAttribute(value = "sshKey", allowNull = true)
    private String sshKey;

    @ConfigAttribute(value = "encryptedSshKey", allowNull = true)
    private String encryptedSshKey;

    public static final String TYPE = "GitMaterial";
    public static final String URL = "url";
    public static final String BRANCH = "branch";
    public static final String DEFAULT_BRANCH = "master";
    public static final String SHALLOW_CLONE = "shallowClone";

    private GitMaterialConfig(GoCipher goCipher) {
        super(TYPE);
        this.goCipher = goCipher;
    }

    public GitMaterialConfig() {
        this(new GoCipher());
    }

    public GitMaterialConfig(String url) {
        this(url, null);
    }

    public GitMaterialConfig(String url, String branch) {
        this(url, branch, false);
    }

    public GitMaterialConfig(String url, String branch, boolean shallowClone) {
        this(new UrlArgument(url), branch, null, false, null, null, null, shallowClone, null);
    }

    public GitMaterialConfig(UrlArgument url, String branch, String submoduleFolder, boolean autoUpdate, Filter filter, String folder, CaseInsensitiveString name, boolean shallowClone, String sshKey) {
        super(name, filter, folder, autoUpdate, TYPE, new ConfigErrors());
        this.goCipher = new GoCipher();
        this.url = url;
        if (branch != null) {
            this.branch = branch;
        }
        this.submoduleFolder = submoduleFolder;
        this.shallowClone = shallowClone;
        this.sshKey = sshKey;
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.forCommandline());
        parameters.put("branch", branch);
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("url", url);
        parameters.put("branch", branch);
        parameters.put("shallowClone", shallowClone);
    }

    @Override
    public String getUrl() {
        return url != null ? url.forCommandline() : null;
    }

    @Override
    public void setUrl(String url) {
        if (url != null) {
            this.url = new UrlArgument(url);
        }
    }

    @Override
    public UrlArgument getUrlArgument() {
        return url;
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s, Branch: %s", url.forDisplay(), branch);
    }

    public String getClearTextSshKey() {
        try {
            return StringUtil.isBlank(encryptedSshKey) ? null : this.goCipher.decrypt(encryptedSshKey);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException("Could not decrypt the SSH key", e);
        }
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

    public String getEncryptedSshKey() {
        return encryptedSshKey;
    }

    public void setEncryptedSshKey(String encryptedSshKey) {
        this.encryptedSshKey = encryptedSshKey;
    }

    @Override
    @PostConstruct
    public void ensureEncrypted() {
        if (StringUtils.isNotBlank(sshKey)) {
            try {
                this.encryptedSshKey = this.goCipher.encrypt(sshKey);
                this.sshKey = null;
            } catch (Exception e) {
                bomb("Ssh Key encryption failed. Please verify your cipher key.", e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GitMaterialConfig that = (GitMaterialConfig) o;

        if (shallowClone != that.shallowClone) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (branch != null ? !branch.equals(that.branch) : that.branch != null) return false;
        if (submoduleFolder != null ? !submoduleFolder.equals(that.submoduleFolder) : that.submoduleFolder != null)
            return false;
        if (sshKey != null ? !sshKey.equals(that.sshKey) : that.sshKey != null) return false;
        return encryptedSshKey != null ? encryptedSshKey.equals(that.encryptedSshKey) : that.encryptedSshKey == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (shallowClone ? 1 : 0);
        result = 31 * result + (submoduleFolder != null ? submoduleFolder.hashCode() : 0);
        result = 31 * result + (sshKey != null ? sshKey.hashCode() : 0);
        result = 31 * result + (encryptedSshKey != null ? encryptedSshKey.hashCode() : 0);
        return result;
    }

    @Override
    public void validateConcreteScmMaterial() {
        if (url == null || StringUtil.isBlank(url.forDisplay())) {
            errors().add(URL, "URL cannot be blank");
        }
    }

    @Override
    protected String getLocation() {
        return url.forDisplay();
    }

    @Override
    public String getTypeForDisplay() {
        return "Git";
    }

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getSubmoduleFolder() {
        return submoduleFolder;
    }

    public void setSubmoduleFolder(String submoduleFolder) {
        this.submoduleFolder = submoduleFolder;
    }

    @Override
    public String getUserName() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getEncryptedPassword() {
        return null;
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getShortRevision(String revision) {
        if (revision == null) return null;
        if (revision.length() < 7) return revision;
        return revision.substring(0, 7);
    }

    @Override
    public String toString() {
        return "GitMaterialConfig{" +
                "url=" + url +
                ", branch='" + branch + '\'' +
                ", submoduleFolder='" + submoduleFolder + '\'' +
                ", shallowClone=" + shallowClone +
                '}';
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        if (map.containsKey(BRANCH)) {
            String branchName = (String) map.get(BRANCH);
            this.branch = StringUtil.isBlank(branchName) ? DEFAULT_BRANCH : branchName;
        }
        if (map.containsKey(URL)) {
            this.url = new UrlArgument((String) map.get(URL));
        }

        this.shallowClone = "true".equals(map.get(SHALLOW_CLONE));
    }

    public boolean isShallowClone() {
        return shallowClone;
    }

    public void setShallowClone(Boolean shallowClone) {
        if (shallowClone != null) {
            this.shallowClone = shallowClone;
        }
    }
}
