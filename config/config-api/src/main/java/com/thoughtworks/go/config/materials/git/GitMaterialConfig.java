/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.util.command.UrlArgument;

import java.util.Map;
import java.util.Objects;

import static com.thoughtworks.go.config.materials.git.RefSpecHelper.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

@ConfigTag("git")
public class GitMaterialConfig extends ScmMaterialConfig implements PasswordAwareMaterial {
    public static final String TYPE = "GitMaterial";
    public static final String URL = "url";
    public static final String BRANCH = "branch";
    public static final String DEFAULT_BRANCH = "master";
    public static final String SHALLOW_CLONE = "shallowClone";

    @ConfigAttribute(value = "url")
    private UrlArgument url;

    @ConfigAttribute(value = "branch")
    private String branch = DEFAULT_BRANCH;

    @ConfigAttribute(value = "shallowClone")
    private boolean shallowClone;

    private String submoduleFolder;

    public GitMaterialConfig() {
        super(TYPE);
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GitMaterialConfig that = (GitMaterialConfig) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(branch, that.branch) &&
                Objects.equals(submoduleFolder, that.submoduleFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url, branch, submoduleFolder);
    }

    @Override
    public void validateConcreteScmMaterial(ValidationContext validationContext) {
        validateMaterialUrl(this.url);
        validateBranchOrRefSpec(this.branch);
        validateCredentials();
        validateEncryptedPassword();
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
            this.branch = isBlank(branchName) ? DEFAULT_BRANCH : branchName;
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
    protected String getLocation() {
        return url.forDisplay();
    }

    /**
     * This does not thoroughly validate the ref format as `git check-ref-format` would; it only does the
     * basic necessities of checking for the presence of refspec components and asserting that no wildcards
     * are present, which probably has the most critical impact to the proper use of refspecs as they pertain
     * to GoCD materials.
     *
     * @param branchOrRefSpec a branch or a refspec
     */
    private void validateBranchOrRefSpec(String branchOrRefSpec) {
        if (isBlank(branchOrRefSpec)) {
            return;
        }

        if (hasRefSpec(branchOrRefSpec)) {
            final String source = findSource(branchOrRefSpec);
            final String dest = findDest(branchOrRefSpec);

            if (isBlank(source)) {
                errors().add(BRANCH, "Refspec is missing a source ref");
            } else {
                if (!source.startsWith("refs/")) {
                    errors().add(BRANCH, "Refspec source must be an absolute ref (must start with `refs/`)");
                }
            }

            if (isBlank(dest)) {
                errors().add(BRANCH, "Refspec is missing a destination ref");
            }

            if (branchOrRefSpec.contains("*")) {
                errors().add(BRANCH, "Refspecs may not contain wildcards; source and destination refs must be exact");
            }
        } else {
            if (branchOrRefSpec.contains("*")) {
                errors().add(BRANCH, "Branch names may not contain '*'");
            }
        }
    }
}
