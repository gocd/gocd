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

package com.thoughtworks.go.server.service.lookups;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.helper.CommandSnippetMother;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(JunitExtRunner.class)
public class CommandRepositoryDirectoryWalkerTest {

    TempFiles tempFiles = new TempFiles();
    private File hiddenFolder;
    private File visibleReadableFolder;
    private File folderWithNoReadAccess;
    private ServerHealthService serverHealthService;
    private CommandRepositoryDirectoryWalker walker;
    private File xmlFile;
    private File docFile;
    private File sampleDir;

    @Before
    public void setUp() throws IOException {
        hiddenFolder = tempFiles.mkdir(".hidden");
        visibleReadableFolder = tempFiles.mkdir("visible");
        folderWithNoReadAccess = tempFiles.mkdir("noReadAccess");
        xmlFile = tempFiles.createFile("foo.xml");
        docFile = tempFiles.createFile("foo.doc");
        serverHealthService = mock(ServerHealthService.class);
        walker = new CommandRepositoryDirectoryWalker(serverHealthService, mock(SystemEnvironment.class));
        sampleDir = tempFiles.mkdir("sampleDir");
    }

    @After
    public void tearDown() {
        tempFiles.cleanUp();
        sampleDir.delete();
    }

    @Test
    public void shouldIgnoreAllUnixStyleHiddenDirectoriesAndShouldNotUpdateServerHealth() throws IOException {
        assertThat(walker.handleDirectory(hiddenFolder, 0, new ArrayList()), is(false));
        verify(serverHealthService, never()).update(Matchers.<ServerHealthState>anyObject());
    }

    @Test
    public void shouldProcessADirectoryWithReadAccess() throws IOException {
        assertThat(walker.handleDirectory(visibleReadableFolder, 0, new ArrayList()), is(true));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldIgnoreFoldersForWhichUsersDoNotHaveReadAccess() throws IOException {
        folderWithNoReadAccess.setReadable(false);
        assertThat(walker.handleDirectory(folderWithNoReadAccess, 0, new ArrayList()), is(false));
    }

    @Test
    public void shouldProcessXmlFiles() throws IOException {
        FileUtils.writeStringToFile(xmlFile, CommandSnippetMother.validXMLSnippetContentForCommand("MsBuild"), UTF_8);
        ArrayList results = new ArrayList();
        walker.handleFile(xmlFile, 0, results);
        assertThat(results.size(), is(1));

        CommandSnippet snippet = (CommandSnippet) results.get(0);
        Assert.assertThat(snippet.getBaseFileName(), is("foo"));
        Assert.assertThat(snippet.getCommandName(), is("MsBuild"));
        Assert.assertThat(snippet.getArguments(), is(Arrays.asList("pack", "component.nuspec")));
    }

    @Test
    public void shouldProcessXmlFilesInsideCommandRepo() throws Exception {
        File command_repo = tempFiles.createUniqueFolder("command-repo");
        File windows = TestFileUtil.createTestFolder(command_repo, "windows");
        FileUtils.writeStringToFile(new File(windows, "msbuild.xml"), CommandSnippetMother.validXMLSnippetContentForCommand("MsBuild"), UTF_8);

        CommandSnippets results = walker.getAllCommandSnippets(command_repo.getPath());

        String expectedRelativePath = "/windows/msbuild.xml".replace('/', File.separatorChar);
        assertThat(results,
                is(new CommandSnippets(Arrays.asList(new CommandSnippet("MsBuild", Arrays.asList("pack", "component.nuspec"), new EmptySnippetComment(), "msbuild", expectedRelativePath)))));
    }

    @Test
    public void shouldIgnoreNonXmlFiles() throws IOException {
        ArrayList results = new ArrayList();
        walker.handleFile(docFile, 0, results);
        assertThat(results.isEmpty(), is(true));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldUpdateServerHealthServiceIfTheCommandRepositoryDirectoryIsUnReadable() throws IOException {
        sampleDir.setReadable(false);

        walker.getAllCommandSnippets(sampleDir.getPath());

        verify(serverHealthService).update(serverHealthWarningMessageWhichContains("Failed to access command repository located in Go Server Directory at " + sampleDir.getPath() +
                ". The directory does not exist or Go does not have sufficient permissions to access it."));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldUpdateServerHealthServiceIfTheCommandRepositoryDirectoryIsNotExecutable() throws IOException {
        sampleDir.setReadable(true);
        sampleDir.setExecutable(false);

        walker.getAllCommandSnippets(sampleDir.getPath());

        verify(serverHealthService).update(serverHealthWarningMessageWhichContains("Failed to access command repository located in Go Server Directory at " + sampleDir.getPath() +
                ". The directory does not exist or Go does not have sufficient permissions to access it."));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldUpdateServerHealthServiceIfTheCommandRepositoryDirectoryDoesNotExist() throws IOException {
        File nonExistentDirectory = new File("dirDoesNotExist");
        walker.getAllCommandSnippets(nonExistentDirectory.getPath());

        verify(serverHealthService).update(serverHealthWarningMessageWhichContains("Failed to access command repository located in Go Server Directory at " + nonExistentDirectory.getPath() +
                ". The directory does not exist or Go does not have sufficient permissions to access it."));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldUpdateServerHealthServiceIfTheCommandRepositoryDirectoryIsUnReadableAndRemoveItOnceItsReadable() throws IOException {
        sampleDir.setReadable(false);
        walker.getAllCommandSnippets(sampleDir.getPath());

        verify(serverHealthService).update(serverHealthWarningMessageWhichContains("Failed to access command repository located in Go Server Directory at " + sampleDir.getPath() +
                ". The directory does not exist or Go does not have sufficient permissions to access it."));

        sampleDir.setReadable(true);
        walker.getAllCommandSnippets(sampleDir.getPath());

        verify(serverHealthService, times(2)).update(serverHealthMessageWhichSaysItsOk());
        verifyNoMoreInteractions(serverHealthService);
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldUpdateServerHealthServiceIfACommandSnippetXMLIsUnReadableAndRemoveItOnceItsReadable() throws IOException {
        File dirWithUnreadableFile = tempFiles.mkdir("dirWithUnreadableFile");
        File unreadableFile = new File(dirWithUnreadableFile, "unreadable.xml");
        FileUtils.copyFile(xmlFile, unreadableFile);

        unreadableFile.setReadable(false);
        walker.getAllCommandSnippets(dirWithUnreadableFile.getPath());

        verify(serverHealthService).update(serverHealthWarningMessageWhichContains("Failed to access command snippet XML file located in Go Server Directory at " + unreadableFile.getPath() +
                ". Go does not have sufficient permissions to access it."));

        unreadableFile.setReadable(true);
        walker.getAllCommandSnippets(dirWithUnreadableFile.getPath());

        verify(serverHealthService, times(2)).update(serverHealthMessageWhichSaysItsOk());
        verifyNoMoreInteractions(serverHealthService);
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldUpdateServerHealthServiceIfTheCommandRepositoryDirectoryIsActuallyAFile() throws IOException {
        walker.getAllCommandSnippets(xmlFile.getPath());

        verify(serverHealthService).update(serverHealthWarningMessageWhichContains("Failed to access command repository located in Go Server Directory at " + xmlFile.getPath() +
                ". The directory does not exist or Go does not have sufficient permissions to access it."));
    }

    private ServerHealthState serverHealthMessageWhichSaysItsOk() {
        return argThat(new BaseMatcher<ServerHealthState>() {
            @Override
            public boolean matches(Object o) {
                return ((ServerHealthState) o).isRealSuccess();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected success health message.");
            }
        });
    }

    private ServerHealthState serverHealthWarningMessageWhichContains(final String expectedPartOfMessage) {
        return argThat(new BaseMatcher<ServerHealthState>() {
            @Override
            public boolean matches(Object o) {
                ServerHealthState serverHealthState = (ServerHealthState) o;
                String description = serverHealthState.getDescription();

                boolean isTheMessageWeHaveBeenWaitingFor = description.contains(expectedPartOfMessage);
                if (isTheMessageWeHaveBeenWaitingFor) {
                    assertThat(serverHealthState.getLogLevel(), is(HealthStateLevel.WARNING));
                }
                return isTheMessageWeHaveBeenWaitingFor;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected message to contain: " + expectedPartOfMessage);
            }
        });
    }
}
