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

package com.thoughtworks.go.config.materials.tfs;

import com.thoughtworks.go.config.*;
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

import javax.annotation.PostConstruct;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

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

    public TfsMaterialConfig() {
        this(new GoCipher());
    }

    public TfsMaterialConfig(GoCipher goCipher) {
        super(TYPE);
        this.goCipher = goCipher;
    }

    public TfsMaterialConfig(GoCipher goCipher, UrlArgument url, String userName, String domain, String projectPath) {
        this(goCipher);
        this.url = url;
        this.userName = userName;
        this.domain = domain;
        this.projectPath = projectPath;
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
                             Filter filter, boolean invertFilter, String folder, CaseInsensitiveString name) {
        super(name, filter, invertFilter, folder, autoUpdate, TYPE, new ConfigErrors());
        this.url = url;
        this.userName = userName;
        this.domain = domain;
        this.goCipher = goCipher;
        setPassword(password);
        this.projectPath = projectPath;
    }

    //for tests only
    protected TfsMaterialConfig(UrlArgument urlArgument, String password, String encryptedPassword, GoCipher goCipher) {
        this(goCipher);
        this.url = urlArgument;
        this.password = password;
        this.encryptedPassword = encryptedPassword;
    }

    public GoCipher getGoCipher() {
        return goCipher;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    @Override
    public void setPassword(String password) {
        resetPassword(password);
    }

    public void setCleartextPassword(String password) {
        this.password = password;
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
        return url != null ? url.forCommandline() : null;
    }

    @Override
    public void setUrl(String url) {
        if (url != null) {
            this.url = new UrlArgument(url);
        }
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
    public void validateConcreteScmMaterial() {
        if (url == null || StringUtil.isBlank(url.forDisplay())) {
            errors().add(URL, "URL cannot be blank");
        }
        if (StringUtil.isBlank(userName)) {
            errors().add(USERNAME, "Username cannot be blank");
        }
        if (StringUtil.isBlank(projectPath)) {
            errors().add(PROJECT_PATH, "Project Path cannot be blank");
        }
        if (isNotEmpty(this.password) && isNotEmpty(this.encryptedPassword)){
            addError("password", "You may only specify `password` or `encrypted_password`, not both!");
            addError("encryptedPassword", "You may only specify `password` or `encrypted_password`, not both!");
        }

        if(isNotEmpty(this.encryptedPassword)) {
            try{
                goCipher.decrypt(encryptedPassword);
            }catch (Exception e) {
                addError("encryptedPassword", format("Encrypted password value for TFS material with url '%s' is invalid. This usually happens when the cipher text is modified to have an invalid value.",
                        this.getUriForDisplay()));
            }
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

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
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

}
