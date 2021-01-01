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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
@AllArgsConstructor(access = AccessLevel.NONE)
@ConfigTag("mailhost")
public class MailHost implements Validatable, PasswordEncrypter {
    public static final String DOES_NOT_LOOK_LIKE_A_VALID_EMAIL_ADDRESS = "Does not look like a valid email address.";
    @ConfigAttribute(value = "hostname", optional = false)
    private String hostName;
    @ConfigAttribute(value = "port", optional = false)
    private int port;
    @ConfigAttribute(value = "username", optional = true, allowNull = true)
    private String username;
    @EqualsAndHashCode.Exclude
    @ConfigAttribute(value = "password", optional = true, allowNull = true)
    private String password;
    @EqualsAndHashCode.Exclude
    @ConfigAttribute(value = "encryptedPassword", optional = true, allowNull = true)
    private String encryptedPassword = null;
    @ConfigAttribute(value = "tls", optional = true)
    private boolean tls;
    @ConfigAttribute(value = "from", optional = false)
    private String from;
    @ConfigAttribute(value = "admin", optional = false)
    private String adminMail;

    @EqualsAndHashCode.Exclude
    private final ConfigErrors configErrors = new ConfigErrors();
    @EqualsAndHashCode.Exclude
    private final GoCipher goCipher;
    @EqualsAndHashCode.Exclude
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

    public MailHost() {
        this(new GoCipher());
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (isBlank(hostName)) {
            errors().add("hostname", "Hostname must not be blank.");
        }

        if (port <= 0) {
            errors().add("port", "Port must be a positive number.");
        }

        if (isBlank(from)) {
            errors().add("sender_email", "Sender email must not be blank.");
        } else if (!from.matches(".*@.*")) {
            errors().add("sender_email", DOES_NOT_LOOK_LIKE_A_VALID_EMAIL_ADDRESS);
        }

        if (isBlank(adminMail)) {
            errors().add("admin_email", "Admin email must not be blank.");
        } else if (!adminMail.matches(".*@.*")) {
            errors().add("admin_email", DOES_NOT_LOOK_LIKE_A_VALID_EMAIL_ADDRESS);
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Tolerate
    @Deprecated // use `getUsername()` instead
    public String getUserName() {
        return getUsername();
    }

    public String getPassword() {
        return getCurrentPassword();
    }

    @Tolerate
    @Deprecated // use `isTls()` instead, left here for rails
    public boolean getTls() {
        return isTls();
    }

    @Override
    @PostConstruct
    public void ensureEncrypted() {
        this.username = StringUtils.stripToNull(username);

        setPasswordIfNotBlank(password);
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
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
        if (StringUtils.isBlank(password)) {
            this.encryptedPassword = null;
        }
        setPasswordIfNotBlank(password);
    }

    private void setPasswordIfNotBlank(String password) {
        this.password = StringUtils.stripToNull(password);
        this.encryptedPassword = StringUtils.stripToNull(encryptedPassword);

        if (this.password == null) {
            return;
        }
        try {
            this.encryptedPassword = this.goCipher.encrypt(password);
        } catch (Exception e) {
            bomb("Password encryption failed. Please verify your cipher key.", e);
        }
        this.password = null;
    }

    public String getCurrentPassword() {
        try {
            return isBlank(encryptedPassword) ? null : this.goCipher.decrypt(encryptedPassword);
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
    }
}
