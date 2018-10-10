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

package com.thoughtworks.go.server.service.lookups;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.helper.CommandSnippetMother;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(JunitExtRunner.class)
public class CommandRepositoryDirectoryWalkerTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

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
        temporaryFolder.create();
        hiddenFolder = temporaryFolder.newFolder(".hidden");
        visibleReadableFolder = temporaryFolder.newFolder("visible");
        folderWithNoReadAccess = temporaryFolder.newFolder("noReadAccess");
        xmlFile = temporaryFolder.newFile("foo.xml");
        docFile = temporaryFolder.newFile("foo.doc");
        serverHealthService = mock(ServerHealthService.class);
        walker = new CommandRepositoryDirectoryWalker(serverHealthService, mock(SystemEnvironment.class));
        sampleDir = temporaryFolder.newFolder("sampleDir");
    }

    @After
    public void tearDown() {
        temporaryFolder.delete();
        sampleDir.delete();
    }

    @Test
    public void shouldIgnoreAllUnixStyleHiddenDirectoriesAndShouldNotUpdateServerHealth() throws IOException {
        assertThat(walker.handleDirectory(hiddenFolder, 0, new ArrayList()), is(false));
        verify(serverHealthService, never()).update(any(ServerHealthState.class));
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
        File command_repo = temporaryFolder.newFolder("command-repo");
        File windows = new File(command_repo, "windows");
        windows.mkdirs();
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
        File dirWithUnreadableFile = temporaryFolder.newFolder("dirWithUnreadableFile");
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
        return argThat(new ArgumentMatcher<ServerHealthState>() {
            @Override
            public boolean matches(ServerHealthState o) {
                return o.isRealSuccess();
            }

            @Override
            public String toString() {
                return "Expected success health message.";
            }
        });
    }

    private ServerHealthState serverHealthWarningMessageWhichContains(final String expectedPartOfMessage) {
        return argThat(new ArgumentMatcher<ServerHealthState>() {
            @Override
            public boolean matches(ServerHealthState o) {
                String description = o.getDescription();

                boolean isTheMessageWeHaveBeenWaitingFor = description.contains(expectedPartOfMessage);
                if (isTheMessageWeHaveBeenWaitingFor) {
                    assertThat(o.getLogLevel(), is(HealthStateLevel.WARNING));
                }
                return isTheMessageWeHaveBeenWaitingFor;
            }

            public String toString() {
                return "Expected message to contain: " + expectedPartOfMessage;
            }
        });
    }
}
