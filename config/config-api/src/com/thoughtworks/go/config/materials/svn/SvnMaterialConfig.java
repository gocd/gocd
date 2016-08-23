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

package com.thoughtworks.go.config.materials.svn;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.annotation.PostConstruct;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

@ConfigTag(value = "svn", label = "Subversion")
public class SvnMaterialConfig extends ScmMaterialConfig implements ParamsAttributeAware, PasswordEncrypter, PasswordAwareMaterial {
    @ConfigAttribute(value = ScmMaterialConfig.URL)
    private UrlArgument url;

    @ConfigAttribute(value = ScmMaterialConfig.USERNAME, allowNull = true)
    private String userName;

    @SkipParameterResolution
    @ConfigAttribute(value = "password", allowNull = true)
    private String password;

    @ConfigAttribute(value = "encryptedPassword", allowNull = true)
    private String encryptedPassword;

    @ConfigAttribute(value = "checkexternals", allowNull = true, label = "Check externals")
    private boolean checkExternals;

    public static final String URL = ScmMaterialConfig.URL;
    public static final String USERNAME = "userName";
    public static final String CHECK_EXTERNALS = "checkExternals";
    private final GoCipher goCipher;
    public static final String TYPE = "SvnMaterial";

    public SvnMaterialConfig() {
        this(new GoCipher());
    }

    private SvnMaterialConfig(GoCipher goCipher) {
        super(TYPE);
        this.goCipher = goCipher;
    }

    public SvnMaterialConfig(String url, boolean checkExternals) {
        this(url, null, null, checkExternals);
    }

    public SvnMaterialConfig(String url, String userName, String password, boolean checkExternals) {
        this(url, userName, password, checkExternals, new GoCipher());
    }

    public SvnMaterialConfig(String url, String userName, String password, boolean checkExternals, GoCipher goCipher) {
        this(goCipher);
        bombIfNull(url, "null url");
        this.url = new UrlArgument(url);
        this.userName = userName;
        setPassword(password);
        this.checkExternals = checkExternals;
    }
    public SvnMaterialConfig(String url, String userName, boolean checkExternals, GoCipher goCipher) {
        this(goCipher);
        bombIfNull(url, "null url");
        this.url = new UrlArgument(url);
        this.userName = userName;
        this.checkExternals = checkExternals;
    }

    public SvnMaterialConfig(String url, String userName, String password, boolean checkExternals, String folder) {
        this(url, userName, password, checkExternals);
        this.folder = folder;
    }

    public SvnMaterialConfig(UrlArgument url, String userName, String password, boolean checkExternals, GoCipher goCipher, boolean autoUpdate, Filter filter, boolean invertFilter, String folder, CaseInsensitiveString name) {
        super(name, filter, invertFilter, folder, autoUpdate, TYPE, new ConfigErrors());
        this.url = url;
        this.userName = userName;
        this.checkExternals = checkExternals;
        this.goCipher = goCipher;
        this.setPassword(password);
    }

    //for tests
    protected SvnMaterialConfig(UrlArgument url, String password, String encryptedPassword, GoCipher goCipher, Filter filter, boolean invertFilter, String folder) {
        super(new CaseInsensitiveString("test"), filter, invertFilter, folder, true, TYPE, new ConfigErrors());
        this.url = url;
        this.password = password;
        this.encryptedPassword = encryptedPassword;
        this.goCipher = goCipher;
    }

    public GoCipher getGoCipher() {
        return goCipher;
    }

    @Override
    public void setPassword(String password) {
        resetPassword(password);
    }

    public void setCleartextPassword(String password) {
        this.password = password;
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

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getPassword() {
        return currentPassword();
    }

    @Override
    public boolean isCheckExternals() {
        return checkExternals;
    }

    public String currentPassword() {
        try {
            return StringUtil.isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt the password to get the real password", e);
        }
    }

    @Override
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @PostConstruct
    @Override
    public void ensureEncrypted() {
        setPasswordIfNotBlank(password);
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
        return String.format("URL: %s, Username: %s, CheckExternals: %s", url.forDisplay(), userName, checkExternals);
    }

    @Override
    protected String getLocation() {
        return url == null ? null : url.forDisplay();
    }

    @Override
    public void validateConcreteScmMaterial() {
        if (StringUtil.isBlank(url.forDisplay())) {
            errors().add(URL, "URL cannot be blank");
        }
        if (isNotEmpty(this.password) && isNotEmpty(this.encryptedPassword)) {
            addError("password", "You may only specify `password` or `encrypted_password`, not both!");
            addError("encryptedPassword", "You may only specify `password` or `encrypted_password`, not both!");
        }
        if (isNotEmpty(this.encryptedPassword)) {
            try {
                currentPassword();
            } catch (Exception e) {
                addError("encryptedPassword", format("Encrypted password value for svn material with url '%s' is invalid. This usually happens when the cipher text is modified to have an invalid value.",
                       this.getUriForDisplay()));
            }
        }
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

    @Override
    public String getTypeForDisplay() {
        return "Subversion";
    }

    @Override
    public boolean matches(String name, String regex) {
        if (!regex.startsWith("/")) {
            regex = "/" + regex;
        }
        return name.matches(regex);
    }

    public String folderFor(String folderForExternal) {
        return getFolder() == null ? folderForExternal : getFolder() + "/" + folderForExternal;
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

        SvnMaterialConfig that = (SvnMaterialConfig) o;

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
        if (map.containsKey(PASSWORD_CHANGED) && "1".equals(map.get(PASSWORD_CHANGED))) {
            String passwordToSet = (String) map.get(PASSWORD);
            resetPassword(passwordToSet);
        }
        this.checkExternals = "true".equals(map.get(CHECK_EXTERNALS));
    }

    public void setCheckExternals(boolean checkExternals) {
        this.checkExternals = checkExternals;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "SvnMaterialConfig{" +
                "url=" + url +
                ", userName='" + userName + '\'' +
                ", checkExternals=" + checkExternals +
                '}';
    }
}