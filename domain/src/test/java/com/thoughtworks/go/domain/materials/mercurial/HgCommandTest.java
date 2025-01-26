/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HgCommandTest {

    private static final String REVISION_0 = "b61d12de515d82d3a377ae3aae6e8abe516a2651";
    private static final String REVISION_1 = "35ff2159f303ecf986b3650fc4299a6ffe5a14e1";
    private static final String REVISION_2 = "ca3ebb67f527c0ad7ed26b789056823d8b9af23f";

    private File serverRepo;
    private File clientRepo;

    private HgCommand hgCommand;

    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
    private File workingDirectory;
    private File secondBranchWorkingCopy;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException {
        serverRepo = TempDirUtils.createTempDirectoryIn(tempDir, "testHgServerRepo").toFile();
        clientRepo = TempDirUtils.createTempDirectoryIn(tempDir, "testHgClientRepo").toFile();
        secondBranchWorkingCopy = TempDirUtils.createTempDirectoryIn(tempDir, "second").toFile();

        setUpServerRepoFromHgBundle(serverRepo, new File("../domain/src/test/resources/data/repos/hgrepo.hgbundle"));
        workingDirectory = new File(clientRepo.getPath());
        hgCommand = new HgCommand(null, workingDirectory, "default", serverRepo.getAbsolutePath(), null);
        hgCommand.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath()));
    }

    @Test
    public void shouldCloneFromRemoteRepo() {
        assertThat(clientRepo.listFiles().length).isEqualTo(2);
    }

    @Test
    public void shouldCloneWithEscapedRepoUrl() {
        hgCommand.clone(outputStreamConsumer, new UrlArgument(echoingAliasFor("clone")));
        assertNoUnescapedEcho();
    }

    @Test
    public void shouldCloneWithEscapedBranch() {
        hgCommand = new HgCommand(null, workingDirectory, echoingAliasFor("clone"), serverRepo.getAbsolutePath(), null);
        hgCommand.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath()));
        assertNoUnescapedEcho();
    }

    private String echoingAliasFor(String command) {
        return String.format("--config=alias.%s=!echo hello world", command);
    }

    private void assertNoUnescapedEcho() {
        assertThat(outputStreamConsumer.getAllOutput()).doesNotContain("\nhello world\n");
    }

    @Test
    public void shouldGetLatestModifications() {
        List<Modification> actual = hgCommand.latestOneModificationAsModifications();
        assertThat(actual.size()).isEqualTo(1);
        final Modification modification = actual.get(0);
        assertThat(modification.getComment()).isEqualTo("test");
        assertThat(modification.getUserName()).isEqualTo("cruise");
        assertThat(modification.getModifiedFiles().size()).isEqualTo(1);
    }

    @Test
    public void shouldNotIncludeCommitFromAnotherBranchInGetLatestModifications() {
        Modification lastCommit = hgCommand.latestOneModificationAsModifications().get(0);

        makeACommitToSecondBranch();
        hg(workingDirectory, "pull").runOrBomb(null);
        Modification actual = hgCommand.latestOneModificationAsModifications().get(0);
        assertThat(actual).isEqualTo(lastCommit);
        assertThat(actual.getComment()).isEqualTo(lastCommit.getComment());
    }

    @Test
    public void shouldGetModifications() {
        List<Modification> actual = hgCommand.modificationsSince(new StringRevision(REVISION_0));
        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual.get(0).getRevision()).isEqualTo(REVISION_2);
        assertThat(actual.get(1).getRevision()).isEqualTo(REVISION_1);
    }

    @Test
    public void shouldNotGetModificationsFromOtherBranches() {
        makeACommitToSecondBranch();
        hg(workingDirectory, "pull").runOrBomb(null);

        List<Modification> actual = hgCommand.modificationsSince(new StringRevision(REVISION_0));
        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual.get(0).getRevision()).isEqualTo(REVISION_2);
        assertThat(actual.get(1).getRevision()).isEqualTo(REVISION_1);
    }

    @Test
    public void shouldUpdateToSpecificRevision() {
        InMemoryStreamConsumer output = ProcessOutputStreamConsumer.inMemoryConsumer();
        assertThat(output.getStdOut()).isEqualTo("");
        File newFile = new File(clientRepo, "test.txt");
        assertThat(newFile.exists()).isFalse();
        Revision revision = createNewFileAndCheckIn(serverRepo);
        hgCommand.updateTo(revision, output);
        assertThat(output.getStdOut()).isNotEqualTo("");
        assertThat(newFile.exists()).isTrue();
    }

    @Test
    public void shouldUpdateToSpecificRevisionOnGivenBranch() {
        makeACommitToSecondBranch();

        InMemoryStreamConsumer output = ProcessOutputStreamConsumer.inMemoryConsumer();
        File newFile = new File(workingDirectory, "test.txt");
        hgCommand.updateTo(new StringRevision("tip"), output);
        assertThat(newFile.exists()).isFalse();
    }

    @Test
    public void shouldThrowExceptionIfUpdateFails() {
        InMemoryStreamConsumer output =
                ProcessOutputStreamConsumer.inMemoryConsumer();

        // delete repository in order to fail the hg pull command
        assertThat(FileUtils.deleteQuietly(serverRepo)).isTrue();

        // now hg pull will fail and throw an exception
        assertThatThrownBy(() -> hgCommand.updateTo(new StringRevision("tip"), output))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to update to revision [StringRevision[tip]]");
    }

    @Test
    public void shouldGetWorkingUrl() {
        String workingUrl = hgCommand.workingRepositoryUrl().outputAsString();

        assertThat(workingUrl).isEqualTo(serverRepo.getAbsolutePath());
    }

    @Test
    public void shouldCheckConnection() {
        hgCommand.checkConnection(new UrlArgument(serverRepo.getAbsolutePath()));
    }

    @Test
    public void shouldCheckConnectionWithEscapedRepoUrl() {
        assertThatThrownBy(() -> hgCommand.checkConnection(new UrlArgument(echoingAliasFor("id"))))
                .isExactlyInstanceOf(CommandLineException.class)
                .hasMessageContaining("repository --config")
                .hasMessageContaining("not found");
    }

    @Test
    public void shouldThrowExceptionForBadConnection() {
        String url = "http://not-exists";
        HgCommand hgCommand = new HgCommand(null, null, null, null, null);

        assertThatThrownBy(() -> hgCommand.checkConnection(new UrlArgument(url)))
                .isExactlyInstanceOf(CommandLineException.class);
    }

    @Test
    public void shouldCloneOnlyTheSpecifiedBranchAndPointToIt() {
        String branchName = "second";
        HgCommand hg = new HgCommand(null, secondBranchWorkingCopy, branchName, serverRepo.getAbsolutePath(), null);
        hg.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath() + "#" + branchName));

        String currentBranch = hg(secondBranchWorkingCopy, "branch").runOrBomb(null).outputAsString();
        assertThat(currentBranch).isEqualTo(branchName);

        List<String> branches = hg(secondBranchWorkingCopy, "branches").runOrBomb(null).output();
        List<String> branchNames = new ArrayList<>();
        for (String branchDetails : branches) {
            branchNames.add(StringUtils.split(branchDetails, " ")[0]);
        }
        assertThat(branchNames.size()).isEqualTo(2);
        assertThat(branchNames.contains(branchName)).isTrue();
        assertThat(branchNames.contains("default")).isTrue();
    }

    private CommandLine hg(File workingDir, String... arguments) {
        CommandLine hg = CommandLine.createCommandLine("hg").withArgs(arguments).withEncoding(UTF_8);
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
                .withArgs("clone", hgBundleFile.getAbsolutePath(), serverRepo.getAbsolutePath()).withEncoding(UTF_8).runOrBomb(null, input);
    }

    private void makeACommitToSecondBranch() {
        HgCommand hg = new HgCommand(null, secondBranchWorkingCopy, "second", serverRepo.getAbsolutePath(), null);
        hg.clone(outputStreamConsumer, new UrlArgument(serverRepo.getAbsolutePath()));
        createNewFileAndPushUpstream(secondBranchWorkingCopy);
    }
}
