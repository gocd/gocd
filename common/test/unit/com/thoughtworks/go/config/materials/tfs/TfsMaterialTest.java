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

package com.thoughtworks.go.config.materials.tfs;

import com.thoughtworks.go.config.PasswordEncrypter;
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.domain.materials.tfs.TfsCommand;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.DataStructureUtils;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.command.UrlArgument;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TfsMaterialTest {
    private TempFiles tempFiles;
    private TfsMaterial tfsMaterialFirstCollectionFirstProject;
    private TfsMaterial tfsMaterialFirstCollectionSecondProject;
    private final String DOMAIN = "domain";
    private final String USERNAME = "username";
    private final String PASSWORD = "password";
    private final String TFS_FIRST_COLLECTION_URL = "http://some.tfs.repo.local";
    private final String TFS_FIRST_PROJECT = "$/first_project";
    private final String TFS_SECOND_PROJECT = "$/second_project";

    @Before
    public void setUp() {
        GoCipher goCipher = mock(GoCipher.class);
        tempFiles = new TempFiles();
        tfsMaterialFirstCollectionFirstProject = new TfsMaterial(goCipher, new UrlArgument(TFS_FIRST_COLLECTION_URL), USERNAME, DOMAIN, PASSWORD, TFS_FIRST_PROJECT);
        tfsMaterialFirstCollectionSecondProject = new TfsMaterial(goCipher, new UrlArgument(TFS_FIRST_COLLECTION_URL), USERNAME, DOMAIN, PASSWORD, TFS_SECOND_PROJECT);
    }

    @After
    public void tearDown() {
        tempFiles.cleanUp();
    }

    @Test
    public void shouldShowLatestModification() {
        File dir = tempFiles.createUniqueFolder("tfs-dir");
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        TfsMaterial spy = spy(tfsMaterialFirstCollectionSecondProject);
        TfsCommand tfsCommand = mock(TfsCommand.class);
        when(tfsCommand.latestModification(dir)).thenReturn(new ArrayList<>());
        doReturn(tfsCommand).when(spy).tfs(execCtx);

        List<Modification> actual = spy.latestModification(dir, execCtx);

        assertThat(actual, is(new ArrayList<Modification>()));
        verify(tfsCommand).latestModification(dir);
    }

    @Test
    public void shouldLoadAllModificationsSinceAGivenRevision() {
        File dir = tempFiles.createUniqueFolder("tfs-dir");
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        TfsMaterial spy = spy(tfsMaterialFirstCollectionFirstProject);
        TfsCommand tfsCommand = mock(TfsCommand.class);
        when(tfsCommand.modificationsSince(dir, new StringRevision("5"))).thenReturn(new ArrayList<>());
        doReturn(tfsCommand).when(spy).tfs(execCtx);

        List<Modification> actual = spy.modificationsSince(dir, new StringRevision("5"), execCtx);

        assertThat(actual, is(new ArrayList<Modification>()));
        verify(tfsCommand).modificationsSince(dir, new StringRevision("5"));
    }

    @Test
    public void shouldInjectAllRelevantAttributesInSqlCriteriaMap() {
        TfsMaterial tfsMaterial = new TfsMaterial(new GoCipher(), new UrlArgument("my-url"), "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getSqlCriteria(), Is.is(DataStructureUtils.m(
                AbstractMaterial.SQL_CRITERIA_TYPE, (Object) "TfsMaterial",
                "url", "my-url",
                "username", "loser",
                "projectPath", "/dev/null", "domain", DOMAIN)));
    }

    @Test
    public void shouldInjectAllRelevantAttributesInAttributeMap() {
        TfsMaterial tfsMaterial = new TfsMaterial(new GoCipher(), new UrlArgument("my-url"), "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getAttributesForXml(), is(m(
                AbstractMaterial.SQL_CRITERIA_TYPE, (Object) "TfsMaterial",
                "url", "my-url",
                "username", "loser",
                "projectPath", "/dev/null", "domain", DOMAIN)));
    }

    @Test
    public void shouldReturnUrlForCommandLine_asUrl_IfSet() {
        TfsMaterial tfsMaterial = new TfsMaterial(new GoCipher(), new UrlArgument("http://foo:bar@my-url.com"), "loser", DOMAIN, "foo_bar_baz", "/dev/null"
        );
        assertThat(tfsMaterial.getUrl(), is("http://foo:bar@my-url.com"));

        tfsMaterial = new TfsMaterial(new GoCipher(), null, "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getUrl(), is(nullValue()));
    }

    @Test
    public void shouldReturnUrlForCommandLine_asLocation_IfSet() {
        TfsMaterial tfsMaterial = new TfsMaterial(new GoCipher(), new UrlArgument("http://foo:bar@my-url.com"), "loser", DOMAIN, "foo_bar_baz", "/dev/null"
        );
        assertThat(tfsMaterial.getLocation(), is("http://foo:******@my-url.com"));

        tfsMaterial = new TfsMaterial(new GoCipher(), null, "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getLocation(), is(nullValue()));
    }

    @Test
    public void shouldEncryptTfsPasswordAndMarkPasswordAsNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");

        TfsMaterial tfsMaterial = new TfsMaterial(mockGoCipher, new UrlArgument("/foo"), "username", DOMAIN, "password", "");
        tfsMaterial.ensureEncrypted();

        assertThat(tfsMaterial.getPassword(), is(nullValue()));
        assertThat(tfsMaterial.getEncryptedPassword(), is("encrypted"));
    }

    @Test
    public void shouldDecryptTfsPassword() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        TfsMaterial tfsMaterial = new TfsMaterial(mockGoCipher, new UrlArgument("/foo"), "username", DOMAIN, null, "");

        ReflectionUtil.setField(tfsMaterial, "encryptedPassword", "encrypted");

        tfsMaterial.ensureEncrypted();
        assertThat(tfsMaterial.getPassword(), is("password"));
    }

    @Test
    public void shouldNotDecryptPasswordIfPasswordIsNotNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        TfsMaterial material = new TfsMaterial(mockGoCipher, new UrlArgument("/foo"), "username", DOMAIN, "password", "");
        material.ensureEncrypted();
        when(mockGoCipher.encrypt("new_password")).thenReturn("new_encrypted");
        material.setPassword("new_password");
        when(mockGoCipher.decrypt("new_encrypted")).thenReturn("new_password");

        assertThat(material.getPassword(), is("new_password"));
    }

    @Test
    public void shouldErrorOutIfDecryptionFails() throws InvalidCipherTextException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String fakeCipherText = "fake cipher text";
        when(mockGoCipher.decrypt(fakeCipherText)).thenThrow(new InvalidCipherTextException("exception"));
        TfsMaterial material = new TfsMaterial(mockGoCipher, new UrlArgument("/foo"), "username", DOMAIN, "password", "");
        ReflectionUtil.setField(material, "encryptedPassword", fakeCipherText);
        try {
            material.getPassword();
            fail("Should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Could not decrypt the password to get the real password"));
        }
    }

    @Test
    public void shouldErrorOutIfEncryptionFails() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenThrow(new InvalidCipherTextException("exception"));
        try {
            new TfsMaterial(mockGoCipher, new UrlArgument("/foo"), "username", DOMAIN, "password", "");
            fail("Should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Password encryption failed. Please verify your cipher key."));
        }
    }

    @Test
    public void shouldBePasswordAware() {
        assertThat(PasswordAwareMaterial.class.isAssignableFrom(TfsMaterial.class), is(true));
    }

    @Test
    public void shouldBePasswordEncrypter() {
        assertThat(PasswordEncrypter.class.isAssignableFrom(TfsMaterial.class), is(true));
    }

    @Test
    public void shouldKnowItsType() {
        assertThat(tfsMaterialFirstCollectionFirstProject.getTypeForDisplay(), is("Tfs"));
    }

    @Test
    public void shouldCheckConnection() {
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        TfsCommand tfsCommand = mock(TfsCommand.class);
        doNothing().when(tfsCommand).checkConnection();
        TfsMaterial spy = spy(tfsMaterialFirstCollectionFirstProject);
        doReturn(tfsCommand).when(spy).tfs(execCtx);
        assertThat(spy.checkConnection(execCtx), Is.is(ValidationBean.valid()));
        verify(tfsCommand, times(1)).checkConnection();
    }

    @Test
    public void shouldGetLongDescriptionForMaterial(){
        TfsMaterial material = new TfsMaterial(new GoCipher(), new UrlArgument("http://url/"),"user", "domain", "password", "$project/path/" );
        assertThat(material.getLongDescription(), is("URL: http://url/, Username: user, Domain: domain, ProjectPath: $project/path/"));
    }

    @Test
    public void shouldCopyOverPasswordWhenConvertingToConfig() throws Exception {
        TfsMaterial material = new TfsMaterial(new GoCipher(), new UrlArgument("http://url/"),"user", "domain", "password", "$project/path/" );

        TfsMaterialConfig config = (TfsMaterialConfig) material.config();

        assertThat(config.getPassword(), is("password"));
        assertThat(config.getEncryptedPassword(), is(Matchers.not(Matchers.nullValue())));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        TfsMaterial material = new TfsMaterial(new GoCipher(), new UrlArgument("http://username:password@tfsrepo.com"), "username", "domain", "password", "$project/path/");
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type"), is("tfs"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("tfs-configuration");
        assertThat(configuration.get("url"), is("http://username:password@tfsrepo.com"));
        assertThat(configuration.get("domain"), is("domain"));
        assertThat(configuration.get("username"), is("username"));
        assertThat(configuration.get("password"), is("password"));
        assertThat(configuration.get("project-path"), is("$project/path/"));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        TfsMaterial material = new TfsMaterial(new GoCipher(), new UrlArgument("http://username:password@tfsrepo.com"), "username", "domain", "password", "$project/path/");
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type"), is("tfs"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("tfs-configuration");
        assertThat(configuration.get("url"), is("http://username:******@tfsrepo.com"));
        assertThat(configuration.get("domain"), is("domain"));
        assertThat(configuration.get("username"), is("username"));
        assertThat(configuration.get("password"), is(nullValue()));
        assertThat(configuration.get("project-path"), is("$project/path/"));
    }
}
