/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.materials.svn;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.*;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(JunitExtRunner.class)
public class SvnCommandTest {
    public static SvnTestRepo testRepo;
    protected static final DotSvnIgnoringFilter DOT_SVN_IGNORING_FILTER = new DotSvnIgnoringFilter();

    protected String svnRepositoryUrl;
    protected File checkoutFolder;
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

    @Before
    public void setup() throws IOException {
        testRepo = new SvnTestRepo();
        svnRepositoryUrl = testRepo.projectRepositoryUrl();
        subversion = new SvnCommand(null, svnRepositoryUrl, "user", "pass", false);
        outputStreamConsumer = inMemoryConsumer();
        checkoutFolder = TestFileUtil.createUniqueTempFile("workingcopy" + System.currentTimeMillis());
    }

    @After
    public void teardown() {
        testRepo.tearDown();
        FileUtil.deleteFolder(checkoutFolder);
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldRecogniseSvnAsTheSameIfURLContainsSpaces() throws Exception {
        File working = TestFileUtil.createTempFolder("shouldRecogniseSvnAsTheSameIfURLContainsSpaces");
        SvnTestRepo repo = new SvnTestRepo("a directory with spaces");
        SvnMaterial material = repo.material();
        assertThat(material.getUrl(), containsString("%20"));
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        material.freshCheckout(output, new SubversionRevision("3"), working);
        assertThat(output.getAllOutput(), containsString("Checked out revision 3"));

        InMemoryStreamConsumer output2 = new InMemoryStreamConsumer();
        material.updateTo(output2, working, new RevisionContext(new SubversionRevision("4")), new TestSubprocessExecutionContext());
        assertThat(output2.getAllOutput(), containsString("Updated to revision 4"));

    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldRecogniseSvnAsTheSameIfURLUsesFileProtocol() throws Exception {
        SvnTestRepo repo = new SvnTestRepo("svnRepo");
        File working = TestFileUtil.createTempFolder("someDir");
        SvnMaterial material = repo.material();
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        material.freshCheckout(output, new SubversionRevision("3"), working);
        assertThat(output.getAllOutput(), containsString("Checked out revision 3"));

        InMemoryStreamConsumer output2 = new InMemoryStreamConsumer();
        updateMaterial(material, new SubversionRevision("4"), working, output2);
        assertThat(output2.getAllOutput(), containsString("Updated to revision 4"));

    }

    private void updateMaterial(SvnMaterial material, SubversionRevision revision, File working, InMemoryStreamConsumer output2) {
        material.updateTo(output2, working, new RevisionContext(revision), new TestSubprocessExecutionContext());
    }


    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldRecogniseSvnAsTheSameIfURLContainsChineseCharacters() throws Exception {
        File working = TestFileUtil.createTempFolder("shouldRecogniseSvnAsTheSameIfURLContainsSpaces");
        SvnTestRepo repo = new SvnTestRepo("a directory with 司徒空在此");
        SvnMaterial material = repo.material();
        assertThat(material.getUrl(), containsString("%20"));
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        material.freshCheckout(output, new SubversionRevision("3"), working);
        assertThat(output.getAllOutput(), containsString("Checked out revision 3"));

        InMemoryStreamConsumer output2 = new InMemoryStreamConsumer();
        updateMaterial(material, new SubversionRevision("4"), working, output2);
        assertThat(output2.getAllOutput(), containsString("Updated to revision 4"));

    }


    @Test
    public void shouldFilterModifiedFilesByRepositoryURL() throws Exception {
        subversion = new SvnCommand(null, testRepo.end2endRepositoryUrl() + "/unit-reports", "user", "pass", false);
        List list = subversion.modificationsSince(new SubversionRevision(0));

        Modification modification = (Modification) list.get(0);
        assertThat(modification.getModifiedFiles().size(), is(3));
        for (ModifiedFile file : modification.getModifiedFiles()) {
            assertThat(file.getFileName().startsWith("/unit-reports"), is(true));
            assertThat(file.getFileName().startsWith("/ft-reports"), is(false));
        }
    }

    @Test
    public void shouldNotFilterModifiedFilesWhileURLPointsToRoot() throws Exception {
        subversion = new SvnCommand(null, testRepo.end2endRepositoryUrl(), "user", "pass", false);
        List list = subversion.modificationsSince(new SubversionRevision(0));

        Modification modification = (Modification) list.get(list.size() - 1);
        assertThat(modification.getModifiedFiles().size(), is(7));
    }

    @Test
    public void shouldCheckVCSConnection() throws Exception {
        ValidationBean validationBean = subversion.checkConnection();
        assertThat(validationBean.isValid(), is(true));
    }

    @Test
    public void shouldReplacePasswordWithStarWhenCheckSvnConnection() throws Exception {
        SvnCommand command = new SvnCommand(null, "http://do-not-care.com", "user", "password", false);
        ValidationBean validationBean = command.checkConnection();
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("INPUT"));
        assertThat(validationBean.getError(), containsString("******"));
    }

    @Test
    public void shouldGetModificationsFromSubversionSinceARevision() {
        final List list = subversion.modificationsSince(new SubversionRevision("1"));
        assertThat(list.size(), is(3));
        assertThat(((Modification) list.get(0)).getRevision(), is("4"));
        assertThat(((Modification) list.get(1)).getRevision(), is("3"));
        assertThat(((Modification) list.get(2)).getRevision(), is("2"));
    }

    @Test
    public void shouldGetLatestModificationFromSubversion() {
        final List<Modification> materialRevisions = subversion.latestModification();
        assertThat(materialRevisions.size(), is(1));
        final Modification modification = materialRevisions.get(0);
        assertThat(modification.getComment(), is("Added simple build shell to dump the environment to console."));
        assertThat(modification.getModifiedFiles().size(), is(1));
    }

    @Test
    public void shouldCheckoutToSpecificRevision() {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, revision(2));
        assertThat(checkoutFolder.exists(), is(true));
        assertThat(checkoutFolder.listFiles().length, org.hamcrest.core.Is.is(not(0)));
        assertAtRevision(2, "TestReport-Unit.xml");
    }

    @Test
    public void shouldUpdateToSpecificRevision() {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        assertAtRevision(4, "TestReport-Unit.xml");
        subversion.updateTo(outputStreamConsumer, checkoutFolder, revision(2));
        assertAtRevision(2, "TestReport-Unit.xml");
        subversion.updateTo(outputStreamConsumer, checkoutFolder, revision(3));
        assertAtRevision(3, "revision3.txt");
    }

    @Test
    public void shouldThrowExceptionWithTheSecretHiddenWhenUpdateToFails() {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        assertAtRevision(4, "TestReport-Unit.xml");
        try {
            subversion.updateTo(outputStreamConsumer, checkoutFolder , revision(-1));
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("--password ******"));
        }

    }

    @Test public void shouldUnlockAndRevertWorkingCopy() throws Exception {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        File file = checkoutFolder.listFiles(DOT_SVN_IGNORING_FILTER)[0];
        file.delete();
        assertThat(file.exists(), is(false));
        subversion.cleanupAndRevert(outputStreamConsumer, checkoutFolder);
        assertThat(file.exists(), is(true));
    }

    @Test public void shouldGetWorkingUrl() throws IOException {
        subversion.checkoutTo(outputStreamConsumer, checkoutFolder, SubversionRevision.HEAD);
        String url = subversion.workingRepositoryUrl(checkoutFolder);
        assertThat(URLDecoder.decode(url, "UTF-8"), is(svnRepositoryUrl));
    }

    protected void assertAtRevision(int rev, String file) {
        String[] filenames = checkoutFolder.list(DOT_SVN_IGNORING_FILTER);
        assertThat(filenames.length, Is.is(rev));
        assertThat(Arrays.asList(filenames), org.junit.matchers.JUnitMatchers.hasItem(file));
    }

    protected SubversionRevision revision(int revision) {
        return new SubversionRevision(revision);
    }

    public static class DotSvnIgnoringFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return !name.equals(".svn");
        }
    }

    @Test
    public void shouldParseSvnInfoWithParthDifferentFromUrl() throws Exception {
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
        assertThat(svnInfo.getPath(), is("/bloresvn/TISSIP/branch/DEV/PurchaseDeliverables"));
        assertThat(svnInfo.getUrl(),
                is("http://svn.somewhere.com/someotherline/bloresvn/TISSIP/branch/DEV/PurchaseDeliverables"));
    }

    @Test
    public void shouldParseSvnInfo() throws Exception {
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
        assertThat(svnInfo.getPath(), is("/someotherline"));
        assertThat(svnInfo.getUrl(),
                is("http://svn.somewhere.com/svn/someotherline"));
        assertThat(svnInfo.getRoot(), is("http://svn.somewhere.com/svn"));
    }

    @Test
    public void shouldParseSvnInfoWithUTF8ChineseNameInUrl() throws Exception {
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
        assertThat(svnInfo.getPath(), is("/司徒空在此"));
        assertThat(svnInfo.getUrl(),
                is("file:///home/cceuser/bigfs/projects/cruise/common/test-resources/unit/data/svnrepo/end2end/%E5%8F%B8%E5%BE%92%E7%A9%BA%E5%9C%A8%E6%AD%A4"));
    }

    @Test
    public void shouldParseEncodedUrl() {
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
        assertThat(svnInfo.getUrl(), is("https://217.45.214.17:8443/svn/Entropy%20System/Envoy%20Enterprise/trunk"));
        assertThat(svnInfo.getPath(), is("/Entropy System/Envoy Enterprise/trunk"));
    }

    @Test
    public void shouldParseEncodedUrlAndPath() {
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
        assertThat(svnInfo.getUrl(), is("file:///C:/Documents%20and%20Settings/cceuser/Local%20Settings/Temp/testSvnRepo-1243722556125/end2end/unit-reports"));
        assertThat(svnInfo.getPath(), is("/unit-reports"));
    }

    @Test
    public void shouldParsePartlyEncodedUrlAndPath() {
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
        assertThat(svnInfo.getUrl(), is("svn+ssh://hostname/foo%20bar%20baz/end2end"));
        assertThat(svnInfo.getPath(), is("/end2end"));
    }

    @Test
    public void shouldHidePasswordInUrl() {
        SvnCommand command = new SvnCommand(
                null, "https://user:password@217.45.214.17:8443/svn/Entropy%20System/Envoy%20Enterprise/trunk");
        assertThat(command.getUrlForDisplay(),
                is("https://user:******@217.45.214.17:8443/svn/Entropy%20System/Envoy%20Enterprise/trunk"));
    }

    @Test
    public void shouldSupportUTF8CheckInMessageAndFilename() throws Exception {
        String message = "司徒空在此";
        String filename = "司徒空在此.scn";
        testRepo.checkInOneFile(filename, message);

        Modification modification = subversion.latestModification().get(0);
        assertThat(modification.getComment(), is(message));
        assertThat(modification.getModifiedFiles().get(0).getFileName(), containsString(filename));
    }

    @Test
    public void shouldNotAddEmptyPasswordWhenUsernameIsProvidedWithNoPassword() throws IOException {
        SvnCommand command = new SvnCommand(null, "url", "shilpaIsGreat", null, false);
        CommandArgument argument = new StringArgument(String.format("--password="));
        assertThat(command.buildSvnLogCommandForLatestOne().getArguments(), not(hasItem(argument)));
    }

    @Test
    public void shouldNotAddEmptyPasswordWhenUsernameIsProvidedWithPassword() throws IOException {
        SvnCommand command = new SvnCommand(null, "url", "shilpaIsGreat", "noSheIsNot", false);
        CommandArgument argument = new StringArgument("--password");
        CommandArgument passArg = new PasswordArgument("noSheIsNot");
        assertThat(command.buildSvnLogCommandForLatestOne().getArguments(), hasItems(argument, passArg));
    }

    @Test
    public void shouldAddNothingWhenNoUsernameIsNotProvided() throws IOException {
        SvnCommand commandWithNullUsername = new SvnCommand(null, "url", null, null, false);

        assertThat(commandWithNullUsername.buildSvnLogCommandForLatestOne().toString().contains("--password="), is(false));
        assertThat(commandWithNullUsername.buildSvnLogCommandForLatestOne().toString().contains("--username"), is(false));

        SvnCommand commandWithEmptyUsername = new SvnCommand(null, "url", " ", " ", false);

        assertThat(commandWithEmptyUsername.buildSvnLogCommandForLatestOne().toString().contains("--password="), is(false));
        assertThat(commandWithEmptyUsername.buildSvnLogCommandForLatestOne().toString().contains("--username"), is(false));
    }

    @Test
    public void shouldGetSvnInfoAndReturnMapOfUrlToUUID() {
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
        when(svnMaterial.getUrl()).thenReturn("http://localhost/svn/project1");
        when(svnMaterial.getUserName()).thenReturn("user");
        when(svnMaterial.getPassword()).thenReturn("password");
        final ConsoleResult consoleResult = mock(ConsoleResult.class);
        when(consoleResult.outputAsString()).thenReturn(svnInfoOutput);
        final HashSet<SvnMaterial> svnMaterials = new HashSet<SvnMaterial>();
        svnMaterials.add(svnMaterial);
        final SvnCommand spy = spy(subversion);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                final CommandLine commandLine = (CommandLine) invocation.getArguments()[0];
                assertThat(commandLine.toString(), containsString("svn info --xml --username user --password ****** http://localhost/svn/project1"));
                return consoleResult;
            }
        }).when(spy).executeCommand(any(CommandLine.class));
        final HashMap<String,String> urlToRemoteUUIDMap = spy.createUrlToRemoteUUIDMap(svnMaterials);
        assertThat(urlToRemoteUUIDMap.size(), is(1));
        assertThat(urlToRemoteUUIDMap.get("http://localhost/svn/project1"), is("b51fe673-20c0-4205-a07b-5deb54bb09f3"));
    }

    @Test
    public void shouldGetSvnInfoForMultipleMaterialsAndReturnMapOfUrlToUUID() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        when(svnMaterial1.getUrl()).thenReturn("http://localhost/svn/project1");
        final SvnMaterial svnMaterial2 = mock(SvnMaterial.class);
        when(svnMaterial2.getUrl()).thenReturn("http://foo.bar");
        final HashSet<SvnMaterial> svnMaterials = new HashSet<SvnMaterial>();
        svnMaterials.add(svnMaterial1);
        svnMaterials.add(svnMaterial2);
        final SvnCommand spy = spy(subversion);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                final ConsoleResult consoleResult = mock(ConsoleResult.class);
                when(consoleResult.outputAsString()).thenReturn(svnInfoOutput);
                final CommandLine commandLine = (CommandLine) invocation.getArguments()[0];
                if (commandLine.toString().contains("http://localhost/svn/project1")) {
                    return consoleResult;
                } else {
                    throw new RuntimeException("Some thing crapped out");
                }
            }
        }).when(spy).executeCommand(any(CommandLine.class));

        HashMap<String,String> urlToRemoteUUIDMap = null;
        try {
            urlToRemoteUUIDMap = spy.createUrlToRemoteUUIDMap(svnMaterials);
        } catch (Exception e) {
            fail("Should not have failed although exception was thrown " + e);
        }

        assertThat(urlToRemoteUUIDMap.size(), is(1));
        assertThat(urlToRemoteUUIDMap.get("http://localhost/svn/project1"), is("b51fe673-20c0-4205-a07b-5deb54bb09f3"));
        verify(spy, times(2)).executeCommand(any(CommandLine.class));
    }

    @Test
    public void shouldUseCorrectCredentialsPerSvnMaterialWhenQueryingForInfo() throws Exception {
        final String svnMaterial1Url = "http://localhost/svn/project1";
        final String svnMaterial1User = "svnMaterial1_user";
        final String svnMaterial1Password = "svnMaterial1_password";
        final SvnMaterial svnMaterial1 = buildMockSvnMaterial(svnMaterial1Url, svnMaterial1User, svnMaterial1Password);
        String svnMaterial2Url = "http://localhost/svn/project2";
        SvnMaterial svnMaterial2 = buildMockSvnMaterial(svnMaterial2Url, null, null);
        HashSet<SvnMaterial> svnMaterials = new HashSet<SvnMaterial>();
        svnMaterials.add(svnMaterial1);
        svnMaterials.add(svnMaterial2);

        SvnCommand spy = spy(subversion);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
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
                            assertThat(argument.forCommandline(), is(svnMaterial1Password));
                        }
                    }
                } else {
                    assertThat(commandString, not(Matchers.containsString("--username")));
                    assertThat(commandString, not(Matchers.containsString("password")));
                }
            }
        }).when(spy).executeCommand(any(CommandLine.class));

        HashMap<String, String> result = spy.createUrlToRemoteUUIDMap(svnMaterials);


        verify(svnMaterial1).getUserName();
        verify(svnMaterial1).getPassword();
        verify(svnMaterial2).getUserName();
        verify(svnMaterial2).getPassword();
    }

    private SvnMaterial buildMockSvnMaterial(String url, String username, String password) {
        final SvnMaterial svnMaterial = mock(SvnMaterial.class);
        when(svnMaterial.getUrl()).thenReturn(url);
        when(svnMaterial.getUserName()).thenReturn(username);
        when(svnMaterial.getPassword()).thenReturn(password);
        return svnMaterial;
    }
}
