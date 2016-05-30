/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.util.JsonUtils.from;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JMock.class)
public class SvnMaterialTest {
    private Mockery context = new ClassMockery();
    private Subversion subversion;

    private SvnMaterial svnMaterial;
    private static final String URL = "svn://something";
    SubversionRevision revision = new SubversionRevision("1");
    private final ArrayList<File> tempFiles = new ArrayList<File>();
    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

    @Before
    public void setUp() {
        subversion = context.mock(Subversion.class);
        context.checking(new Expectations() {
            {
                allowing(subversion).getUrl();
                will(returnValue(new UrlArgument(URL)));
                allowing(subversion).getPassword();
                will(returnValue(""));
                allowing(subversion).getUserName();
                will(returnValue(""));
                allowing(subversion).isCheckExternals();
                will(returnValue(false));
            }
        });
        svnMaterial = SvnMaterial.createSvnMaterialWithMock(subversion);
        svnMaterial.setUrl(URL);
    }

    @After
    public void tearDown() throws Exception {
        for (File tempFile : tempFiles) {
            tempFile.delete();
        }
    }

    private File createSvnWorkingCopy(boolean withDotSvnFolder) {
        File folder = TestFileUtil.createTempFolder("testSvnWorkingCopy");
        if (withDotSvnFolder) {
            File dotSvnFolder = new File(folder, ".svn");
            dotSvnFolder.mkdir();
            tempFiles.add(dotSvnFolder);
        }
        tempFiles.add(folder);
        return folder;
    }

    @Test
    public void shouldNotDisplayPasswordInStringRepresentation() {
        SvnMaterial svn = new SvnMaterial("my-url", "user", "loser", false);
        assertThat(svn.toString(), not(containsString("loser")));

        svn = new SvnMaterial("https://user:loser@foo.bar/baz?quux=bang", "user", "loser", false);
        assertThat(svn.toString(), not(containsString("loser")));
    }

    @Test
    public void shouldCheckoutWhenFolderDoesNotExist() {
        final File workingCopy = new File("xyz");
        context.checking(new Expectations() {
            {
                one(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
            }
        });
        updateMaterial(svnMaterial, revision, workingCopy);
    }

    @Test
    public void shouldLogRepoInfoToConsoleOutWithOutFolder() throws Exception {
        final File workingCopy = new File("xyz");

        context.checking(new Expectations() {
            {
                one(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
            }
        });
        updateMaterial(svnMaterial, revision, workingCopy);
        String stdout = outputStreamConsumer.getStdOut();
        assertThat(stdout, containsString(
                String.format("Start updating %s at revision %s from %s", "files", revision.getRevision(),
                        svnMaterial.getUrl())));
    }

    @Test
    public void shouldCheckoutForInvalidSvnWorkingCopy() {
        final File workingCopy = createSvnWorkingCopy(false);
        context.checking(new Expectations() {
            {
                one(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
            }
        });
        updateMaterial(svnMaterial, revision, workingCopy);
        assertThat(workingCopy.exists(), is(false));
    }

    private void updateMaterial(SvnMaterial svnMaterial, SubversionRevision revision, File workingCopy) {
        svnMaterial.updateTo(outputStreamConsumer, workingCopy, new RevisionContext(revision), new TestSubprocessExecutionContext());
    }

    @Test
    public void shouldCheckoutIfSvnRepositoryChanged() throws IOException {
        final File workingCopy = createSvnWorkingCopy(true);
        context.checking(new Expectations() {
            {
                one(subversion).workingRepositoryUrl(workingCopy);
                will(returnValue("new url"));
                one(subversion).checkoutTo(outputStreamConsumer, workingCopy, revision);
            }
        });
        updateMaterial(svnMaterial, revision, workingCopy);
        assertThat(workingCopy.exists(), is(false));
    }

    @Test
    public void shouldUpdateForValidSvnWorkingCopy() throws IOException {
        final File workingCopy = createSvnWorkingCopy(true);
        context.checking(new Expectations() {
            {
                one(subversion).workingRepositoryUrl(workingCopy);
                will(returnValue(URL));
                one(subversion).cleanupAndRevert(outputStreamConsumer, workingCopy);
                one(subversion).updateTo(outputStreamConsumer, workingCopy, revision);
            }
        });
        updateMaterial(svnMaterial, revision, workingCopy);
    }

    @Test
    public void shouldBeEqualWhenUrlSameForSvnMaterial() throws Exception {
        final Material material1 = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        final Material material = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        assertComplementaryEquals(material1, material, true);

    }

    @Test
    public void shouldNotBeEqualWhenUrlDifferent() throws Exception {
        final Material material1 = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        final Material material2 = MaterialsMother.defaultSvnMaterialsWithUrl("url2").get(0);
        assertComplementaryEquals(material1, material2, false);
    }

    @Test
    public void shouldNotBeEqualWhenTypeDifferent() throws Exception {
        final Material hgMaterial = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material nonHgMaterial = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        assertComplementaryEquals(hgMaterial, nonHgMaterial, false);
    }

    @Test
    public void shouldNotBeEqualWhenAlternateFolderDifferent() throws Exception {
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
    public void shouldSerializeAndDeserializeCorrectly() throws Exception {
        final SvnMaterial material1 = MaterialsMother.svnMaterial("url1", "foo");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ObjectOutputStream serialized = new ObjectOutputStream(buf);
        serialized.writeObject(material1);
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
        assertThat((SvnMaterial) in.readObject(), is(material1));
    }

    @Test
    public void shouldReturnNotEqualsWhenUrlIsChanged() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial("A");

        SvnMaterial other = MaterialsMother.svnMaterial("B");
        assertThat(material, is(not(other)));
    }

    @Test
    public void shouldReturnNotEqualsWhenUserNameIsChanged() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial("url", "svnDir", "userName", null, false, "*.txt");

        SvnMaterial other = MaterialsMother.svnMaterial("url", "svnDir", "userName1", null, false, "*.txt");
        assertThat(material, is(not(other)));
    }

    @Test
    public void shouldReturnEqualsEvenIfPasswordsAreDifferent() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial();
        material.setPassword("password");

        SvnMaterial other = MaterialsMother.svnMaterial();
        other.setPassword("password1");
        assertThat(material, is(other));
    }

    @Test
    public void shouldReturnNotEqualsWhenCheckExternalsIsChanged() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial("url", "svnDir", null, null, true, "*.txt");
        SvnMaterial other = MaterialsMother.svnMaterial("url", "svnDir", null, null, false, "*.txt");
        assertThat(material, is(not(other)));
    }

    @Test
    public void shouldReturnEqualsWhenEverythingIsSame() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial("URL", "dummy-folder", "userName", "password", true, "*.doc");
        SvnMaterial other = MaterialsMother.svnMaterial("URL", "dummy-folder", "userName", "password", true, "*.doc");

        assertThat(other, is(material));
    }

    /* TODO: *SBD* Move this test into SvnMaterialConfig test after mothers are moved. */
    @Test
    public void shouldReturnEqualsWhenEverythingIsSameForSvnMaterialConfigs() throws Exception {
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

        assertThat(other, is(svnMaterialConfig));
    }

    @Test
    public void shouldBeAbleToConvertToJson() {
        SvnMaterial material = MaterialsMother.svnMaterial("url");
        Map<String, Object> json = new LinkedHashMap<>();
        material.toJson(json, revision);

        JsonValue jsonValue = from(json);
        assertThat(jsonValue.getString("scmType"), is("Subversion"));
        assertThat(new File(jsonValue.getString("location")), is(new File(material.getUrl())));
        assertThat(jsonValue.getString("action"), is("Modified"));
    }

    @Test
    public void shouldAddTheForwardSlashAndApplyThePattern() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial();

        assertThat(material.matches("/a.doc", "a.doc"), is(true));
        assertThat(material.matches("a.doc", "a.doc"), is(false));
    }

    @Test
    public void shouldApplyThePatternDirectly() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial();

        assertThat(material.matches("/a.doc", "/a.doc"), is(true));
    }

    @Test
    public void shouldGenerateSqlCriteriaMapInSpecificOrder() throws Exception {
        SvnMaterial material = new SvnMaterial("url", "username", "password", true);
        Map<String, Object> map = material.getSqlCriteria();
        assertThat(map.size(), is(4));
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey(), is("type"));
        assertThat(iter.next().getKey(), is("url"));
        assertThat(iter.next().getKey(), is("username"));
        assertThat(iter.next().getKey(), is("checkExternals"));
    }

    @Test
    public void shouldGenerateFingerprintBasedOnSqlCriteria() throws Exception {
        SvnMaterial one = new SvnMaterial("url", "username", "password", true);
        SvnMaterial two = new SvnMaterial("url", "username", "password", false);
        assertThat(one.getFingerprint(), is(Matchers.not(two.getFingerprint())));
        assertThat(one.getFingerprint(), is(DigestUtils.sha256Hex("type=SvnMaterial<|>url=url<|>username=username<|>checkExternals=true")));
    }

    @Test
    public void shouldGeneratePipelineUniqueFingerprintBasedOnFingerprintAndDest() throws Exception {
        SvnMaterial one = new SvnMaterial("url", "username", "password", true, "folder1");
        SvnMaterial two = new SvnMaterial("url", "username", "password", true, "folder2");
        assertThat(one.getPipelineUniqueFingerprint(), is(Matchers.not(two.getFingerprint())));
        assertThat(one.getPipelineUniqueFingerprint(), is(DigestUtils.sha256Hex("type=SvnMaterial<|>url=url<|>username=username<|>checkExternals=true<|>dest=folder1")));
    }

    @Test
    public void shouldNotUsePasswordForEquality() {
        SvnMaterial svnBoozer = new SvnMaterial("foo.com", "loser", "boozer", true);
        SvnMaterial svnZooser = new SvnMaterial("foo.com", "loser", "zooser", true);
        assertThat(svnBoozer.hashCode(), is(svnZooser.hashCode()));
        assertThat(svnBoozer, is(svnZooser));
    }

    @Test
    public void shouldEncryptSvnPasswordAndMarkPasswordAsNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");

        SvnMaterial material = new SvnMaterial("/foo", "username", "password", false, mockGoCipher);
        material.ensureEncrypted();

        assertThat(material.getPassword(), is(nullValue()));
        assertThat(material.getEncryptedPassword(), is("encrypted"));
     }

    @Test
    public void shouldDecryptSvnPassword() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        SvnMaterial material = new SvnMaterial("/foo", "username", null, false, mockGoCipher);
        ReflectionUtil.setField(material, "encryptedPassword", "encrypted");

        material.ensureEncrypted();
        assertThat(material.getPassword(), is("password"));
    }

    @Test
    public void shouldNotDecryptSvnPasswordIfPasswordIsNotNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        SvnMaterial material = new SvnMaterial("/foo", "username", "password", false, mockGoCipher);
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
        SvnMaterial material = new SvnMaterial("/foo", "username", null, false, mockGoCipher);
        ReflectionUtil.setField(material, "encryptedPassword", fakeCipherText);
        try {
            material.getPassword();
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
            new SvnMaterial("/foo", "username", "password", false, mockGoCipher);
            fail("Should have thrown up");
        }
        catch (Exception e) {
            assertThat(e.getMessage(), is("Password encryption failed. Please verify your cipher key."));
        }
    }

    @Test
    public void shouldGetLongDescriptionForMaterial(){
        SvnMaterial material = new SvnMaterial("http://url/", "user", "password", true, "folder");
        assertThat(material.getLongDescription(), is("URL: http://url/, Username: user, CheckExternals: true"));
    }

    @Test
    public void shouldCopyOverPasswordWhenConvertingToConfig() throws Exception {
        SvnMaterial material = new SvnMaterial("abc", "def", "ghi", false);
        SvnMaterialConfig config = (SvnMaterialConfig) material.config();

        assertThat(config.getEncryptedPassword(), is(not(Matchers.nullValue())));
        assertThat(config.getPassword(), is("ghi"));
    }

    private void assertComplementaryEquals(Object o1, Object o2, boolean value) {
        assertThat(o1.equals(o2), is(value));
        assertThat(o2.equals(o1), is(value));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        SvnMaterial material = new SvnMaterial("http://username:password@svnrepo.com", "user", "password", true);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat((String) attributes.get("type"), is("svn"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("svn-configuration");
        assertThat((String) configuration.get("url"), is("http://username:password@svnrepo.com"));
        assertThat((String) configuration.get("username"), is("user"));
        assertThat((String) configuration.get("password"), is("password"));
        assertThat((Boolean) configuration.get("check-externals"), is(true));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        SvnMaterial material = new SvnMaterial("http://username:password@svnrepo.com", "user", "password", true);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat((String) attributes.get("type"), is("svn"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("svn-configuration");
        assertThat((String) configuration.get("url"), is("http://username:******@svnrepo.com"));
        assertThat((String) configuration.get("username"), is("user"));
        assertThat(configuration.get("password"), is(nullValue()));
        assertThat((Boolean) configuration.get("check-externals"), is(true));
    }
}
