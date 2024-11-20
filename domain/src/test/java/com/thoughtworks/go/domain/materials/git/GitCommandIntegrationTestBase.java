/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
abstract class GitCommandIntegrationTestBase {
    protected static final String TEST_BRANCH = "foo";
    protected static final String TEST_SUBMODULE = "submodule-1";
    @SystemStub
    protected SystemProperties systemProperties;
    protected GitCommand git;
    protected String repoUrl;
    protected File repoLocation;
    protected GitTestRepo gitRepo;
    protected File gitLocalRepoDir;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws IOException {
        gitRepo = new GitTestRepo(tempDir);
        gitLocalRepoDir = createTempWorkingDirectory();
        git = new GitCommand(null, gitLocalRepoDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        repoLocation = gitRepo.gitRepository();
        repoUrl = gitRepo.projectRepositoryUrl();
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        int returnCode = git.cloneWithNoCheckout(outputStreamConsumer, repoUrl);
        if (returnCode > 0) {
            fail(outputStreamConsumer.getAllOutput());
        }
        systemProperties.set(GitCommand.GIT_SUBMODULE_ALLOW_FILE_PROTOCOL, "Y");
    }

    @AfterEach
    void teardown() {
        unsetColoring();
        unsetLogDecoration();
    }

    protected File createTempWorkingDirectory() throws IOException {
        return TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
    }

    protected void gitInRepo(String... args) {
        git_C(gitLocalRepoDir, args);
    }

    /**
     * Like {@code git -C <dir> command [args...]}
     *
     * @param dir  the directory to set as CWD
     * @param args the args to pass to {@code git}
     * @return a {@link ConsoleResult}
     */
    protected ConsoleResult git_C(File dir, String... args) {
        CommandLine commandLine = CommandLine.createCommandLine("git");
        commandLine.withArgs(args);
        commandLine.withEncoding(UTF_8);
        assertTrue(dir.exists());
        commandLine.setWorkingDir(dir);
        return commandLine.runOrBomb(true, null);
    }

    protected void setColoring() {
        gitInRepo("config", "color.diff", "always");
        gitInRepo("config", "color.status", "always");
        gitInRepo("config", "color.interactive", "always");
        gitInRepo("config", "color.branch", "always");
    }

    protected void setLogDecoration() {
        gitInRepo("config", "log.decorate", "true");
    }

    private void unsetLogDecoration() {
        gitInRepo("config", "log.decorate", "off");
    }

    private void unsetColoring() {
        gitInRepo("config", "color.diff", "auto");
        gitInRepo("config", "color.status", "auto");
        gitInRepo("config", "color.interactive", "auto");
        gitInRepo("config", "color.branch", "auto");
    }

    protected void assertWorkingCopyNotCheckedOut() {
        assertArrayEquals(new File[]{new File(gitLocalRepoDir, ".git")}, gitLocalRepoDir.listFiles());
    }
}
