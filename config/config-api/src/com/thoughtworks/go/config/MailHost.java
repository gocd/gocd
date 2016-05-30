/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.ConfigErrors;

import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.StringUtil;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.defaultString;

@ConfigTag("mailhost")
public class MailHost implements Validatable, PasswordEncrypter {
    @ConfigAttribute(value = "hostname", optional = false) private String hostName;
    @ConfigAttribute(value = "port", optional = false) private int port;
    @ConfigAttribute(value = "username", optional = true, allowNull = true) private String username = "";
    @ConfigAttribute(value = "password", optional = true, allowNull = true) private String password = "";//should never be used, will be converted to encrypted password on load, is used by magical-loader
    @ConfigAttribute(value = "encryptedPassword", optional = true, allowNull = true) private String encryptedPassword = null;
    @ConfigAttribute(value = "tls", optional = false) private Boolean tls;
    @ConfigAttribute(value = "from", optional = false) private String from;
    @ConfigAttribute(value = "admin", optional = false) private String adminMail;

    private final ConfigErrors configErrors = new ConfigErrors();
    private final GoCipher goCipher;
    private boolean passwordChanged = false;


    public MailHost(String hostName, int port, String username, String password, boolean passwordChanged, boolean tls, String from, String adminMail) {
        this(hostName, port, username, password, null, passwordChanged, tls, from, adminMail, new GoCipher());
    }

    public MailHost(String hostName, int port, String username, String password, String encryptedPassword, boolean passwordChanged, boolean tls, String from, String adminMail, GoCipher goCipher) {
        this(goCipher);
        this.hostName = hostName;
        this.port = port;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        if (passwordChanged) {
            resetPassword(password);
            this.password = password; // This is for the double save that happens through rails. We do not want to flip passwordChanged after encrypting as it modifies model state unknown to the user
        }
        this.passwordChanged = passwordChanged;
        this.tls = tls;
        this.from = from;
        this.adminMail = adminMail;
    }

    public MailHost(String hostName, int port, String username, String password, String encryptedPassword, boolean passwordChanged, boolean tls, String from, String adminMail) {
        this(hostName, port, username, password, encryptedPassword, passwordChanged, tls, from, adminMail, new GoCipher());
    }

    public MailHost(GoCipher goCipher) {
        this.goCipher = goCipher;
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MailHost mailHost = (MailHost) o;

        if (port != mailHost.port) {
            return false;
        }
        if (tls != mailHost.tls) {
            return false;
        }
        if (getAdminMail() != null ? !getAdminMail().equals(
                mailHost.getAdminMail()) : mailHost.getAdminMail() != null) {
            return false;
        }
        if (getFrom() != null ? !getFrom().equals(mailHost.getFrom()) : mailHost.getFrom() != null) {
            return false;
        }
        if (hostName != null ? !hostName.equals(mailHost.hostName) : mailHost.hostName != null) {
            return false;
        }
        if (username != null ? !username.equals(mailHost.username) : mailHost.username != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (hostName != null ? hostName.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (Boolean.TRUE.equals(tls) ? 1 : 0);
        result = 31 * result + (getFrom() != null ? getFrom().hashCode() : 0);
        result = 31 * result + (getAdminMail() != null ? getAdminMail().hashCode() : 0);
        return result;
    }

    public String toString() {
        return format("MailHost[%s, %s, %s, %s, %s, %s, %s]", hostName, port, username, password, encryptedPassword, tls, getFrom(), getAdminMail());
    }

    public Map json() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("hostName", defaultString(hostName));
        model.put("port", port == 0 ? "" : valueOf(port));
        model.put("username", defaultString(username));
        model.put("password", defaultString(getPassword()));
        model.put("tls", tls == null ? "false" : tls.toString());
        model.put("from", defaultString(getFrom()));
        model.put("adminMail", defaultString(getAdminMail()));
        return model;
    }

    public String getFrom() {
        return from;
    }

    public String getAdminMail() {
        return adminMail;
    }

    public String getHostName() {
        return hostName;
    }

    public String getUserName() {
        return username;
    }

    public String getPassword() {
        return getCurrentPassword();
    }

    public int getPort() {
        return port;
    }

    public Boolean getTls() {
        return tls;
    }

    @Deprecated // Only for test
    public void setAdminMail(String adminMail) {
        this.adminMail = adminMail;
    }

    @PostConstruct
    public void ensureEncrypted() {
        setPasswordIfNotBlank(password);
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public boolean isPasswordChanged() {
        return passwordChanged;
    }

    public void updateWithNew(MailHost newMailHost) {//TODO: #5086 kill passing mailhost around, pass arguments instead
        if (newMailHost.isPasswordChanged()) {
            resetPassword(newMailHost.password);
        }
        this.hostName = newMailHost.hostName;
        this.port = newMailHost.port;
        this.username = newMailHost.username;
        this.tls = newMailHost.tls;
        this.from = newMailHost.from;
        this.adminMail = newMailHost.adminMail;
    }

    private void resetPassword(String password) {
        if (StringUtil.isBlank(password)) {
            this.encryptedPassword = null;
        }
        setPasswordIfNotBlank(password);
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
        this.password = "";
    }

    public String getCurrentPassword() {
        try {
            return StringUtil.isBlank(encryptedPassword) ? "" : this.goCipher.decrypt(encryptedPassword);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e);
        }
    }
}
