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

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.util.command.*;
import org.jdom2.input.SAXBuilder;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class SvnCommandTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static SvnTestRepo testRepo;
    private static final DotSvnIgnoringFilter DOT_SVN_IGNORING_FILTER = new DotSvnIgnoringFilter();

    private String svnRepositoryUrl;
    private File checkoutFolder;
    protected SvnCommand subversion;
    protected ProcessOutputStreamConsumer outputStreamConsumer;
    private final String svnInfoOutput = "<?xml version=\"1.0\"?>\n"
            + "<info>\n"
            + "<entry\n"
            + "   kind=\"dir\"\n"
            + "   path=\"project1\"\n"
            + "   revision=\"27\">\n"
            + "<url>http://localhost/svn/project1</url>\n"
            + "<repository>\n"
            + "<root>http://localhost/svn/project1</root>\n"
            + "<uuid>b51fe673-20c0-4205-a07b-5deb54bb09f3</uuid>\n"
            + "</repository>\n"
            + "<commit\n"
            + "   revision=\"27\">\n"
            + "<author>anthill</author>\n"
            + "<date>2012-10-18T07:54:06.487895Z</date>\n"
            + "</commit>\n"
            + "</entry>\n"
            + "</info>";

    @BeforeEach
    void setup() throws IOException {
        testRepo = new SvnTestRepo(temporaryFolder);
        svnRepositoryUrl = testRepo.projectRepositoryUrl();
        subversion = new SvnCommand(null, svnRepositoryUrl, "user", "pass", false);
        outputStreamConsumer = inMemoryConsumer();
        checkoutFolder = temporaryFolder.newFolder("workingcopy");
    }

    @AfterEach
    void teardown() {
        testRepo.tearDown();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldRecogniseSvnAsTheSameIfURLContainsSpaces() throws Exception {
        File working = temporaryFolder.newFolder("shouldRecogniseSvnAsTheSameIfURLContainsSpaces");
        SvnTestRepo repo = new SvnTestRepo(temporaryFolder, "a directory with spaces");
        SvnMaterial material = repo.material();
        assertThat(material.getUrl()).contains("%20");
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        material.freshCheckout(output, new SubversionRevision("3"), working);
        assertThat(output.getAllOutput()).contains("Checked out revision 3");

        InMemoryStreamConsumer output2 = new InMemoryStreamConsumer();
        material.updateTo(output2, working, new RevisionContext(new SubversionRevision("4")), new TestSubprocessExecutionContext());
        assertThat(output2.getAllOutput()).contains("Updated to revision 4");

    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldRecogniseSvnAsTheSameIfURLUsesFileProtocol() throws Exception {
        SvnTestRepo repo = new SvnTestRepo(temporaryFolder);
        File working = temporaryFolder.newFolder("someDir");
        SvnMaterial material = repo.material();
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        material.freshCheckout(output, new SubversionRevision("3"), working);
        assertThat(output.getAllOutput()).contains("Checked out revision 3");

        InMemoryStreamConsumer output2 = new InMemoryStreamConsumer();
        updateMaterial(material, new SubversionRevision("4"), working, output2);
        assertThat(output2.getAllOutput()).contains("Updated to revision 4");

    }

    private void updateMaterial(SvnMaterial material, SubversionRevision revision, File working, InMemoryStreamConsumer output2) {
        material.updateTo(output2, working, new RevisionContext(revision), new TestSubprocessExecutionContext());
    }


    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldRecogniseSvnAsTheSameIfURLContainsChineseCharacters() throws Exception {
        File working = temporaryFolder.newFolder("shouldRecogniseSvnAsTheSameIfURLContainsSpaces");
        SvnTestRepo repo = new SvnTestRepo(temporaryFolder, "a directory with 司徒空在此");
        SvnMaterial material = repo.material();
        assertThat(material.getUrl()).contains("%20");
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        material.freshCheckout(output, new SubversionRevision("3"), working);
        assertThat(output.getAllOutput()).contains("Checked out revision 3");

        InMemoryStreamConsumer output2 = new InMemoryStreamConsumer();
        updateMaterial(material, new SubversionRevision("4"), working, output2);
        assertThat(output2.getAllOutput()).contains("Updated to revision 4");

    }


    @Test
    void shouldFilterModifiedFilesByRepositoryURL() {
        subversion = new SvnCommand(null, testRepo.end2endRepositoryUrl() + "/unit-reports", "user", "pass", false);
        List list = subversion.modificationsSince(new SubversionRevision(0));

        Modification modification = (Modification) list.get(0);
        assertThat(modification.getModifiedFiles().size()).isEqualTo(3);
        for (ModifiedFile file : modification.getModifiedFiles()) {
            assertThat(file.getFileName().startsWith("/unit-reports")).isTrue();
            assertThat(file.getFileName().startsWith("/ft-reports")).isFalse();
        }
    }

    @Test
    void shouldNotFilterModifiedFilesWhileURLPointsToRoot() {
        subversion = new SvnCommand(null, testRepo.end2endRepositoryUrl(), "user", "pass", false);
        List list = subversion.modificationsSince(new SubversionRevision(0));

        Modification modification = (Modification) list.get(list.size() - 1);
        assertThat(modification.getModifiedFiles().size()).isEqualTo(7);
    }

    @Test
    void shouldCheckVCSConnection() {
        ValidationBean validationBean = subversion.checkConnection();
        assertThat(validationBean.isValid()).isTrue();
    }

    @Test
    void shouldReplacePasswordWithStarWhenCheckSvnConnection() {
        SvnCommand command = new SvnCommand(null, "http://do-not-care.com", "user", "password", false);
        ValidationBean validationBean = command.checkConnection();
        assertThat(validationBean.isValid()).isFalse();
        assertThat(validationBean.getError()).contains("INPUT");
        assertThat(validationBean.getError()).contains("******");
    }

    @Test
    void shouldGetModificationsFromSubversionSinceARevision() {
        final List list = subversion.modificationsSince(new SubversionRevision("1"));
        assertThat(list.size()).isEqualTo(3);
        assertThat(((Modification) list.get(0)).getRevision()).isEqualTo("4");
        assertThat(((Modification) list.get(1)).getRevision()).isEqualTo("3");
        assertThat(((Modification) list.get(2)).getRevision()).isEqualTo("2");
    }

    @Test
    void shouldGetLatestModificationFromSubversion() {
        final List<Modification> materialRevisions = subversion.latestModification();
        assertThat(materialRevisions.size()).isEqualTo(1);
        final Modification modification = materialRevisions.get(0);
        assertThat(modification.getComment()).isEqualTo("Added simple build shell to dump the environment to console.");
        assertThat(modification.getModifiedFiles().size()).isEqualTo(1);
    }

    @Test
    void shouldCheckoutToSpecificRevision() {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, revision(2));
        assertThat(checkoutFolder.exists()).isTrue();
        assertThat(checkoutFolder.listFiles().length).isNotEqualTo(0);
        assertAtRevision(2, "TestReport-Unit.xml");
    }

    @Test
    void shouldUpdateToSpecificRevision() {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        assertAtRevision(4, "TestReport-Unit.xml");
        subversion.updateTo(outputStreamConsumer, checkoutFolder, revision(2));
        assertAtRevision(2, "TestReport-Unit.xml");
        subversion.updateTo(outputStreamConsumer, checkoutFolder, revision(3));
        assertAtRevision(3, "revision3.txt");
    }

    @Test
    void shouldThrowExceptionWithTheSecretHiddenWhenUpdateToFails() {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        assertAtRevision(4, "TestReport-Unit.xml");
        try {
            subversion.updateTo(outputStreamConsumer, checkoutFolder, revision(-1));
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("--password ******");
        }

    }

    @Test
    void shouldUnlockAndRevertWorkingCopy() {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        File file = checkoutFolder.listFiles(DOT_SVN_IGNORING_FILTER)[0];
        file.delete();
        assertThat(file.exists()).isFalse();
        subversion.cleanupAndRevert(outputStreamConsumer, checkoutFolder);
        assertThat(file.exists()).isTrue();
    }

    @Test
    void shouldGetWorkingUrl() throws IOException {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        String url = subversion.workingRepositoryUrl(checkoutFolder);
        assertThat(URLDecoder.decode(url, "UTF-8")).isEqualToIgnoringCase(svnRepositoryUrl);
    }

    void assertAtRevision(int rev, String file) {
        String[] filenames = checkoutFolder.list(DOT_SVN_IGNORING_FILTER);
        assertThat(filenames.length).isEqualTo(rev);
        assertThat(Arrays.asList(filenames)).contains(file);
    }

    protected SubversionRevision revision(int revision) {
        return new SubversionRevision(revision);
    }

    public static class DotSvnIgnoringFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return !name.equals(".svn");
        }
    }

    @Test
    void shouldParseSvnInfoWithParthDifferentFromUrl() {
        String output = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\"PurchaseDeliverables\"\n"
                + "   revision=\"36788\">\n"
                + "<url>http://svn.somewhere.com/someotherline/bloresvn/TISSIP/branch/DEV/PurchaseDeliverables</url>\n"
                + "<repository>\n"
                + "<root>http://svn.somewhere.com/someotherline</root>\n"
                + "<uuid>f6689194-972b-e749-89bf-11ebdadc4dc5</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "   revision=\"26449\">\n"
                + "<author>nigelfer</author>\n"
                + "<date>2009-02-03T15:43:08.059944Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        SvnCommand.SvnInfo svnInfo = new SvnCommand.SvnInfo();
        svnInfo.parse(output, new SAXBuilder());
        assertThat(svnInfo.getPath()).isEqualTo("/bloresvn/TISSIP/branch/DEV/PurchaseDeliverables");
        assertThat(svnInfo.getUrl()).isEqualTo("http://svn.somewhere.com/someotherline/bloresvn/TISSIP/branch/DEV/PurchaseDeliverables");
    }

    @Test
    void shouldParseSvnInfo() {
        String output = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\"someotherline\"\n"
                + "   revision=\"36788\">\n"
                + "<url>http://svn.somewhere.com/svn/someotherline</url>\n"
                + "<repository>\n"
                + "<root>http://svn.somewhere.com/svn</root>\n"
                + "<uuid>f6689194-972b-e749-89bf-11ebdadc4dc5</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "   revision=\"36788\">\n"
                + "<author>csebasti</author>\n"
                + "<date>2009-05-30T17:47:27.435599Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        SvnCommand.SvnInfo svnInfo = new SvnCommand.SvnInfo();
        svnInfo.parse(output, new SAXBuilder());
        assertThat(svnInfo.getPath()).isEqualTo("/someotherline");
        assertThat(svnInfo.getUrl()).isEqualTo("http://svn.somewhere.com/svn/someotherline");
        assertThat(svnInfo.getRoot()).isEqualTo("http://svn.somewhere.com/svn");
    }

    @Test
    void shouldParseSvnInfoWithUTF8ChineseNameInUrl() {
        String output = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "  kind=\"dir\"\n"
                + "  path=\"司徒空在此\"\n"
                + "  revision=\"4\">\n"
                + "<url>file:///home/cceuser/bigfs/projects/cruise/common/test-resources/unit/data/svnrepo/end2end/%E5%8F%B8%E5%BE%92%E7%A9%BA%E5%9C%A8%E6%AD%A4</url>\n"
                + "<repository>\n"
                + "<root>file:///home/cceuser/bigfs/projects/cruise/common/test-resources/unit/data/svnrepo/end2end</root>\n"
                + "<uuid>f953918e-915c-4459-8d4c-83860cce9d9a</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "  revision=\"4\">\n"
                + "<author></author>\n"
                + "<date>2009-05-31T04:14:44.223393Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        SvnCommand.SvnInfo svnInfo = new SvnCommand.SvnInfo();
        svnInfo.parse(output, new SAXBuilder());
        assertThat(svnInfo.getPath()).isEqualTo("/司徒空在此");
        assertThat(svnInfo.getUrl()).isEqualTo("file:///home/cceuser/bigfs/projects/cruise/common/test-resources/unit/data/svnrepo/end2end/%E5%8F%B8%E5%BE%92%E7%A9%BA%E5%9C%A8%E6%AD%A4");
    }

    @Test
    void shouldParseEncodedUrl() {
        String output = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\"trunk\"\n"
                + "   revision=\"8650\">\n"
                + "<url>https://217.45.214.17:8443/svn/Entropy%20System/Envoy%20Enterprise/trunk</url>\n"
                + "<repository>\n"
                + "<root>https://217.45.214.17:8443/svn</root>\n"
                + "<uuid>3ed677eb-f12f-3343-ac77-786e4d01a301</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "   revision=\"8650\">\n"
                + "<author>BuildServer</author>\n"
                + "<date>2009-04-03 15:52:16 +0800 (Fri, 03 Apr 2009)</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        SvnCommand.SvnInfo svnInfo = new SvnCommand.SvnInfo();
        svnInfo.parse(output, new SAXBuilder());
        assertThat(svnInfo.getUrl()).isEqualTo("https://217.45.214.17:8443/svn/Entropy%20System/Envoy%20Enterprise/trunk");
        assertThat(svnInfo.getPath()).isEqualTo("/Entropy System/Envoy Enterprise/trunk");
    }

    @Test
    void shouldParseEncodedUrlAndPath() {
        String output = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\"unit-reports\"\n"
                + "   revision=\"3\">\n"
                + "<url>file:///C:/Documents%20and%20Settings/cceuser/Local%20Settings/Temp/testSvnRepo-1243722556125/end2end/unit-reports</url>\n"
                + "<repository>\n"
                + "<root>file:///C:/Documents%20and%20Settings/cceuser/Local%20Settings/Temp/testSvnRepo-1243722556125/end2end</root>\n"
                + "<uuid>f953918e-915c-4459-8d4c-83860cce9d9a</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "   revision=\"1\">\n"
                + "<author>cceuser</author>\n"
                + "<date>2008-03-20T04:00:43.976517Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        SvnCommand.SvnInfo svnInfo = new SvnCommand.SvnInfo();
        svnInfo.parse(output, new SAXBuilder());
        assertThat(svnInfo.getUrl()).isEqualTo("file:///C:/Documents%20and%20Settings/cceuser/Local%20Settings/Temp/testSvnRepo-1243722556125/end2end/unit-reports");
        assertThat(svnInfo.getPath()).isEqualTo("/unit-reports");
    }

    @Test
    void shouldParsePartlyEncodedUrlAndPath() {
        String output = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\"unit-reports\"\n"
                + "   revision=\"3\">\n"
                + "<url>svn+ssh://hostname/foo%20bar%20baz/end2end</url>\n"
                + "<repository>\n"
                + "<root>svn+ssh://hostname/foo%20bar%20baz</root>\n"
                + "<uuid>f953918e-915c-4459-8d4c-83860cce9d9a</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "   revision=\"1\">\n"
                + "<author>cceuser</author>\n"
                + "<date>2008-03-20T04:00:43.976517Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        SvnCommand.SvnInfo svnInfo = new SvnCommand.SvnInfo();
        svnInfo.parse(output, new SAXBuilder());
        assertThat(svnInfo.getUrl()).isEqualTo("svn+ssh://hostname/foo%20bar%20baz/end2end");
        assertThat(svnInfo.getPath()).isEqualTo("/end2end");
    }

    @Test
    void shouldHidePasswordInUrl() {
        SvnCommand command = new SvnCommand(
                null, "https://user:password@217.45.214.17:8443/svn/Entropy%20System/Envoy%20Enterprise/trunk");
        assertThat(command.getUrlForDisplay()).isEqualTo("https://user:******@217.45.214.17:8443/svn/Entropy%20System/Envoy%20Enterprise/trunk");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldSupportUTF8CheckInMessageAndFilename() throws Exception {
        String message = "司徒空在此";
        String filename = "司徒空在此.scn";
        testRepo.checkInOneFile(filename, message);

        Modification modification = subversion.latestModification().get(0);
        assertThat(modification.getComment()).isEqualTo(message);
        assertThat(modification.getModifiedFiles().get(0).getFileName()).contains(filename);
    }

    @Test
    void shouldNotAddEmptyPasswordWhenUsernameIsProvidedWithNoPassword() {
        SvnCommand command = new SvnCommand(null, "url", "shilpaIsGreat", null, false);
        CommandArgument argument = new StringArgument("--password=");
        assertThat(command.buildSvnLogCommandForLatestOne().getArguments()).doesNotContain(argument);
    }

    @Test
    void shouldNotAddEmptyPasswordWhenUsernameIsProvidedWithPassword() {
        SvnCommand command = new SvnCommand(null, "url", "shilpaIsGreat", "noSheIsNot", false);
        CommandArgument argument = new StringArgument("--password");
        CommandArgument passArg = new PasswordArgument("noSheIsNot");
        assertThat(command.buildSvnLogCommandForLatestOne().getArguments()).contains(argument, passArg);
    }

    @Test
    void shouldAddNothingWhenNoUsernameIsNotProvided() {
        SvnCommand commandWithNullUsername = new SvnCommand(null, "url", null, null, false);

        assertThat(commandWithNullUsername.buildSvnLogCommandForLatestOne().toString().contains("--password=")).isFalse();
        assertThat(commandWithNullUsername.buildSvnLogCommandForLatestOne().toString().contains("--username")).isFalse();

        SvnCommand commandWithEmptyUsername = new SvnCommand(null, "url", " ", " ", false);

        assertThat(commandWithEmptyUsername.buildSvnLogCommandForLatestOne().toString().contains("--password=")).isFalse();
        assertThat(commandWithEmptyUsername.buildSvnLogCommandForLatestOne().toString().contains("--username")).isFalse();
    }

    @Test
    void shouldGetSvnInfoAndReturnMapOfUrlToUUID() {
        final String svnInfoOutput = "<?xml version=\"1.0\"?>\n"
                + "<info>\n"
                + "<entry\n"
                + "   kind=\"dir\"\n"
                + "   path=\"project1\"\n"
                + "   revision=\"27\">\n"
                + "<url>http://localhost/svn/project1</url>\n"
                + "<repository>\n"
                + "<root>http://localhost/svn/project1</root>\n"
                + "<uuid>b51fe673-20c0-4205-a07b-5deb54bb09f3</uuid>\n"
                + "</repository>\n"
                + "<commit\n"
                + "   revision=\"27\">\n"
                + "<author>anthill</author>\n"
                + "<date>2012-10-18T07:54:06.487895Z</date>\n"
                + "</commit>\n"
                + "</entry>\n"
                + "</info>";
        final SvnMaterial svnMaterial = mock(SvnMaterial.class);
        when(svnMaterial.urlForCommandLine()).thenReturn("http://localhost/svn/project1");
        when(svnMaterial.getUserName()).thenReturn("user");
        when(svnMaterial.passwordForCommandLine()).thenReturn("password");
        final ConsoleResult consoleResult = mock(ConsoleResult.class);
        when(consoleResult.outputAsString()).thenReturn(svnInfoOutput);
        final HashSet<SvnMaterial> svnMaterials = new HashSet<>();
        svnMaterials.add(svnMaterial);
        final SvnCommand spy = spy(subversion);
        doAnswer(invocation -> {
            final CommandLine commandLine = (CommandLine) invocation.getArguments()[0];
            assertThat(commandLine.toString()).contains("svn info --xml --username user --password ****** http://localhost/svn/project1");
            return consoleResult;
        }).when(spy).executeCommand(any(CommandLine.class));
        final HashMap<String, String> urlToRemoteUUIDMap = spy.createUrlToRemoteUUIDMap(svnMaterials);
        assertThat(urlToRemoteUUIDMap.size()).isEqualTo(1);
        assertThat(urlToRemoteUUIDMap.get("http://localhost/svn/project1")).isEqualTo("b51fe673-20c0-4205-a07b-5deb54bb09f3");
    }

    @Test
    void shouldGetSvnInfoForMultipleMaterialsAndReturnMapOfUrlToUUID() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        when(svnMaterial1.urlForCommandLine()).thenReturn("http://localhost/svn/project1");
        final SvnMaterial svnMaterial2 = mock(SvnMaterial.class);
        when(svnMaterial2.urlForCommandLine()).thenReturn("http://foo.bar");
        final HashSet<SvnMaterial> svnMaterials = new HashSet<>();
        svnMaterials.add(svnMaterial1);
        svnMaterials.add(svnMaterial2);
        final SvnCommand spy = spy(subversion);
        doAnswer(invocation -> {
            final ConsoleResult consoleResult = mock(ConsoleResult.class);
            when(consoleResult.outputAsString()).thenReturn(svnInfoOutput);
            final CommandLine commandLine = (CommandLine) invocation.getArguments()[0];
            if (commandLine.toString().contains("http://localhost/svn/project1")) {
                return consoleResult;
            } else {
                throw new RuntimeException("Some thing crapped out");
            }
        }).when(spy).executeCommand(any(CommandLine.class));

        HashMap<String, String> urlToRemoteUUIDMap = null;
        try {
            urlToRemoteUUIDMap = spy.createUrlToRemoteUUIDMap(svnMaterials);
        } catch (Exception e) {
            fail("Should not have failed although exception was thrown " + e);
        }

        assertThat(urlToRemoteUUIDMap.size()).isEqualTo(1);
        assertThat(urlToRemoteUUIDMap.get("http://localhost/svn/project1")).isEqualTo("b51fe673-20c0-4205-a07b-5deb54bb09f3");
        verify(spy, times(2)).executeCommand(any(CommandLine.class));
    }

    @Test
    void shouldUseCorrectCredentialsPerSvnMaterialWhenQueryingForInfo() {
        final String svnMaterial1Url = "http://localhost/svn/project1";
        final String svnMaterial1User = "svnMaterial1_user";
        final String svnMaterial1Password = "svnMaterial1_password";
        final SvnMaterial svnMaterial1 = buildMockSvnMaterial(svnMaterial1Url, svnMaterial1User, svnMaterial1Password);
        String svnMaterial2Url = "http://localhost/svn/project2";
        SvnMaterial svnMaterial2 = buildMockSvnMaterial(svnMaterial2Url, null, null);
        HashSet<SvnMaterial> svnMaterials = new HashSet<>();
        svnMaterials.add(svnMaterial1);
        svnMaterials.add(svnMaterial2);

        SvnCommand spy = spy(subversion);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                final ConsoleResult consoleResult = mock(ConsoleResult.class);
                when(consoleResult.outputAsString()).thenReturn(svnInfoOutput);
                verifyCommandLine((CommandLine) invocation.getArguments()[0]);
                return consoleResult;
            }

            private void verifyCommandLine(CommandLine commandLine) {
                String commandString = commandLine.toStringForDisplay();
                if (commandString.contains(svnMaterial1User)) {
                    List<CommandArgument> arguments = commandLine.getArguments();
                    for (CommandArgument argument : arguments) {
                        if (argument instanceof PasswordArgument) {
                            assertThat(argument.originalArgument()).isEqualTo(svnMaterial1Password);
                        }
                    }
                } else {
                    assertThat(commandString).doesNotContainPattern("--username");
                    assertThat(commandString).doesNotContainPattern("password");
                }
            }
        }).when(spy).executeCommand(any(CommandLine.class));

        spy.createUrlToRemoteUUIDMap(svnMaterials);

        verify(svnMaterial1).getUserName();
        verify(svnMaterial1).passwordForCommandLine();
        verify(svnMaterial2).getUserName();
        verify(svnMaterial2).passwordForCommandLine();
    }

    private SvnMaterial buildMockSvnMaterial(String url, String username, String password) {
        final SvnMaterial svnMaterial = mock(SvnMaterial.class);
        when(svnMaterial.getUrl()).thenReturn(url);
        when(svnMaterial.getUserName()).thenReturn(username);
        when(svnMaterial.getPassword()).thenReturn(password);
        return svnMaterial;
    }
}
