/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import javax.annotation.PostConstruct;

import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.StringUtil;
import org.bouncycastle.crypto.InvalidCipherTextException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.StringUtil.nullToBlank;

@ConfigTag("ldap")
public class LdapConfig implements Validatable, PasswordEncrypter{
    @ConfigAttribute(value = "uri") private String uri = "";
    @ConfigAttribute(value = "managerDn") private String managerDn = "";
    @ConfigAttribute(value = "managerPassword") private String managerPassword = "";
    @ConfigAttribute(value = "encryptedManagerPassword", allowNull = true) private String encryptedManagerPassword = null;
    @ConfigAttribute(value = "searchFilter") private String searchFilter = "";
    @ConfigSubtag(optional = false) private BasesConfig basesConfig = new BasesConfig();
    private ConfigErrors errors = new ConfigErrors();
    private boolean passwordChanged;
    private final GoCipher goCipher;
    private static final LdapConfig EMPTY_LDAP_CONFIG = new LdapConfig(new GoCipher());

    public LdapConfig(GoCipher goCipher) {
        this.goCipher = goCipher;
    }

    public LdapConfig(String uri, String managerDn, String managerPassword, String encryptedManagerPassword, boolean passwordChanged, BasesConfig basesConfig, String searchFilter) {
        this(new GoCipher());
        this.uri = nullToBlank(uri);
        this.managerDn = nullToBlank(managerDn);
        this.encryptedManagerPassword = encryptedManagerPassword;
        this.passwordChanged = passwordChanged;
        if (passwordChanged) {
            resetPassword(managerPassword);
            this.managerPassword = nullToBlank(managerPassword); // This is for the double save that happens through rails. We do not want to flip passwordChanged after encrypting as it modifies model state unknown to the user
        }
        this.basesConfig = basesConfig;
        this.searchFilter = nullToBlank(searchFilter);
    }

    public String uri() {
        return uri;
    }

    public String managerDn() {
        return managerDn;
    }

    public String managerPassword() {
        return currentManagerPassword();
    }

    public BasesConfig searchBases() {
        return basesConfig;
    }

    public String searchFilter() {
        return searchFilter;
    }

    public String getEncryptedManagerPassword() {
        return encryptedManagerPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LdapConfig that = (LdapConfig) o;

        if (basesConfig != null ? !basesConfig.equals(that.basesConfig) : that.basesConfig != null) {
            return false;
        }
        if (encryptedManagerPassword != null ? !encryptedManagerPassword.equals(that.encryptedManagerPassword) : that.encryptedManagerPassword != null) {
            return false;
        }
        if (managerDn != null ? !managerDn.equals(that.managerDn) : that.managerDn != null) {
            return false;
        }
        if (searchFilter != null ? !searchFilter.equals(that.searchFilter) : that.searchFilter != null) {
            return false;
        }
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (managerDn != null ? managerDn.hashCode() : 0);
        result = 31 * result + (encryptedManagerPassword != null ? encryptedManagerPassword.hashCode() : 0);
        result = 31 * result + (searchFilter != null ? searchFilter.hashCode() : 0);
        result = 31 * result + (basesConfig != null ? basesConfig.hashCode() : 0);
        return result;
    }

    public boolean isEnabled() {
        return !equals(EMPTY_LDAP_CONFIG);
    }

    public void validate(ValidationContext validationContext) {
        if (isEnabled()) {
            if (!validationContext.systemEnvironment().inbuiltLdapPasswordAuthEnabled()) {
                errors.add("base", "'ldap' tag has been deprecated in favour of bundled LDAP plugin. Use that instead.");
                return;
            }
            basesConfig.validateBases();
            for (BaseConfig baseConfig : basesConfig) {
                baseConfig.validateBase();
            }
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @PostConstruct
    public void ensureEncrypted() {
        setPasswordIfNotBlank(managerPassword);
    }

    public void updateWithNew(LdapConfig newLdapConfig) {
        if (newLdapConfig.passwordChanged) {
            this.encryptedManagerPassword = newLdapConfig.encryptedManagerPassword;
        }
        this.uri = newLdapConfig.uri;
        this.managerDn = newLdapConfig.managerDn;
        this.basesConfig = newLdapConfig.getBasesConfig();
        this.searchFilter = newLdapConfig.searchFilter;
    }

    private void resetPassword(String password) {
        if (StringUtil.isBlank(password)) {
            this.encryptedManagerPassword = null;
        }
        setPasswordIfNotBlank(password);
    }

    private void setPasswordIfNotBlank(String password) {
        if (StringUtil.isBlank(password)) {
            return;
        }
        try {
            this.encryptedManagerPassword = this.goCipher.encrypt(password);
        } catch (Exception e) {
            bomb("Password encryption failed. Please verify your cipher key.", e);
        }
        this.managerPassword = "";
    }

    public String currentManagerPassword() {
        try {
            return StringUtil.isBlank(encryptedManagerPassword) ? "" : this.goCipher.decrypt(encryptedManagerPassword);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException("Could not decrypt the password to get the real password", e);
        }
    }

    public boolean isPasswordChanged() {
        return passwordChanged;
    }

    public BasesConfig getBasesConfig() {
        return basesConfig;
    }
}
