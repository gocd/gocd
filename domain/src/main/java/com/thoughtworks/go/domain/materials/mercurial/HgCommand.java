/*
 * Copyright 2022 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.materials.SCMCommand;
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombUnless;
import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;

public class HgCommand extends SCMCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(HgCommand.class);
    private final File workingDir;
    private static String templatePath;
    private final String branch;
    private final String url;
    private final List<SecretString> secrets;


    public HgCommand(String materialFingerprint, File workingDir, String branch, String url, List<SecretString> secrets) {
        super(materialFingerprint);
        this.workingDir = workingDir;
        this.branch = branch;
        this.url = url;
        this.secrets = secrets != null ? secrets : new ArrayList<>();
    }


    private boolean pull(ConsoleOutputStreamConsumer outputStreamConsumer) {
        CommandLine hg = hg("pull", "-b", branch, "--config", String.format("paths.default=%s", url));
        return execute(hg, outputStreamConsumer) == 0;
    }

    public HgVersion version() {
        CommandLine hg = createCommandLine("hg").withArgs("version").withEncoding("utf-8");
        String hgOut = execute(hg, new NamedProcessTag("hg version check")).outputAsString();
        return HgVersion.parse(hgOut);
    }


    public int clone(ConsoleOutputStreamConsumer outputStreamConsumer, UrlArgument repositoryUrl) {
        CommandLine hg = createCommandLine("hg").withArgs("clone").withArg("-b").withArg(branch).withArg(repositoryUrl)
                .withArg(workingDir.getAbsolutePath()).withNonArgSecrets(secrets).withEncoding("utf-8");
        return execute(hg, outputStreamConsumer);
    }

    public void checkConnection(UrlArgument repositoryURL) {
        execute(createCommandLine("hg").withArgs("id", "--id").withArg(repositoryURL).withNonArgSecrets(secrets).withEncoding("utf-8"), new NamedProcessTag(repositoryURL.forDisplay()));
    }

    public void updateTo(Revision revision, ConsoleOutputStreamConsumer outputStreamConsumer) {
        if (!pull(outputStreamConsumer) || !update(revision, outputStreamConsumer)) {
            bomb(format("Unable to update to revision [%s]", revision));
        }
    }

    private boolean update(Revision revision, ConsoleOutputStreamConsumer outputStreamConsumer) {
        CommandLine hg = hg("update", "--clean", "-r", revision.getRevision());
        return execute(hg, outputStreamConsumer) == 0;
    }

    public void add(ConsoleOutputStreamConsumer outputStreamConsumer, File file) {
        CommandLine hg = hg("add", file.getAbsolutePath());
        execute(hg, outputStreamConsumer);
    }

    public void commit(ConsoleOutputStreamConsumer consumer, String comment, String username) {
        CommandLine hg = hg("commit", "-m", comment, "-u", username);
        execute(hg, consumer);
    }

    public void push(ConsoleOutputStreamConsumer consumer) {
        CommandLine hg = hg("push");
        execute(hg, consumer);
    }

    public List<Modification> latestOneModificationAsModifications() {
        return findRecentModifications(1);
    }

    private String templatePath() {
        if (templatePath == null) {
            String file = HgCommand.class.getResource("/hg.template").getFile();
            try {
                templatePath = URLDecoder.decode(new File(file).getAbsolutePath(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                templatePath = URLDecoder.decode(new File(file).getAbsolutePath());
            }
        }
        return templatePath;
    }

    List<Modification> findRecentModifications(int count) {
        // Currently impossible to check modifications on a remote repository.
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        bombUnless(pull(consumer), "Failed to run hg pull command: " + consumer.getAllOutput());
        CommandLine hg = hg("log", "--limit", String.valueOf(count), "-b", branch, "--style", templatePath());
        return new HgModificationSplitter(execute(hg)).modifications();
    }

    public List<Modification> modificationsSince(Revision revision) {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        bombUnless(pull(consumer), "Failed to run hg pull command: " + consumer.getAllOutput());
        CommandLine hg = hg("log",
                "-r", "tip:" + revision.getRevision(),
                "-b", branch,
                "--style", templatePath());
        return new HgModificationSplitter(execute(hg)).filterOutRevision(revision);
    }

    public ConsoleResult workingRepositoryUrl() {
        CommandLine hg = hg("showconfig", "paths.default");

        final ConsoleResult result = execute(hg);
        LOGGER.trace("Current repository url of [{}]: {}", workingDir, result.outputForDisplayAsString());
        LOGGER.trace("Target repository url: {}", url);
        return result;
    }

    private CommandLine hg(String... arguments) {
        return createCommandLine("hg").withArgs(arguments).withNonArgSecrets(secrets).withWorkingDir(workingDir).withEncoding("UTF-8");
    }

    private static ConsoleResult execute(CommandLine hgCmd, NamedProcessTag processTag) {
        return hgCmd.runOrBomb(processTag);
    }

    private ConsoleResult execute(CommandLine hgCmd) {
        return runOrBomb(hgCmd);
    }

    private int execute(CommandLine hgCmd, ConsoleOutputStreamConsumer outputStreamConsumer) {
        return run(hgCmd, outputStreamConsumer);
    }
}
