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

package com.thoughtworks.go.domain.materials.mercurial;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.FileUtil.deleteFolder;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HgCommandTest {
    private static File serverRepo;
    private static File clientRepo;

    private HgCommand hgCommand;

    private List<File> foldersToDelete = new ArrayList<File>();
    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
    private File workingDirectory;
    private static final String REVISION_0 = "b61d12de515d82d3a377ae3aae6e8abe516a2651";
    private static final String REVISION_1 = "35ff2159f303ecf986b3650fc4299a6ffe5a14e1";
    private static final String REVISION_2 = "ca3ebb67f527c0ad7ed26b789056823d8b9af23f";
    private File secondBranchWorkingCopy;

    @Before
    public void setUp() throws IOException {
        serverRepo = createTmpFolder("testHgServerRepo");
        clientRepo = createTmpFolder("testHgClientRepo");
        secondBranchWorkingCopy = createTmpFolder("second");

        setUpServerRepoFromHgBundle(serverRepo, new File("../common/test-resources/unit/data/hgrepo.hgbundle"));
        workingDirectory = new File(clientRepo.getPath());
        hgCommand = new HgCommand(null, workingDirectory, "default", serverRepo.getAbsolutePath());
        hgCommand.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath()));
    }

    @After
    public void tearDown() throws IOException {
        for (File folder : foldersToDelete) {
            if (folder.exists()) {
                deleteFolder(folder);
            }
        }
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
        assertThat(deleteFolder(serverRepo), is(true));

        // now hg pull will fail and throw an exception
        hgCommand.updateTo(new StringRevision("tip"), output);
    }

    @Test
    public void shouldGetWorkingUrl() {
        String workingUrl = hgCommand.workingRepositoryUrl().outputAsString();

        assertThat(workingUrl, is(serverRepo.getAbsolutePath()));
    }

    @Test
    @Ignore("super slow test")
    public void shouldProvideDetailedErrorMsgWhenPullFailsDueToLock() throws IOException {
        addLockTo(workingDirectory);
        try {
            hgCommand.findRecentModifications(1);
            fail("should have thrown with detailed error message when pull fails");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("waiting for lock on repository"));
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForBadConnection() throws Exception {
        String url = "http://not-exists";
        HgCommand.checkConnection(new UrlArgument(url));
    }

    @Test
    public void shouldCloneOnlyTheSpecifiedBranchAndPointToIt() {
        String branchName = "second";
        HgCommand hg = new HgCommand(null, secondBranchWorkingCopy, branchName, serverRepo.getAbsolutePath());
        hg.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath() + "#" + branchName));

        String currentBranch = hg(secondBranchWorkingCopy, "branch").runOrBomb(null).outputAsString();
        assertThat(currentBranch, is(branchName));

        List<String> branches = hg(secondBranchWorkingCopy, "branches").runOrBomb(null).output();
        ArrayList<String> branchNames = new ArrayList<String>();
        for (String branchDetails : branches) {
            branchNames.add(StringUtils.split(branchDetails, " ")[0]);
        }
        assertThat(branchNames.size(), is(2));
        assertThat(branchNames.contains(branchName), is(true));
        assertThat(branchNames.contains("default"), is(true));
    }

    private void addLockTo(File hgRepoRootDir) throws IOException {
        File lock = new File(hgRepoRootDir, ".hg/store/lock");
        FileUtils.touch(lock);
    }

    private CommandLine hg(File workingDir, String... arguments) {
        CommandLine hg = CommandLine.createCommandLine("hg").withArgs(arguments);
        hg.setWorkingDir(workingDir);
        return hg;
    }

    private File createTmpFolder(String folderName) {
        return new File(TestFileUtil.createUniqueTempFolder(folderName), "repo");
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
                .withArgs("clone", hgBundleFile.getAbsolutePath(), serverRepo.getAbsolutePath()).runOrBomb(null, input);
    }

    private void makeACommitToSecondBranch() {
        HgCommand hg = new HgCommand(null, secondBranchWorkingCopy, "second", serverRepo.getAbsolutePath());
        hg.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath()));
        createNewFileAndPushUpstream(secondBranchWorkingCopy);
    }
}
