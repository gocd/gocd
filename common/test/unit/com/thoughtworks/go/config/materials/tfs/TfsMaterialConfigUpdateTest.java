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

package com.thoughtworks.go.config.materials.tfs;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TfsMaterialConfigUpdateTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldSetConfigAttributes() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "some_domain", "passwd", "walk_this_path");

        Map<String, String> map = new HashMap<String, String>();
        map.put(ScmMaterialConfig.URL, "http://foo:8080/tfs/HelloWorld");
        map.put(ScmMaterialConfig.USERNAME, "boozer");
        map.put(ScmMaterialConfig.PASSWORD, "secret");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, "0");
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "my-tfs-material-name");
        map.put(TfsMaterialConfig.PROJECT_PATH, "/useless/project");
        map.put(TfsMaterialConfig.DOMAIN, "CORPORATE");

        tfsMaterialConfig.setConfigAttributes(map);
        TfsMaterialConfig newTfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://foo:8080/tfs/HelloWorld"), "boozer", "CORPORATE", "secret", "/useless/project");
        newTfsMaterialConfig.setName(new CaseInsensitiveString("my-tfs-material-name"));
        newTfsMaterialConfig.setFolder("folder");

        assertThat(tfsMaterialConfig, is(newTfsMaterialConfig));
        assertThat(tfsMaterialConfig.getPassword(), is("passwd"));
        assertThat(tfsMaterialConfig.isAutoUpdate(), is(false));
        assertThat(tfsMaterialConfig.getDomain(), is("CORPORATE"));

        assertThat(tfsMaterialConfig.getName(), is(new CaseInsensitiveString("my-tfs-material-name")));
        assertThat(tfsMaterialConfig.filter(), is(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help"))));
    }

    @Test
    public void shouldDefaultDomainToEmptyStringWhenNothingIsSet() throws Exception {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(mock(GoCipher.class));
        assertThat(tfsMaterialConfig.getDomain(), is(""));
    }

    @Test
    public void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");
        Map<String, String> map = new HashMap<String, String>();
        map.put(TfsMaterialConfig.PASSWORD, "secret");
        map.put(TfsMaterialConfig.PASSWORD_CHANGED, "1");

        tfsMaterialConfig.setConfigAttributes(map);

        tfsMaterialConfig.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(tfsMaterialConfig, "password"), is(nullValue()));
        assertThat(tfsMaterialConfig.getPassword(), is("secret"));
        assertThat(tfsMaterialConfig.getEncryptedPassword(), is(new GoCipher().encrypt("secret")));

        //Dont change
        map.put(TfsMaterialConfig.PASSWORD, "Hehehe");
        map.put(TfsMaterialConfig.PASSWORD_CHANGED, "0");
        tfsMaterialConfig.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(tfsMaterialConfig, "password"), is(nullValue()));
        assertThat(tfsMaterialConfig.getPassword(), is("secret"));
        assertThat(tfsMaterialConfig.getEncryptedPassword(), is(new GoCipher().encrypt("secret")));

        map.put(TfsMaterialConfig.PASSWORD, "");
        map.put(TfsMaterialConfig.PASSWORD_CHANGED, "1");
        tfsMaterialConfig.setConfigAttributes(map);

        assertThat(tfsMaterialConfig.getPassword(), is(nullValue()));
        assertThat(tfsMaterialConfig.getEncryptedPassword(), is(nullValue()));

    }

    @Test
    public void validate_shouldEnsureMandatoryFieldsAreNotBlank() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument(""), "", "CORPORATE", "", "");
        tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.URL), is("URL cannot be blank"));
        assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.USERNAME), is("Username cannot be blank"));
        assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.PROJECT_PATH), is("Project Path cannot be blank"));
    }

    @Test
    public void validate_shouldEnsureMaterialNameIsValid() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");

        tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.MATERIAL_NAME), is(nullValue()));

        tfsMaterialConfig.setName(new CaseInsensitiveString(".bad-name-with-dot"));
        tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.MATERIAL_NAME),
                is("Invalid material name '.bad-name-with-dot'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void validate_shouldEnsureDestFilePathIsValid() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");
        tfsMaterialConfig.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "../a"));
        tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.FOLDER), is("Dest folder '../a' is not valid. It must be a sub-directory of the working folder."));
    }

    @Test
    public void shouldThrowErrorsIfBothPasswordAndEncryptedPasswordAreProvided() {
        TfsMaterialConfig materialConfig = new TfsMaterialConfig(new UrlArgument("foo/bar"), "password", "encryptedPassword", new GoCipher());
        materialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(materialConfig.errors().on("password"), is("You may only specify `password` or `encrypted_password`, not both!"));
        assertThat(materialConfig.errors().on("encryptedPassword"), is("You may only specify `password` or `encrypted_password`, not both!"));
    }

    @Test
    public void shouldValidateWhetherTheEncryptedPasswordIsCorrect() {
        TfsMaterialConfig materialConfig = new TfsMaterialConfig(new UrlArgument("foo/bar"), "", "encryptedPassword", new GoCipher());
        materialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(materialConfig.errors().on("encryptedPassword"), is("Encrypted password value for TFS material with url 'foo/bar' is invalid. This usually happens when the cipher text is modified to have an invalid value."));
    }

    @Test
     public void shouldEncryptTfsPasswordAndMarkPasswordAsNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");

        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "password", "walk_this_path");
        materialConfig.ensureEncrypted();

        assertThat(materialConfig.getPassword(), is(nullValue()));
        assertThat(materialConfig.getEncryptedPassword(), is("encrypted"));
     }

    @Test
     public void shouldDecryptTfsPassword() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "secret", "walk_this_path");
        ReflectionUtil.setField(materialConfig, "encryptedPassword", "encrypted");

        materialConfig.ensureEncrypted();
        assertThat(materialConfig.getPassword(), is("password"));
    }

    @Test
    public void shouldNotDecryptTfsPasswordIfPasswordIsNotNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "password", "walk_this_path");
        materialConfig.ensureEncrypted();
        when(mockGoCipher.encrypt("new_password")).thenReturn("new_encrypted");
        materialConfig.setPassword("new_password");
        when(mockGoCipher.decrypt("new_encrypted")).thenReturn("new_password");

        assertThat(materialConfig.getPassword(), is("new_password"));
    }

    @Test
    public void shouldErrorOutIfDecryptionFails() throws InvalidCipherTextException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String fakeCipherText = "fake cipher text";
        when(mockGoCipher.decrypt(fakeCipherText)).thenThrow(new InvalidCipherTextException("exception"));
        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");
        ReflectionUtil.setField(materialConfig, "encryptedPassword", fakeCipherText);
        try {
            materialConfig.getPassword();
            fail("Should have thrown up");
        }
        catch (Exception e) {
            assertThat(e.getMessage(), is("Could not decrypt the password to get the real password"));
        }
    }

    @Test
    public void shouldErrorOutIfEncryptionFails() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenThrow(new InvalidCipherTextException("exception"));
        try {
            new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "password", "walk_this_path");
            fail("Should have thrown up");
        }
        catch (Exception e) {
            assertThat(e.getMessage(), is("Password encryption failed. Please verify your cipher key."));
        }
    }

    @Test
    public void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        TfsMaterialConfig config = new TfsMaterialConfig();

        config.setUrl(url);

        assertThat(config.getUrl(), is(url));
    }

    @Test
    public void shouldReturnNullIfUrlForMaterialNotSpecified() {
        TfsMaterialConfig config = new TfsMaterialConfig();

        assertNull(config.getUrl());
    }

    @Test
    public void shouldHandleNullWhenSettingUrlForAMaterial() {
        TfsMaterialConfig config = new TfsMaterialConfig();

        config.setUrl(null);

        assertNull(config.getUrl());
    }
}
