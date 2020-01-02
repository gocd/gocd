/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.util.command.UrlArgument;

import java.util.Map;

@ConfigTag(value = "svn", label = "Subversion")
public class SvnMaterialConfig extends ScmMaterialConfig implements ParamsAttributeAware, PasswordAwareMaterial {
    @ConfigAttribute(value = ScmMaterialConfig.URL)
    private UrlArgument url;

    @ConfigAttribute(value = "checkexternals", allowNull = true, label = "Check externals")
    private boolean checkExternals;

    public static final String URL = ScmMaterialConfig.URL;
    public static final String USERNAME = "userName";
    public static final String CHECK_EXTERNALS = "checkExternals";
    public static final String TYPE = "SvnMaterial";

    public SvnMaterialConfig() {
        super(TYPE);
    }

    @Override
    public boolean isCheckExternals() {
        return checkExternals;
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
        return String.format("URL: %s, Username: %s, CheckExternals: %s", url.forDisplay(), userName, checkExternals);
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
        validateMaterialUrl(this.url, validationContext);
        validateEncryptedPassword();
    }

    @Override
    protected void appendCriteria(Map parameters) {
        parameters.put(ScmMaterialConfig.URL, url.originalArgument());
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


    @Override
    public String toString() {
        return "SvnMaterialConfig{" +
                "url=" + url +
                ", userName='" + userName + '\'' +
                ", checkExternals=" + checkExternals +
                '}';
    }
}
