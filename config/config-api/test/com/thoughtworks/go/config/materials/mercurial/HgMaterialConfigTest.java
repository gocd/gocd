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

package com.thoughtworks.go.config.materials.mercurial;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HgMaterialConfigTest {
    @Test
    public void shouldSetConfigAttributes() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("", null);

        Map<String, String> map = new HashMap<String, String>();
        map.put(HgMaterialConfig.URL, "url");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, "0");
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        hgMaterialConfig.setConfigAttributes(map);

        assertThat(hgMaterialConfig.getUrl(), is("url"));
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getName(), is(new CaseInsensitiveString("material-name")));
        assertThat(hgMaterialConfig.isAutoUpdate(), is(false));
        assertThat(hgMaterialConfig.filter(), is(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help"))));
    }

    @Test
    public void validate_shouldEnsureUrlIsNotBlank() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("", null);
        hgMaterialConfig.validate(new ValidationContext(null));
        assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL), is("URL cannot be blank"));
    }

    @Test
    public void shouldReturnIfAttributeMapIsNull() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("", null);

        hgMaterialConfig.setConfigAttributes(null);

        assertThat(hgMaterialConfig, is(new HgMaterialConfig("", null)));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        HgMaterialConfig material = new HgMaterialConfig("http://username:password@hgrepo.com", null);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat((String) attributes.get("type"), is("mercurial"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("mercurial-configuration");
        assertThat((String) configuration.get("url"), is("http://username:password@hgrepo.com"));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        HgMaterialConfig material = new HgMaterialConfig("http://username:password@hgrepo.com", null);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat((String) attributes.get("type"), is("mercurial"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("mercurial-configuration");
        assertThat((String) configuration.get("url"), is("http://username:******@hgrepo.com"));
    }
}
