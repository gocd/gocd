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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.PasswordEncrypter;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bouncycastle.crypto.InvalidCipherTextException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@ConfigTag(value = "tfs", label = "TFS")
public class TfsMaterialConfig extends ScmMaterialConfig implements ParamsAttributeAware, PasswordAwareMaterial, PasswordEncrypter {
    public static final String TYPE = "TfsMaterial";

    @ConfigAttribute(value = "url")
    private UrlArgument url;

    @ConfigAttribute(value = "username")
    private String userName;

    @ConfigAttribute(value = "domain", optional = true)
    private String domain = "";

    @SkipParameterResolution
    @ConfigAttribute(value = "password", allowNull = true)
    private String password;

    @ConfigAttribute(value = "encryptedPassword", allowNull = true)
    private String encryptedPassword;

    @ConfigAttribute(value = "projectPath")
    private String projectPath;

    private final GoCipher goCipher;

    public static final String PROJECT_PATH = "projectPath";
    public static final String DOMAIN = "domain";


    public TfsMaterialConfig(GoCipher goCipher) {
        super(TYPE);
        this.goCipher = goCipher;
    }

    public TfsMaterialConfig(GoCipher goCipher, UrlArgument url, String userName, String domain, String password, String projectPath) {
        this(goCipher);
        this.url = url;
        this.userName = userName;
        this.domain = domain;
        setPassword(password);
        this.projectPath = projectPath;
    }

    public TfsMaterialConfig(UrlArgument url, String userName, String domain, String password, String projectPath, GoCipher goCipher, boolean autoUpdate,
                             Filter filter, String folder, CaseInsensitiveString name) {
        super(name, filter, folder, autoUpdate, TYPE, new ConfigErrors());
        this.url = url;
        this.userName = userName;
        this.domain = domain;
        this.goCipher = goCipher;
        setPassword(password);
        this.projectPath = projectPath;
    }

    public GoCipher getGoCipher() {
        return goCipher;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public void setPassword(String password) {
        resetPassword(password);
    }

    @Override
    public String getPassword() {
        try {
            return StringUtil.isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException("Could not decrypt the password to get the real password", e);
        }
    }

    @Override
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getUrl() {
        return url == null ? null : url.forCommandline();
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
    protected void validateConcreteScmMaterial(ValidationContext validationContext) {
        if (StringUtil.isBlank(url.forDisplay())) {
            errors().add(URL, "URL cannot be blank");
        }
        if (StringUtil.isBlank(userName)) {
            errors().add(USERNAME, "Username cannot be blank");
        }
        if (StringUtil.isBlank(projectPath)) {
            errors().add(PROJECT_PATH, "Project Path cannot be blank");
        }
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.forCommandline());
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

    @PostConstruct
    @Override
    public void ensureEncrypted() {
        setPasswordIfNotBlank(password);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE, true);
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

    public String getProjectPath() {
        return projectPath;
    }

    public String getDomain() {
        return this.domain;
    }

    /* Needed although there is a getUserName above */
    public String getUsername() {
        return userName;
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

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<String, Object>();
        materialMap.put("type", "tfs");
        Map<String, Object> configurationMap = new HashMap<String, Object>();
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
}
