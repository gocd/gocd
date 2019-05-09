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

package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@ConfigTag("git")
public class GitMaterialConfig extends ScmMaterialConfig {

    @ConfigAttribute(value = "url")
    private UrlArgument url;

    @ConfigAttribute(value = "branch")
    private String branch = DEFAULT_BRANCH;

    @ConfigAttribute(value = "shallowClone")
    private boolean shallowClone;

    private String submoduleFolder;

    public static final String TYPE = "GitMaterial";
    public static final String URL = "url";
    public static final String BRANCH = "branch";
    public static final String DEFAULT_BRANCH = "master";
    public static final String SHALLOW_CLONE = "shallowClone";

    public GitMaterialConfig() {
        super(TYPE);
    }

    public GitMaterialConfig(String url) {
        super(TYPE);
        setUrl(url);
    }

    public GitMaterialConfig(String url, String branch) {
        this(url);
        if (branch != null) {
            this.branch = branch;
        }
    }

    public GitMaterialConfig(String url, String branch, Boolean shallowClone) {
        this(url, branch);
        setShallowClone(shallowClone);
    }

    public GitMaterialConfig(UrlArgument url, String branch, String submoduleFolder, boolean autoUpdate, Filter filter, boolean invertFilter, String folder, CaseInsensitiveString name, Boolean shallowClone) {
        super(name, filter, invertFilter, folder, autoUpdate, TYPE, new ConfigErrors());
        this.url = url;
        if (branch != null) {
            this.branch = branch;
        }
        this.submoduleFolder = submoduleFolder;
        this.shallowClone = shallowClone;
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.originalArgument());
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
        return url != null ? url.originalArgument() : null;
    }

    @Override
    public void setUrl(String url) {
        if (url != null) {
            this.url = new UrlArgument(url);
        }
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s, Branch: %s", url.forDisplay(), branch);
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

        GitMaterialConfig that = (GitMaterialConfig) o;

        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }
        if (submoduleFolder != null ? !submoduleFolder.equals(that.submoduleFolder) : that.submoduleFolder != null) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return super.equals(that);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (submoduleFolder != null ? submoduleFolder.hashCode() : 0);
        return result;
    }

    @Override
    public void validateConcreteScmMaterial(ValidationContext validationContext) {
        validateMaterialUrl(this.url, validationContext);
    }

    @Override
    protected String getLocation() {
        return url.forDisplay();
    }

    @Override
    public String getUriForDisplay() {
        return this.url.forDisplay();
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
            this.branch = StringUtils.isBlank(branchName) ? DEFAULT_BRANCH : branchName;
        }
        if (map.containsKey("userName")) {
            this.userName = (String) map.get("userName");
        }
        if (map.containsKey(PASSWORD_CHANGED) && "1".equals(map.get(PASSWORD_CHANGED))) {
            String passwordToSet = (String) map.get(PASSWORD);
            resetPassword(passwordToSet);
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
