/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.tfssdk14;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.tfssdk14.wrapper.GoTfsVersionControlClient;
import com.thoughtworks.go.tfssdk14.wrapper.GoTfsWorkspace;
import com.thoughtworks.go.util.command.StringArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

class TfsSDKCommandTest {

    private TfsSDKCommand tfsCommand;
    private final String DOMAIN = "domain";
    private final String USERNAME = "username";
    private final String PASSWORD = "password";
    private final String TFS_COLLECTION = "http://some.repo.local:8000/";
    private final String TFS_PROJECT = "$/project_path";
    private final String TFS_WORKSPACE = "workspace";
    private GoTfsVersionControlClient client;
    private TFSTeamProjectCollection collection;

    @BeforeEach
    void setUp() {
        client = mock(GoTfsVersionControlClient.class);
        collection = mock(TFSTeamProjectCollection.class);
        tfsCommand = new TfsSDKCommand(client, collection, null, new StringArgument(TFS_COLLECTION), DOMAIN, USERNAME, PASSWORD, TFS_WORKSPACE, TFS_PROJECT);
    }

    @Test
    void shouldGetLatestModifications() {
        Changeset[] changeSets = getChangeSets(42);
        when(client.queryHistory(TFS_PROJECT, null, 1)).thenReturn(changeSets);
        TfsSDKCommand spy = spy(tfsCommand);
        doReturn(null).when(spy).getModifiedFiles(changeSets[0]);

        assertThat(spy.latestModification(null).isEmpty()).isFalse();

        verify(client).queryHistory(TFS_PROJECT, null, 1);
        verify(spy).getModifiedFiles(changeSets[0]);
    }

    @Test
    void shouldCheckConnectionSuccessfullyIfAllCredentialsAreValid() {
        Changeset[] changeSets = getChangeSets(42);
        when(client.queryHistory(TFS_PROJECT, null, 1)).thenReturn(changeSets);
        TfsSDKCommand spy = spy(tfsCommand);
        doReturn(null).when(spy).getModifiedFiles(changeSets[0]);
        try {
            spy.checkConnection();
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
        verify(client).queryHistory(TFS_PROJECT, null, 1);
        verify(spy).getModifiedFiles(changeSets[0]);
    }

    @Test
    void shouldThrowExceptionDuringCheckConnectionIfInvalid() {
        when(client.queryHistory(TFS_PROJECT, null, 1)).thenThrow(new RuntimeException("could not connect"));
        try {
            tfsCommand.checkConnection();
            fail("should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Failed while checking connection using Url: http://some.repo.local:8000/, Project Path: $/project_path, Username: username, Domain: domain, Root Cause: could not connect");
        }
        verify(client).queryHistory(TFS_PROJECT, null, 1);
    }

    @Test
    void shouldReturnChangeSetsFromAPreviouslyKnownRevisionUptilTheLatest() {
        Changeset[] changeSets = getChangeSets(42);
        when(client.queryHistory(eq(TFS_PROJECT), or(isNull(), any(ChangesetVersionSpec.class)), anyInt())).thenReturn(changeSets);
        TfsSDKCommand spy = spy(tfsCommand);
        doReturn(null).when(spy).getModifiedFiles(changeSets[0]);

        List<Modification> modifications = spy.modificationsSince(null, new StringRevision("2"));

        assertThat(modifications.isEmpty()).isFalse();

        verify(client, times(2)).queryHistory(eq(TFS_PROJECT), or(isNull(), any(ChangesetVersionSpec.class)), anyInt());
    }

    @Test
    void shouldCreateWorkspaceAndMapDirectory() throws Exception {
        File workingDirectory = mock(File.class);
        when(workingDirectory.exists()).thenReturn(false);
        when(workingDirectory.getCanonicalPath()).thenReturn("/some-random-path/");
        GoTfsWorkspace[] workspaces = {};
        when(client.queryWorkspaces(TFS_WORKSPACE, USERNAME)).thenReturn(workspaces);
        GoTfsWorkspace workspace = mock(GoTfsWorkspace.class);
        when(client.createWorkspace(TFS_WORKSPACE)).thenReturn(workspace);
        when(workspace.isLocalPathMapped("/some-random-path/")).thenReturn(false);
        doNothing().when(workspace).createWorkingFolder(any(com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder.class));
        TfsSDKCommand spy = spy(tfsCommand);
        doNothing().when(spy).retrieveFiles(workingDirectory, null);

        spy.checkout(workingDirectory, null);

        verify(client, times(1)).queryWorkspaces(TFS_WORKSPACE, USERNAME);
        verify(client, times(1)).createWorkspace(TFS_WORKSPACE);
        verify(workspace, times(1)).isLocalPathMapped(anyString());
        verify(workspace, times(1)).createWorkingFolder(any(com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder.class));
        verify(spy).retrieveFiles(workingDirectory, null);
    }

    @Test
    void shouldOnlyMapDirectoryAndNotCreateAWorkspaceIfWorkspaceIsAlreadyCreated() throws Exception {
        File workingDirectory = mock(File.class);
        when(workingDirectory.exists()).thenReturn(false);
        when(workingDirectory.getCanonicalPath()).thenReturn("/some-random-path/");
        GoTfsWorkspace workspace = mock(GoTfsWorkspace.class);
        GoTfsWorkspace[] workspaces = {workspace};
        when(client.queryWorkspaces(TFS_WORKSPACE, USERNAME)).thenReturn(workspaces);
        when(workspace.isLocalPathMapped("/some-random-path/")).thenReturn(false);
        doNothing().when(workspace).createWorkingFolder(any(com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder.class));
        TfsSDKCommand spy = spy(tfsCommand);
        doNothing().when(spy).retrieveFiles(workingDirectory, null);

        spy.checkout(workingDirectory, null);

        verify(client, times(1)).queryWorkspaces(TFS_WORKSPACE, USERNAME);
        verify(client, never()).createWorkspace(TFS_WORKSPACE);
        verify(workspace, times(1)).isLocalPathMapped("/some-random-path/");
        verify(workspace, times(1)).createWorkingFolder(any(com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder.class));
        verify(spy).retrieveFiles(workingDirectory, null);
    }

    @Test
    void shouldThrowUpWhenUrlIsInvalid() throws Exception {
        TfsSDKCommand tfsCommandForInvalidCollection = new TfsSDKCommand(null, new StringArgument("invalid_url"), DOMAIN, USERNAME, PASSWORD, TFS_WORKSPACE, TFS_PROJECT);
        try {
            tfsCommandForInvalidCollection.init();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Unable to connect to TFS Collection invalid_url java.lang.RuntimeException: [TFS] Failed when converting the url string to a uri: invalid_url, Project Path: $/project_path, Username: username, Domain: domain");
        }
    }

    @Test
    void shouldCheckoutAllFilesWhenWorkingDirectoryIsDeleted() throws Exception {
        File workingDirectory = mock(File.class);
        when(workingDirectory.exists()).thenReturn(false);
        when(workingDirectory.getCanonicalPath()).thenReturn("canonical_path");
        when(workingDirectory.listFiles()).thenReturn(null);
        TfsSDKCommand spy = spy(tfsCommand);
        doNothing().when(spy).initializeWorkspace(workingDirectory);
        GoTfsWorkspace workspace = mock(GoTfsWorkspace.class);
        when(client.queryWorkspace(TFS_WORKSPACE, USERNAME)).thenReturn(workspace);
        doNothing().when(workspace).get(any(GetRequest.class), eq(GetOptions.GET_ALL));

        spy.checkout(workingDirectory, null);

        verify(workingDirectory).getCanonicalPath();
        verify(workingDirectory).listFiles();
        verify(workspace).get(any(GetRequest.class), eq(GetOptions.GET_ALL));
    }

    @Test
    void should_GetLatestRevisions_WhenCheckingOutToLaterRevision() throws Exception {
        File workingDirectory = mock(File.class);
        when(workingDirectory.exists()).thenReturn(false);
        when(workingDirectory.getCanonicalPath()).thenReturn("canonical_path");
        File[] checkedOutFiles = {mock(File.class)};
        when(workingDirectory.listFiles()).thenReturn(checkedOutFiles);
        TfsSDKCommand spy = spy(tfsCommand);
        doNothing().when(spy).initializeWorkspace(workingDirectory);
        GoTfsWorkspace workspace = mock(GoTfsWorkspace.class);
        when(client.queryWorkspace(TFS_WORKSPACE, USERNAME)).thenReturn(workspace);
        doNothing().when(workspace).get(any(GetRequest.class), eq(GetOptions.NONE));

        spy.checkout(workingDirectory, null);

        verify(workingDirectory).getCanonicalPath();
        verify(workingDirectory).listFiles();
        verify(workspace).get(any(GetRequest.class), eq(GetOptions.NONE));
    }

    @Test
    void shouldClearWorkingDirectoryBeforeCheckingOut() {
        File workingDirectory = mock(File.class);
        when(workingDirectory.exists()).thenReturn(true);
        TfsSDKCommand spy = spy(tfsCommand);
        doNothing().when(spy).initializeWorkspace(workingDirectory);
        doNothing().when(spy).retrieveFiles(workingDirectory, null);

        spy.checkout(workingDirectory, null);

        verify(workingDirectory).exists();
    }

    @Test
    void shouldDeleteWorkspace() {
        GoTfsWorkspace workspace = mock(GoTfsWorkspace.class);
        when(client.queryWorkspace(TFS_WORKSPACE, USERNAME)).thenReturn(workspace);
        doNothing().when(client).deleteWorkspace(workspace);

        tfsCommand.deleteWorkspace();

        verify(client).queryWorkspace(TFS_WORKSPACE, USERNAME);
        verify(client).deleteWorkspace(workspace);
    }

    @Test
    void destroyShouldCloseClientAndCollection() {
        doNothing().when(client).close();
        doNothing().when(collection).close();

        tfsCommand.destroy();

        verify(client).close();
        verify(collection).close();
    }

    private Changeset[] getChangeSets(int changeSetID) {
        Changeset oneChangeSet = new Changeset("owner", "comment", null, null);
        oneChangeSet.setChangesetID(changeSetID);
        return new Changeset[]{oneChangeSet};
    }

}
