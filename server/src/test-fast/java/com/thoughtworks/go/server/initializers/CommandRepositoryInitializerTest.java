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
package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.server.domain.Version;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class CommandRepositoryInitializerTest {
    private SystemEnvironment systemEnvironment;
    private ZipInputStream zipInputStream;
    private CommandRepositoryInitializer spy;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ZipUtil zipUtil;
    private ServerHealthService serverHealthService;
    private String VERSION_FILE = "version.txt";

    @Before
    public void setUp() {
        serverHealthService = mock(ServerHealthService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        zipUtil = mock(ZipUtil.class);
        zipInputStream = mock(ZipInputStream.class);

        when(systemEnvironment.get(SystemEnvironment.VERSION_FILE_IN_DEFAULT_COMMAND_REPOSITORY)).thenReturn("version.txt");

        CommandRepositoryInitializer initializer = new CommandRepositoryInitializer(systemEnvironment, zipUtil, serverHealthService);
        spy = spy(initializer);
    }

    @Test
    public void shouldReplaceCommandRepositoryIfExistingVersionIsLessThanPackagedVersion() throws Exception {
        File defaultDirectory = getAFile(true);
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);

        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();
        doReturn("13.1=  50   ").when(zipUtil).getFileContentInsideZip(zipInputStream, VERSION_FILE);
        doReturn(new Version("13.1=9")).when(spy).getExistingVersion(defaultDirectory);
        doNothing().when(spy).usePackagedCommandRepository(zipInputStream, defaultDirectory);

        spy.initialize();

        verify(spy).usePackagedCommandRepository(zipInputStream, defaultDirectory);
        verify(spy, times(2)).getPackagedRepositoryZipStream();
    }

    @Test
    public void shouldReplaceCommandRepositoryIfExistingVersionIsLessThanPackagedVersionIrrespectiveOfGoVersion() throws Exception {
        File defaultDirectory = getAFile(true);
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);

        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();
        doReturn("13.1=  50   ").when(zipUtil).getFileContentInsideZip(zipInputStream, VERSION_FILE);
        doReturn(new Version("  textHereDoesNotMatter = 9 ")).when(spy).getExistingVersion(defaultDirectory);
        doNothing().when(spy).usePackagedCommandRepository(zipInputStream, defaultDirectory);

        spy.initialize();

        verify(spy).usePackagedCommandRepository(zipInputStream, defaultDirectory);
        verify(spy, times(2)).getPackagedRepositoryZipStream();
    }

    @Test
    public void shouldRetainCommandRepositoryIfExistingVersionIsEqualToPackagedVersion() throws Exception {
        File defaultDirectory = getAFile(true);
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);

        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();
        doReturn("13.1=50").when(zipUtil).getFileContentInsideZip(zipInputStream, VERSION_FILE);
        doReturn(new Version("13.1=50")).when(spy).getExistingVersion(defaultDirectory);

        spy.initialize();

        verify(spy, never()).usePackagedCommandRepository(zipInputStream, defaultDirectory);
        verify(spy, times(1)).getPackagedRepositoryZipStream();
    }

    @Test
    public void shouldRetainCommandRepositoryIfExistingVersionIsGreaterThanPackagedVersion() throws Exception {
        File defaultDirectory = getAFile(true);
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);

        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();

        doReturn("13.1=45").when(zipUtil).getFileContentInsideZip(zipInputStream, VERSION_FILE);
        doReturn(new Version("13.1=50")).when(spy).getExistingVersion(defaultDirectory);

        spy.initialize();

        verify(spy, never()).usePackagedCommandRepository(zipInputStream, defaultDirectory);
        verify(spy, times(1)).getPackagedRepositoryZipStream();
    }

    @Test
    public void shouldExpandPackagedCommandRepositoryWhenNoneExists() throws IOException {
        File defaultDirectory = getAFile(false);
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);

        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();

        spy.initialize();

        verify(spy, times(1)).usePackagedCommandRepository(zipInputStream, defaultDirectory);
        verify(spy, times(1)).getPackagedRepositoryZipStream();
        verify(zipUtil, never()).getFileContentInsideZip(zipInputStream, VERSION_FILE);
        verify(spy, never()).getExistingVersion(defaultDirectory);
    }

    @Test
    public void shouldAddToServerHealthMessageWhen_CheckingIfRepoShouldBeReplacedThrowsAnException() throws IOException {
        File defaultDirectory = getAFile(true);
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);
        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();

        doThrow(new IOException("Something went wrong")).when(zipUtil).getFileContentInsideZip(zipInputStream, VERSION_FILE);

        spy.initialize();

        String errorMessage = "Unable to upgrade command repository located at " + defaultDirectory.getAbsolutePath() + ". Message: Something went wrong";
        verify(serverHealthService).update(serverHealthErrorMessageWhichContains(errorMessage));
        verify(spy, never()).usePackagedCommandRepository(zipInputStream, defaultDirectory);
    }

    @Test
    public void shouldAddToServerHealthMessageWhenUsingOfPackagedVersionThrowsException() throws IOException {
        File defaultDirectory = getAFile(false);
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);
        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();

        doThrow(new IOException("Something went wrong")).when(spy).usePackagedCommandRepository(zipInputStream, defaultDirectory);

        spy.initialize();

        String errorMessage = "Unable to upgrade command repository located at " + defaultDirectory.getAbsolutePath() + ". Message: Something went wrong";
        verify(serverHealthService).update(serverHealthErrorMessageWhichContains(errorMessage));
        verify(spy, times(1)).usePackagedCommandRepository(zipInputStream, defaultDirectory);
    }

    @Test
    public void shouldUsePackagedRepositoryIfNoVersionFileIsFoundInInstalledDirectory() throws IOException {
        File defaultDirectory = temporaryFolder.newFolder("default");
        when(systemEnvironment.getDefaultCommandRepository()).thenReturn(defaultDirectory);

        doReturn(zipInputStream).when(spy).getPackagedRepositoryZipStream();
        doReturn("13.1=50").when(zipUtil).getFileContentInsideZip(zipInputStream, VERSION_FILE);

        doNothing().when(spy).usePackagedCommandRepository(zipInputStream, defaultDirectory);

        spy.initialize();

        verify(spy).usePackagedCommandRepository(zipInputStream, defaultDirectory);
    }

    private File getAFile(final boolean exists) {
        File file = mock(File.class);
        when(file.exists()).thenReturn(exists);
        return file;
    }

    private ServerHealthState serverHealthErrorMessageWhichContains(final String expectedPartOfMessage) {
        return argThat(new ArgumentMatcher<ServerHealthState>() {
            @Override
            public boolean matches(ServerHealthState o) {
                ServerHealthState serverHealthState = (ServerHealthState) o;
                String description = serverHealthState.getDescription();

                boolean isTheMessageWeHaveBeenWaitingFor = description.contains(expectedPartOfMessage);
                if (isTheMessageWeHaveBeenWaitingFor) {
                    assertThat(serverHealthState.getLogLevel(), is(HealthStateLevel.ERROR));
                }
                return isTheMessageWeHaveBeenWaitingFor;
            }

            @Override
            public String toString() {
                return "Expected message to contain: " + expectedPartOfMessage;
            }
        });
    }
}
