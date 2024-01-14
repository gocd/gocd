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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.util.ProcessWrapper;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;


public class SvnRemoteRepository {
    private static final String HOST = "127.0.0.1";
    
    public SvnTestRepo repo;
    public ProcessWrapper processWrapper;
    public InMemoryStreamConsumer consumer;
    public int port;

    public SvnRemoteRepository(Path tempDir) throws IOException {
        this.repo = new SvnTestRepo(tempDir);
    }

    public void start() throws Exception {
        if (processWrapper != null) {
            throw new RuntimeException("Cannot start repository twice!");
        }

        port = RandomPort.find(toString());
        CommandLine svnserve = CommandLine.createCommandLine("svnserve")
            .withArgs("-d", "--foreground")
            .withArgs("--listen-host", HOST, "--listen-port", Integer.toString(port))
            .withArgs("-r", repo.projectRepositoryRoot().getCanonicalPath())
            .withEncoding(StandardCharsets.UTF_8);
        consumer = inMemoryConsumer();
        processWrapper = svnserve.execute(consumer, new EnvironmentVariableContext(), null);

        RandomPort.waitForPort(port);

        if (!processWrapper.isRunning()) {
            throw new RuntimeException("Could not run command " + svnserve);
        }
    }

    public void addUser(String username, String password) throws Exception {
        enableAuthentication();
        File passwdFile = new File(repo.projectRepositoryRoot(), "conf/passwd");
        String passwd = String.join(System.lineSeparator(), Files.readAllLines(passwdFile.toPath()));
        if (!(passwd.contains("\n[users]\n"))) {
            passwd = passwd + "\n[users]\n";
        }
        passwd = passwd + String.format("\n%s = %s\n", username, password);
        FileUtils.writeStringToFile(passwdFile, passwd, UTF_8);
    }

    private void enableAuthentication() throws IOException {
        File confFile = new File(repo.projectRepositoryRoot(), "conf/svnserve.conf");
        String passwd = """
                [general]
                anon-access = none
                auth-access = read
                auth-access = write
                password-db = passwd
                """;
        FileUtils.writeStringToFile(confFile, passwd, UTF_8);
    }

    public void stop() throws Exception {
        if (processWrapper != null) {
            Process process = ReflectionUtil.getField(processWrapper, "process");
            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();
            processWrapper.waitForExit();
            processWrapper = null;
        }
    }

    public String getUrl() {
        return String.format("svn://%s:%d", HOST, port);
    }
}
