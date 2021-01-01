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
package com.thoughtworks.go.domain.materials.mercurial;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class HgCommandTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File serverRepo;
    private File clientRepo;

    private HgCommand hgCommand;

    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
    private File workingDirectory;
    private static final String REVISION_0 = "b61d12de515d82d3a377ae3aae6e8abe516a2651";
    private static final String REVISION_1 = "35ff2159f303ecf986b3650fc4299a6ffe5a14e1";
    private static final String REVISION_2 = "ca3ebb67f527c0ad7ed26b789056823d8b9af23f";
    private File secondBranchWorkingCopy;

    @Before
    public void setUp() throws IOException {
        serverRepo = temporaryFolder.newFolder("testHgServerRepo");
        clientRepo = temporaryFolder.newFolder("testHgClientRepo");
        secondBranchWorkingCopy = temporaryFolder.newFolder("second");

        setUpServerRepoFromHgBundle(serverRepo, new File("../common/src/test/resources/data/hgrepo.hgbundle"));
        workingDirectory = new File(clientRepo.getPath());
        hgCommand = new HgCommand(null, workingDirectory, "default", serverRepo.getAbsolutePath(), null);
        hgCommand.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath()));
    }

    @Test
    public void shouldCloneFromRemoteRepo() {
        assertThat(clientRepo.listFiles().length > 0, is(true));
    }

    @Test
    public void shouldGetLatestModifications() throws Exception {
        List<Modification> actual = hgCommand.latestOneModificationAsModifications();
        assertThat(actual.size(), is(1));
        final Modification modification = actual.get(0);
        assertThat(modification.getComment(), is("test"));
        assertThat(modification.getUserName(), is("cruise"));
        assertThat(modification.getModifiedFiles().size(), is(1));
    }

    @Test
    public void shouldNotIncludeCommitFromAnotherBranchInGetLatestModifications() throws Exception {
        Modification lastCommit = hgCommand.latestOneModificationAsModifications().get(0);

        makeACommitToSecondBranch();
        hg(workingDirectory, "pull").runOrBomb(null);
        Modification actual = hgCommand.latestOneModificationAsModifications().get(0);
        assertThat(actual, is(lastCommit));
        assertThat(actual.getComment(), is(lastCommit.getComment()));
    }

    @Test
    public void shouldGetModifications() throws Exception {
        List<Modification> actual = hgCommand.modificationsSince(new StringRevision(REVISION_0));
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0).getRevision(), is(REVISION_2));
        assertThat(actual.get(1).getRevision(), is(REVISION_1));
    }

    @Test
    public void shouldNotGetModificationsFromOtherBranches() throws Exception {
        makeACommitToSecondBranch();
        hg(workingDirectory, "pull").runOrBomb(null);

        List<Modification> actual = hgCommand.modificationsSince(new StringRevision(REVISION_0));
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0).getRevision(), is(REVISION_2));
        assertThat(actual.get(1).getRevision(), is(REVISION_1));
    }

    @Test
    public void shouldUpdateToSpecificRevision() {
        InMemoryStreamConsumer output = ProcessOutputStreamConsumer.inMemoryConsumer();
        assertThat(output.getStdOut(), is(""));
        File newFile = new File(clientRepo, "test.txt");
        assertThat(newFile.exists(), is(false));
        Revision revision = createNewFileAndCheckIn(serverRepo);
        hgCommand.updateTo(revision, output);
        assertThat(output.getStdOut(), is(not("")));
        assertThat(newFile.exists(), is(true));
    }

    @Test
    public void shouldUpdateToSpecificRevisionOnGivenBranch() {
        makeACommitToSecondBranch();

        InMemoryStreamConsumer output = ProcessOutputStreamConsumer.inMemoryConsumer();
        File newFile = new File(workingDirectory, "test.txt");
        hgCommand.updateTo(new StringRevision("tip"), output);
        assertThat(newFile.exists(), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfUpdateFails() throws Exception {
        InMemoryStreamConsumer output =
                ProcessOutputStreamConsumer.inMemoryConsumer();

        // delete repository in order to fail the hg pull command
        assertThat(FileUtils.deleteQuietly(serverRepo), is(true));

        // now hg pull will fail and throw an exception
        hgCommand.updateTo(new StringRevision("tip"), output);
    }

    @Test
    public void shouldGetWorkingUrl() {
        String workingUrl = hgCommand.workingRepositoryUrl().outputAsString();

        assertThat(workingUrl, is(serverRepo.getAbsolutePath()));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForBadConnection() throws Exception {
        String url = "http://not-exists";
        HgCommand hgCommand = new HgCommand(null, null, null, null, null);

        hgCommand.checkConnection(new UrlArgument(url));
    }

    @Test
    public void shouldCloneOnlyTheSpecifiedBranchAndPointToIt() {
        String branchName = "second";
        HgCommand hg = new HgCommand(null, secondBranchWorkingCopy, branchName, serverRepo.getAbsolutePath(), null);
        hg.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath() + "#" + branchName));

        String currentBranch = hg(secondBranchWorkingCopy, "branch").runOrBomb(null).outputAsString();
        assertThat(currentBranch, is(branchName));

        List<String> branches = hg(secondBranchWorkingCopy, "branches").runOrBomb(null).output();
        ArrayList<String> branchNames = new ArrayList<>();
        for (String branchDetails : branches) {
            branchNames.add(StringUtils.split(branchDetails, " ")[0]);
        }
        assertThat(branchNames.size(), is(2));
        assertThat(branchNames.contains(branchName), is(true));
        assertThat(branchNames.contains("default"), is(true));
    }

    private CommandLine hg(File workingDir, String... arguments) {
        CommandLine hg = CommandLine.createCommandLine("hg").withArgs(arguments).withEncoding("utf-8");
        hg.setWorkingDir(workingDir);
        return hg;
    }

    private void commit(String message, File workingDir) {
        CommandLine hg = hg(workingDir, "ci", "-u", "cruise-test", "-m", message);
        String[] input = new String[]{};
        hg.runOrBomb(null, input);
    }

    private Revision latestRevisionOf() {
        CommandLine hg = hg(serverRepo, "log", "--limit", "1", "--template", "{node}");
        String[] input = new String[]{};
        return new StringRevision(hg.runOrBomb(null, input).outputAsString());
    }

    private void addremove(File workingDir) {
        CommandLine hg = hg(workingDir, "addremove");
        String[] input = new String[]{};
        hg.runOrBomb(null, input);
    }

    private void createNewFileAndPushUpstream(File workingDir) {
        createNewFileAndCheckIn(workingDir);
        String branchName = hg(workingDir, "branch").runOrBomb(null).outputAsString();
        hg(workingDir, "push", "--rev", branchName).runOrBomb(null);
    }

    private Revision createNewFileAndCheckIn(File directory) {
        try {
            new FileOutputStream(new File(directory, "test.txt")).close();
            addremove(directory);
            commit("created test.txt", directory);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return latestRevisionOf();
    }

    private void setUpServerRepoFromHgBundle(File serverRepo, File hgBundleFile) {
        String[] input = new String[]{};
        CommandLine.createCommandLine("hg")
                .withArgs("clone", hgBundleFile.getAbsolutePath(), serverRepo.getAbsolutePath()).withEncoding("utf-8").runOrBomb(null, input);
    }

    private void makeACommitToSecondBranch() {
        HgCommand hg = new HgCommand(null, secondBranchWorkingCopy, "second", serverRepo.getAbsolutePath(), null);
        hg.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath()));
        createNewFileAndPushUpstream(secondBranchWorkingCopy);
    }
}
