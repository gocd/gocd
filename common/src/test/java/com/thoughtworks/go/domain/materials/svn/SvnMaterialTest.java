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
package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.JsonUtils.from;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class SvnMaterialTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Subversion subversion;

    private SvnMaterial svnMaterial;
    private static final String URL = "svn://something";
    private SubversionRevision revision = new SubversionRevision("1");
    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

    @BeforeEach
    void setUp() throws IOException {
        temporaryFolder.create();
        subversion = mock(Subversion.class);

        when(subversion.getUrl()).thenReturn(new UrlArgument(URL));
        when(subversion.getPassword()).thenReturn("");
        when(subversion.getUserName()).thenReturn("");
        when(subversion.isCheckExternals()).thenReturn(false);

        svnMaterial = SvnMaterial.createSvnMaterialWithMock(subversion);
        svnMaterial.setUrl(URL);
    }

    @AfterEach
    void tearDown() {
        temporaryFolder.delete();
    }

    private File createSvnWorkingCopy(boolean withDotSvnFolder) throws IOException {
        File folder = temporaryFolder.newFolder("testSvnWorkingCopy");
        if (withDotSvnFolder) {
            File dotSvnFolder = new File(folder, ".svn");
            dotSvnFolder.mkdir();
        }
        return folder;
    }

    @Test
    void shouldNotDisplayPasswordInStringRepresentation() {
        SvnMaterial svn = new SvnMaterial("my-url", "user", "loser", false);
        assertThat(svn.toString()).doesNotContain("loser");

        svn = new SvnMaterial("https://user:loser@foo.bar/baz?quux=bang", "user", "loser", false);
        assertThat(svn.toString()).doesNotContain("loser");
    }

    @Test
    void shouldCheckoutWhenFolderDoesNotExist() {
        final File workingCopy = new File("xyz");

        updateMaterial(svnMaterial, revision, workingCopy);

        verify(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
    }

    @Test
    void shouldLogRepoInfoToConsoleOutWithOutFolder() throws Exception {
        final File workingCopy = new File("xyz");

        updateMaterial(svnMaterial, revision, workingCopy);
        String stdout = outputStreamConsumer.getStdOut();
        assertThat(stdout).contains(String.format("Start updating %s at revision %s from %s", "files", revision.getRevision(),
                svnMaterial.getUrl()));

        verify(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
    }

    @Test
    void shouldCheckoutForInvalidSvnWorkingCopy() throws IOException {
        final File workingCopy = createSvnWorkingCopy(false);

        updateMaterial(svnMaterial, revision, workingCopy);

        assertThat(workingCopy.exists()).isFalse();
        verify(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
    }

    private void updateMaterial(SvnMaterial svnMaterial, SubversionRevision revision, File workingCopy) {
        svnMaterial.updateTo(outputStreamConsumer, workingCopy, new RevisionContext(revision), new TestSubprocessExecutionContext());
    }

    @Test
    void shouldCheckoutIfSvnRepositoryChanged() throws IOException {
        final File workingCopy = createSvnWorkingCopy(true);

        when(subversion.workingRepositoryUrl(workingCopy)).thenReturn("new url");

        updateMaterial(svnMaterial, revision, workingCopy);
        assertThat(workingCopy.exists()).isFalse();
        verify(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
    }

    @Test
    void shouldUpdateForValidSvnWorkingCopy() throws IOException {
        final File workingCopy = createSvnWorkingCopy(true);

        when(subversion.workingRepositoryUrl(workingCopy)).thenReturn(URL);

        updateMaterial(svnMaterial, revision, workingCopy);

        verify(subversion).cleanupAndRevert(outputStreamConsumer, workingCopy);
        verify(subversion).updateTo(outputStreamConsumer, workingCopy, revision);
    }

    @Test
    void shouldBeEqualWhenUrlSameForSvnMaterial() {
        final Material material1 = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        final Material material = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        assertComplementaryEquals(material1, material, true);

    }

    @Test
    void shouldNotBeEqualWhenUrlDifferent() {
        final Material material1 = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        final Material material2 = MaterialsMother.defaultSvnMaterialsWithUrl("url2").get(0);
        assertComplementaryEquals(material1, material2, false);
    }

    @Test
    void shouldNotBeEqualWhenTypeDifferent() {
        final Material hgMaterial = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material nonHgMaterial = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        assertComplementaryEquals(hgMaterial, nonHgMaterial, false);
    }

    @Test
    void shouldNotBeEqualWhenAlternateFolderDifferent() {
        final SvnMaterial material1 = MaterialsMother.svnMaterial("url1");
        final SvnMaterial material2 = MaterialsMother.svnMaterial("url1");

        assertComplementaryEquals(material1, material2, true);

        material1.setFolder("foo");
        material2.setFolder(null);
        assertComplementaryEquals(material1, material2, false);

        material1.setFolder("foo");
        material2.setFolder("bar");
        assertComplementaryEquals(material1, material2, false);
    }

    @Test
    void shouldSerializeAndDeserializeCorrectly() throws Exception {
        final SvnMaterial material1 = MaterialsMother.svnMaterial("url1", "foo");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ObjectOutputStream serialized = new ObjectOutputStream(buf);
        serialized.writeObject(material1);
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
        assertThat(in.readObject()).isEqualTo(material1);
    }

    @Test
    void shouldReturnNotEqualsWhenUrlIsChanged() {
        SvnMaterial material = MaterialsMother.svnMaterial("A");

        SvnMaterial other = MaterialsMother.svnMaterial("B");
        assertThat(material).isNotEqualTo(other);
    }

    @Test
    void shouldReturnNotEqualsWhenUserNameIsChanged() {
        SvnMaterial material = MaterialsMother.svnMaterial("url", "svnDir", "userName", null, false, "*.txt");

        SvnMaterial other = MaterialsMother.svnMaterial("url", "svnDir", "userName1", null, false, "*.txt");
        assertThat(material).isNotEqualTo(other);
    }

    @Test
    void shouldReturnEqualsEvenIfPasswordsAreDifferent() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        material.setPassword("password");

        SvnMaterial other = MaterialsMother.svnMaterial();
        other.setPassword("password1");
        assertThat(material).isEqualTo(other);
    }

    @Test
    void shouldReturnNotEqualsWhenCheckExternalsIsChanged() {
        SvnMaterial material = MaterialsMother.svnMaterial("url", "svnDir", null, null, true, "*.txt");
        SvnMaterial other = MaterialsMother.svnMaterial("url", "svnDir", null, null, false, "*.txt");
        assertThat(material).isNotEqualTo(other);
    }

    @Test
    void shouldReturnEqualsWhenEverythingIsSame() {
        SvnMaterial material = MaterialsMother.svnMaterial("URL", "dummy-folder", "userName", "password", true, "*.doc");
        SvnMaterial other = MaterialsMother.svnMaterial("URL", "dummy-folder", "userName", "password", true, "*.doc");

        assertThat(other).isEqualTo(material);
    }

    /* TODO: *SBD* Move this test into SvnMaterialConfig test after mothers are moved. */
    @Test
    void shouldReturnEqualsWhenEverythingIsSameForSvnMaterialConfigs() {
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig();
        svnMaterialConfig.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.CHECK_EXTERNALS, String.valueOf(true)));
        svnMaterialConfig.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.USERNAME, "userName"));
        svnMaterialConfig.setPassword("password");
        svnMaterialConfig.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.URL, "URL"));


        SvnMaterialConfig other = MaterialConfigsMother.svnMaterialConfig();
        other.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.CHECK_EXTERNALS, String.valueOf(true)));
        other.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.USERNAME, "userName"));
        other.setPassword("password");
        other.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.URL, "URL"));

        assertThat(other).isEqualTo(svnMaterialConfig);
    }

    @Test
    void shouldBeAbleToConvertToJson() {
        SvnMaterial material = MaterialsMother.svnMaterial("url");
        Map<String, Object> json = new LinkedHashMap<>();
        material.toJson(json, revision);

        JsonValue jsonValue = from(json);
        assertThat(jsonValue.getString("scmType")).isEqualTo("Subversion");
        assertThat(new File(jsonValue.getString("location"))).isEqualTo(new File(material.getUrl()));
        assertThat(jsonValue.getString("action")).isEqualTo("Modified");
    }

    @Test
    void shouldAddTheForwardSlashAndApplyThePattern() {
        SvnMaterial material = MaterialsMother.svnMaterial();

        assertThat(material.matches("/a.doc", "a.doc")).isTrue();
        assertThat(material.matches("a.doc", "a.doc")).isFalse();
    }

    @Test
    void shouldApplyThePatternDirectly() {
        SvnMaterial material = MaterialsMother.svnMaterial();

        assertThat(material.matches("/a.doc", "/a.doc")).isTrue();
    }

    @Test
    void shouldGenerateSqlCriteriaMapInSpecificOrder() {
        SvnMaterial material = new SvnMaterial("url", "username", "password", true);
        Map<String, Object> map = material.getSqlCriteria();
        assertThat(map.size()).isEqualTo(4);
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey()).isEqualTo("type");
        assertThat(iter.next().getKey()).isEqualTo("url");
        assertThat(iter.next().getKey()).isEqualTo("username");
        assertThat(iter.next().getKey()).isEqualTo("checkExternals");
    }

    @Test
    void shouldGenerateFingerprintBasedOnSqlCriteria() {
        SvnMaterial one = new SvnMaterial("url", "username", "password", true);
        SvnMaterial two = new SvnMaterial("url", "username", "password", false);
        assertThat(one.getFingerprint()).isNotEqualTo(two.getFingerprint());
        assertThat(one.getFingerprint()).isEqualTo(DigestUtils.sha256Hex("type=SvnMaterial<|>url=url<|>username=username<|>checkExternals=true"));
    }

    @Test
    void shouldGeneratePipelineUniqueFingerprintBasedOnFingerprintAndDest() {
        SvnMaterial one = new SvnMaterial("url", "username", "password", true, "folder1");
        SvnMaterial two = new SvnMaterial("url", "username", "password", true, "folder2");
        assertThat(one.getPipelineUniqueFingerprint()).isNotEqualTo(two.getFingerprint());
        assertThat(one.getPipelineUniqueFingerprint()).isEqualTo(DigestUtils.sha256Hex("type=SvnMaterial<|>url=url<|>username=username<|>checkExternals=true<|>dest=folder1"));
    }

    @Test
    void shouldNotUsePasswordForEquality() {
        SvnMaterial svnBoozer = new SvnMaterial("foo.com", "loser", "boozer", true);
        SvnMaterial svnZooser = new SvnMaterial("foo.com", "loser", "zooser", true);
        assertThat(svnBoozer.hashCode()).isEqualTo(svnZooser.hashCode());
        assertThat(svnBoozer).isEqualTo(svnZooser);
    }

    @Test
    void shouldEncryptSvnPasswordAndMarkPasswordAsNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");

        SvnMaterial material = new SvnMaterial("/foo", "username", "password", false, mockGoCipher);
        material.ensureEncrypted();

        assertThat(material.getPassword()).isNull();
        assertThat(material.getEncryptedPassword()).isEqualTo("encrypted");
    }

    @Test
    void shouldDecryptSvnPassword() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        SvnMaterial material = new SvnMaterial("/foo", "username", null, false, mockGoCipher);
        ReflectionUtil.setField(material, "encryptedPassword", "encrypted");

        material.ensureEncrypted();
        assertThat(material.getPassword()).isEqualTo("password");
    }

    @Test
    void shouldNotDecryptSvnPasswordIfPasswordIsNotNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        SvnMaterial material = new SvnMaterial("/foo", "username", "password", false, mockGoCipher);
        material.ensureEncrypted();
        when(mockGoCipher.encrypt("new_password")).thenReturn("new_encrypted");
        material.setPassword("new_password");
        when(mockGoCipher.decrypt("new_encrypted")).thenReturn("new_password");

        assertThat(material.getPassword()).isEqualTo("new_password");
    }

    @Test
    void shouldErrorOutIfDecryptionFails() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String fakeCipherText = "fake cipher text";
        when(mockGoCipher.decrypt(fakeCipherText)).thenThrow(new CryptoException("exception"));
        SvnMaterial material = new SvnMaterial("/foo", "username", null, false, mockGoCipher);
        ReflectionUtil.setField(material, "encryptedPassword", fakeCipherText);
        try {
            material.getPassword();
            fail("Should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Could not decrypt the password to get the real password");
        }
    }

    @Test
    void shouldErrorOutIfEncryptionFails() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenThrow(new CryptoException("exception"));
        try {
            new SvnMaterial("/foo", "username", "password", false, mockGoCipher);
            fail("Should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Password encryption failed. Please verify your cipher key.");
        }
    }

    @Test
    void shouldGetLongDescriptionForMaterial() {
        SvnMaterial material = new SvnMaterial("http://url/", "user", "password", true, "folder");
        assertThat(material.getLongDescription()).isEqualTo("URL: http://url/, Username: user, CheckExternals: true");
    }

    @Test
    void shouldCopyOverPasswordWhenConvertingToConfig() {
        SvnMaterial material = new SvnMaterial("abc", "def", "ghi", false);
        SvnMaterialConfig config = (SvnMaterialConfig) material.config();

        assertThat(config.getEncryptedPassword()).isNotNull();
        assertThat(config.getPassword()).isEqualTo("ghi");
    }

    private void assertComplementaryEquals(Object o1, Object o2, boolean value) {
        assertThat(o1.equals(o2)).isEqualTo(value);
        assertThat(o2.equals(o1)).isEqualTo(value);
    }

    @Test
    void shouldGetAttributesWithSecureFields() {
        SvnMaterial material = new SvnMaterial("http://username:password@svnrepo.com", "user", "password", true);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type")).isEqualTo("svn");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("svn-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:password@svnrepo.com");
        assertThat(configuration.get("username")).isEqualTo("user");
        assertThat(configuration.get("password")).isEqualTo("password");
        assertThat(configuration.get("check-externals")).isEqualTo(true);
    }

    @Test
    void shouldGetAttributesWithoutSecureFields() {
        SvnMaterial material = new SvnMaterial("http://username:password@svnrepo.com", "user", "password", true);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type")).isEqualTo("svn");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("svn-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:******@svnrepo.com");
        assertThat(configuration.get("username")).isEqualTo("user");
        assertThat(configuration.get("password")).isNull();
        assertThat(configuration.get("check-externals")).isEqualTo(true);
    }

    @Nested
    class hasSecretParams {
        @Test
        void shouldBeTrueIfPasswordHasSecretParam() {
            SvnMaterial svnMaterial = new SvnMaterial("http://foo.com", null, "{{SECRET:[secret_config_id][lookup_password]}}", false);

            assertThat(svnMaterial.hasSecretParams()).isTrue();
        }

        @Test
        void shouldBeFalseIfPasswordDoesNotHaveSecretParams() {
            SvnMaterial svnMaterial = new SvnMaterial("http://foo.com", null, "password", false);

            assertThat(svnMaterial.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class getSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            SvnMaterial svnMaterial = new SvnMaterial("http://foo.com",
                    "username", "{{SECRET:[secret_config_id][lookup_pass]}}", false);

            assertThat(svnMaterial.getSecretParams())
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "lookup_pass"));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInMaterialUrlOrPassword() {
            SvnMaterial svnMaterial = new SvnMaterial("http://foo.com", null, "pass", false);

            assertThat(svnMaterial.getSecretParams())
                    .hasSize(0);
        }
    }

    @Nested
    class passwordForCommandLine {
        @Test
        void shouldReturnPasswordAsConfigured_IfNotDefinedAsSecretParam() {
            SvnMaterial svnMaterial = new SvnMaterial("url", null, "badger", false);

            assertThat(svnMaterial.passwordForCommandLine()).isEqualTo("badger");
        }

        @Test
        void shouldReturnAResolvedPassword_IfPasswordDefinedAsSecretParam() {
            SvnMaterial svnMaterial = new SvnMaterial("url", null, "{{SECRET:[secret_config_id][lookup_pass]}}", false);

            svnMaterial.getSecretParams().findFirst("lookup_pass").ifPresent(secretParam -> secretParam.setValue("resolved_password"));

            assertThat(svnMaterial.passwordForCommandLine()).isEqualTo("resolved_password");
        }

        @Test
        void shouldErrorOutWhenCalledOnAUnResolvedSecretParam_IfPasswordDefinedAsSecretParam() {
            SvnMaterial svnMaterial = new SvnMaterial("url", null, "{{SECRET:[secret_config_id][lookup_pass]}}", false);

            assertThatCode(svnMaterial::passwordForCommandLine)
                    .isInstanceOf(UnresolvedSecretParamException.class)
                    .hasMessageContaining("SecretParam 'lookup_pass' is used before it is resolved.");
        }
    }

    @Nested
    class setPassword {
        @Test
        void shouldParsePasswordString_IfDefinedAsSecretParam() {
            SvnMaterial svnMaterial = new SvnMaterial("url", null, "{{SECRET:[secret_config_id][lookup_pass]}}", false);

            assertThat(svnMaterial.getSecretParams())
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "lookup_pass"));
        }
    }
}
