/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.materials.svn;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class SvnMaterialConfigTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldSetConfigAttributes() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("", "", "", false);

        Map<String, String> map = new HashMap<String, String>();
        map.put(SvnMaterialConfig.URL, "url");
        map.put(SvnMaterialConfig.USERNAME, "username");
        map.put(SvnMaterialConfig.CHECK_EXTERNALS, "true");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, "0");
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        svnMaterialConfig.setConfigAttributes(map);

        assertThat(svnMaterialConfig.getUrl(), is("url"));
        assertThat(svnMaterialConfig.getUserName(), is("username"));
        assertThat(svnMaterialConfig.isCheckExternals(), is(true));
        assertThat(svnMaterialConfig.getFolder(), is("folder"));
        assertThat(svnMaterialConfig.getName(), is(new CaseInsensitiveString("material-name")));
        assertThat(svnMaterialConfig.isAutoUpdate(), is(false));
        assertThat(svnMaterialConfig.filter(), is(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help"))));
    }

    @Test
    public void validate_shouldEnsureUrlIsNotBlank() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("", "", "", false);
        svnMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.URL), is("URL cannot be blank"));
    }

    @Test
    public void validate_shouldEnsureMaterialNameIsValid() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("/foo", "", "", false);
        svnMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.MATERIAL_NAME), is(nullValue()));

        svnMaterialConfig.setName(new CaseInsensitiveString(".bad-name-with-dot"));
        svnMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.MATERIAL_NAME),
                is("Invalid material name '.bad-name-with-dot'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void validate_shouldEnsureDestFilePathIsValid() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("/foo", "", "", false);
        svnMaterialConfig.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "../a"));
        svnMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.FOLDER), is("Dest folder '../a' is not valid. It must be a sub-directory of the working folder."));
    }

    @Test
    public void shouldThrowErrorsIfBothPasswordAndEncryptedPasswordAreProvided() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig(new UrlArgument("foo/bar"), "password", "encryptedPassword", new GoCipher(), null, false, "folder");
        svnMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(svnMaterialConfig.errors().on("password"), is("You may only specify `password` or `encrypted_password`, not both!"));
        assertThat(svnMaterialConfig.errors().on("encryptedPassword"), is("You may only specify `password` or `encrypted_password`, not both!"));
    }

    @Test
    public void shouldValidateWhetherTheEncryptedPasswordIsCorrect() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig(new UrlArgument("foo/bar"), "", "encryptedPassword", new GoCipher(), null, false, "folder");
        svnMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(svnMaterialConfig.errors().on("encryptedPassword"), is("Encrypted password value for svn material with url 'foo/bar' is invalid. This usually happens when the cipher text is modified to have an invalid value."));
    }

    @Test
    public void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        SvnMaterialConfig svnMaterial = new SvnMaterialConfig("", "", "notSoSecret", false);
        Map<String, String> map = new HashMap<String, String>();
        map.put(SvnMaterialConfig.PASSWORD, "secret");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "1");

        svnMaterial.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(svnMaterial, "password"), is(nullValue()));
        assertThat(svnMaterial.getPassword(), is("secret"));
        assertThat(svnMaterial.getEncryptedPassword(), Is.is(new GoCipher().encrypt("secret")));

        //Dont change
        map.put(SvnMaterialConfig.PASSWORD, "Hehehe");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "0");
        svnMaterial.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(svnMaterial, "password"), is(nullValue()));
        assertThat(svnMaterial.getPassword(), is("secret"));
        assertThat(svnMaterial.getEncryptedPassword(), is(new GoCipher().encrypt("secret")));

        map.put(SvnMaterialConfig.PASSWORD, "");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "1");
        svnMaterial.setConfigAttributes(map);

        assertThat(svnMaterial.getPassword(), is(nullValue()));
        assertThat(svnMaterial.getEncryptedPassword(), is(nullValue()));
    }

    @Test
    public void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        SvnMaterialConfig config = new SvnMaterialConfig();
        config.setUrl(url);

        assertThat(config.getUrl(), is(url));
    }

    @Test
    public void shouldReturnNullIfUrlForMaterialNotSpecified() {
        SvnMaterialConfig config = new SvnMaterialConfig();

        assertNull(config.getUrl());
    }

    @Test
    public void shouldHandleNullWhenSettingUrlForAMaterial() {
        SvnMaterialConfig config = new SvnMaterialConfig();

        config.setUrl(null);

        assertNull(config.getUrl());
    }
}
