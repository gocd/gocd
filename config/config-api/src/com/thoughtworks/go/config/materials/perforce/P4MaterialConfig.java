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

package com.thoughtworks.go.config.materials.perforce;

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

@ConfigTag(value = "p4", label = "Perforce")
public class P4MaterialConfig extends ScmMaterialConfig implements ParamsAttributeAware, PasswordEncrypter, PasswordAwareMaterial {
    @ConfigAttribute(value = "port")
    private String serverAndPort;

    @ConfigAttribute(value = "username", allowNull = true)
    private String userName;

    @SkipParameterResolution
    @ConfigAttribute(value = "password", allowNull = true)
    private String password;

    @ConfigAttribute(value = "encryptedPassword", allowNull = true)
    private String encryptedPassword;

    @ConfigAttribute(value = "useTickets")
    private Boolean useTickets = false;

    @ConfigSubtag(optional = false)
    private P4MaterialViewConfig view;

    public static final String TYPE = "P4Material";
    public static final String USERNAME = "userName";
    public static final String VIEW = "view";
    public static final String SERVER_AND_PORT = "serverAndPort";
    public static final String USE_TICKETS = "useTickets";
    private final GoCipher goCipher;

    public P4MaterialConfig() {
        this(new GoCipher());
    }

    private P4MaterialConfig(GoCipher goCipher) {
        super(TYPE);
        this.goCipher = goCipher;
    }

    public P4MaterialConfig(String serverAndPort, String view, GoCipher goCipher) {
        this(goCipher);
        bombIfNull(serverAndPort, "null serverAndPort");
        this.serverAndPort = serverAndPort;
        setView(view);
    }

    public P4MaterialConfig(String serverAndPort, String view) {
        this(serverAndPort, view, new GoCipher());
    }

    public P4MaterialConfig(String url, String view, String userName) {
        this(url, view);
        this.userName = userName;
    }

    public P4MaterialConfig(String serverAndPort, String userName, String password, Boolean useTickets, String viewStr, GoCipher goCipher, CaseInsensitiveString name,
                            boolean autoUpdate, Filter filter, boolean invertFilter, String folder) {
        super(name, filter, invertFilter, folder, autoUpdate, TYPE, new ConfigErrors());
        this.serverAndPort = serverAndPort;
        this.goCipher = goCipher;
        setPassword(password);
        this.userName = userName;
        this.useTickets = useTickets;
        setView(viewStr);
    }

    //for tests only
    protected P4MaterialConfig(String serverAndPort, String password, String encryptedPassword, GoCipher goCipher) {
        this(goCipher);
        this.password = password;
        this.encryptedPassword = encryptedPassword;
        this.serverAndPort = serverAndPort;
    }

    public GoCipher getGoCipher() {
        return goCipher;
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, serverAndPort);
        parameters.put(ScmMaterialConfig.USERNAME, userName);
        parameters.put("view", view.getValue());
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        appendCriteria(parameters);
    }

    public String getServerAndPort() {
        return serverAndPort;
    }

    public void setServerAndPort(String serverAndPort) {
        this.serverAndPort = serverAndPort;
    }


    public String getView() {
        return view == null ? null : view.getValue();
    }

    //USED IN RSPEC TESTS
    public P4MaterialViewConfig getP4MaterialView() {
        return view;
    }

    //USED IN RSPEC TESTS
    public void setP4MaterialView(P4MaterialViewConfig view) {
        resetCachedIdentityAttributes();
        this.view = view;
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getUrl() {
        return serverAndPort;
    }

    @Override
    public void setUrl(String serverAndPort) {
        this.serverAndPort = serverAndPort;
    }

    @Override
    protected UrlArgument getUrlArgument() {
        return new UrlArgument(serverAndPort);
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s, View: %s, Username: %s", serverAndPort, view.getValue(), userName);
    }

    @Override
    public String getUserName() {
        return userName;
    }
        public void setUserName(String userName) {
                this.userName = userName;
            }

    @Override
    public String getPassword() {
        return currentPassword();
    }

    @Override
    public void setPassword(String password) {
        resetPassword(password);
    }

    public void setCleartextPassword(String password) {
        this.password = password;
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

        P4MaterialConfig that = (P4MaterialConfig) o;

        if (serverAndPort != null ? !serverAndPort.equals(that.serverAndPort) : that.serverAndPort != null) {
            return false;
        }
        if (useTickets != null ? !useTickets.equals(that.useTickets) : that.useTickets != null) {
            return false;
        }
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }
        if (view != null ? !view.equals(that.view) : that.view != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (serverAndPort != null ? serverAndPort.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (useTickets != null ? useTickets.hashCode() : 0);
        result = 31 * result + (view != null ? view.hashCode() : 0);
        return result;
    }

    @Override
    public void validateConcreteScmMaterial() {
        if (getView() == null || getView().trim().isEmpty()) {
            errors.add(VIEW, "P4 view cannot be empty.");
        }
        if (StringUtil.isBlank(getServerAndPort())) {
            errors.add(SERVER_AND_PORT, "P4 port cannot be empty.");
        }
        if (isNotEmpty(this.password) && isNotEmpty(this.encryptedPassword)){
            addError("password", "You may only specify `password` or `encrypted_password`, not both!");
            addError("encryptedPassword", "You may only specify `password` or `encrypted_password`, not both!");
        }
        if(isNotEmpty(this.encryptedPassword)) {
            try{
                currentPassword();
            }catch (Exception e) {
                addError("encryptedPassword", format("Encrypted password value for P4 material with serverAndPort '%s' is invalid. This usually happens when the cipher text is modified to have an invalid value.",
                        this.getServerAndPort()));
            }
        }
    }

    @Override
    protected String getLocation() {
        return getServerAndPort();
    }

    @Override
    public String getTypeForDisplay() {
        return "Perforce";
    }

    @Override public String toString() {
        return "P4MaterialConfig{" +
                "serverAndPort='" + serverAndPort + '\'' +
                ", userName='" + userName + '\'' +
                ", view=" + view.getValue() +
                '}';
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        if (map.containsKey(SERVER_AND_PORT)) {
            this.serverAndPort = (String) map.get(SERVER_AND_PORT);
        }
        if (map.containsKey(VIEW)) {
            setView((String) map.get(VIEW));
        }
        if (map.containsKey(USERNAME)) {
            this.userName = (String) map.get(USERNAME);
        }
        if (map.containsKey(PASSWORD_CHANGED) && "1".equals(map.get(PASSWORD_CHANGED))) {
            String passwordToSet = (String) map.get(PASSWORD);
            resetPassword(passwordToSet);
        }
        setUseTickets("true".equals(map.get(USE_TICKETS)));
    }

    public void setView(String viewStr) {
        this.view = new P4MaterialViewConfig(viewStr);
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
        this.password = null;
    }

    @PostConstruct
    public void ensureEncrypted() {
        setPasswordIfNotBlank(password);
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

    private String p4RepoId() {
        return hasUser() ? userName + "@" + serverAndPort : serverAndPort;
    }

    private boolean hasUser() {
        return userName != null && !userName.trim().isEmpty();
    }

    public boolean getUseTickets() {
        return this.useTickets;
    }

    public void setUseTickets(boolean useTickets) {
        this.useTickets = useTickets;
    }

    @Override
    public String getFolder() {
        return folder;
    }
}
