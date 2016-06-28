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

package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;

import java.util.Map;

@ConfigTag(value = "hg", label = "Mercurial")
public class HgMaterialConfig extends ScmMaterialConfig implements ParamsAttributeAware {
    @ConfigAttribute(value = "url")
    private HgUrlArgument url;

    public static final String TYPE = "HgMaterial";
    public static final String URL = "url";

    public HgMaterialConfig() {
        super(TYPE);
    }

    public HgMaterialConfig(String url, String folder) {
        this();
        this.url = new HgUrlArgument(url);
        this.folder = folder;
    }

    public HgMaterialConfig(HgUrlArgument url, boolean autoUpdate, Filter filter, boolean invertFilter, String folder, CaseInsensitiveString name) {
        super(name, filter, invertFilter, folder, autoUpdate, TYPE, new ConfigErrors());
        this.url = url;
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.forCommandline());
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("url", url);
    }

    @Override
    public String getUserName() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getEncryptedPassword() {
        return null;
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
            this.url = new HgUrlArgument(url);
        }
    }

    @Override
    public UrlArgument getUrlArgument() {
        return url;
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s", url.forDisplay());
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

        HgMaterialConfig that = (HgMaterialConfig) o;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    @Override
    public void validateConcreteScmMaterial() {
        if (url == null || StringUtil.isBlank(url.forDisplay())) {
            errors().add(URL, "URL cannot be blank");
        }
    }

    @Override
    protected String getLocation() {
        return getUrlArgument().forDisplay();
    }

    @Override
    public String getTypeForDisplay() {
        return "Mercurial";
    }

    @Override
    public String getShortRevision(String revision) {
        if (revision == null) {
            return null;
        }
        if (revision.length() < 12) {
            return revision;
        }
        return revision.substring(0, 12);
    }

    @Override public String toString() {
        return "HgMaterialConfig{" +
                "url=" + url +
                '}';
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        super.setConfigAttributes(attributes);
        Map map = (Map) attributes;
        if (map.containsKey(URL)) {
            this.url = new HgUrlArgument((String) map.get(URL));
        }
    }
}
