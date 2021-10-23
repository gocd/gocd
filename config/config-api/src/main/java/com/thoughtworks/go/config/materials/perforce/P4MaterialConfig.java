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
package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@ConfigTag(value = "p4", label = "Perforce")
public class P4MaterialConfig extends ScmMaterialConfig implements ParamsAttributeAware, PasswordAwareMaterial {
    @ConfigAttribute(value = "port")
    private String serverAndPort;

    @ConfigAttribute(value = "useTickets")
    private Boolean useTickets = false;

    @ConfigSubtag(optional = false)
    private P4MaterialViewConfig view;

    public static final String TYPE = "P4Material";
    public static final String USERNAME = "userName";
    public static final String VIEW = "view";
    public static final String SERVER_AND_PORT = "serverAndPort";
    public static final String USE_TICKETS = "useTickets";

    public P4MaterialConfig() {
        super(TYPE);
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
    public String getLongDescription() {
        return String.format("URL: %s, View: %s, Username: %s", serverAndPort, view.getValue(), userName);
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
        if (view != null ? !view.equals(that.view) : that.view != null) {
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
        result = 31 * result + (serverAndPort != null ? serverAndPort.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (useTickets != null ? useTickets.hashCode() : 0);
        result = 31 * result + (view != null ? view.hashCode() : 0);
        return result;
    }

    @Override
    public void validateConcreteScmMaterial(ValidationContext validationContext) {
        if (getView() == null || getView().trim().isEmpty()) {
            errors.add(VIEW, "P4 view cannot be empty.");
        }
        if (StringUtils.isBlank(getServerAndPort())) {
            errors.add(SERVER_AND_PORT, "P4 port cannot be empty.");
        }

        validateEncryptedPassword();
    }

    @Override
    protected String getLocation() {
        return getServerAndPort();
    }

    @Override
    public String getUriForDisplay() {
        return new UrlArgument(serverAndPort).forDisplay();
    }

    @Override
    public String getTypeForDisplay() {
        return "Perforce";
    }

    @Override
    public String toString() {
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
