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
package com.thoughtworks.go.config.materials.tfs;

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Map;

@ConfigTag(value = "tfs", label = "TFS")
public class TfsMaterialConfig extends ScmMaterialConfig implements ParamsAttributeAware, PasswordAwareMaterial {
    public static final String TYPE = "TfsMaterial";

    @ConfigAttribute(value = "url")
    private UrlArgument url;

    @ConfigAttribute(value = "domain", optional = true)
    private String domain = "";

    @ConfigAttribute(value = "projectPath")
    private String projectPath;

    public static final String PROJECT_PATH = "projectPath";
    public static final String DOMAIN = "domain";

    public TfsMaterialConfig() {
        super(TYPE);
    }

    @Override
    public boolean isCheckExternals() {
        return false;
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
        return String.format("URL: %s, Username: %s, Domain: %s, ProjectPath: %s", url.forDisplay(), userName, domain, projectPath);
    }

    @Override
    protected String getLocation() {
        return url == null ? null : url.forDisplay();
    }

    @Override
    public String getUriForDisplay() {
        return this.url.forDisplay();
    }

    @Override
    public void validateConcreteScmMaterial(ValidationContext validationContext) {
        validateMaterialUrl(this.url);

        if (StringUtils.isBlank(userName)) {
            errors().add(USERNAME, "Username cannot be blank");
        }
        if (StringUtils.isBlank(projectPath)) {
            errors().add(PROJECT_PATH, "Project Path cannot be blank");
        }
        validateEncryptedPassword();
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.originalArgument());
        parameters.put(ScmMaterialConfig.USERNAME, userName);
        parameters.put(DOMAIN, domain);
        parameters.put(PROJECT_PATH, projectPath);
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        appendCriteria(parameters);
    }

    @Override
    public String getTypeForDisplay() {
        return "Tfs";
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE, true);
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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

        TfsMaterialConfig material = (TfsMaterialConfig) o;

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
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        if (map.containsKey(URL)) {
            this.url = new UrlArgument((String) map.get(URL));
        }
        if (map.containsKey(USERNAME)) {
            this.userName = (String) map.get(USERNAME);
        }
        if (map.containsKey(DOMAIN)) {
            this.domain = (String) map.get(DOMAIN);
        }
        if (map.containsKey(PASSWORD_CHANGED) && "1".equals(map.get(PASSWORD_CHANGED))) {
            String passwordToSet = (String) map.get(PASSWORD);
            resetPassword(passwordToSet);
        }
        if (map.containsKey(PROJECT_PATH)) {
            this.projectPath = (String) map.get(PROJECT_PATH);
        }

    }

}
