/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.perforce.P4Client;
import com.thoughtworks.go.domain.materials.perforce.PerforceFixture;
import com.thoughtworks.go.util.ProcessWrapper;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.helper.MaterialConfigsMother.p4;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.apache.commons.io.FileUtils.copyDirectory;

public class P4TestRepo extends TestRepo {
    protected File tempRepo;
    private ProcessWrapper p4dProcess;
    private final int port;
    private String user;
    private String password;
    private String clientName;
    private boolean useTickets;
    private File clientFolder;

    private P4TestRepo(int port, String repoPrototype, String user, String password, String clientName,
                       boolean useTickets, TemporaryFolder temporaryFolder, File clientFolder) throws IOException {
        super(temporaryFolder);
        this.port = port;
        this.user = user;
        this.password = password;
        this.clientName = clientName;
        this.useTickets = useTickets;
        tempRepo = temporaryFolder.newFolder();
        this.clientFolder = clientFolder;
        try {
            copyDirectory(new File(repoPrototype), tempRepo);
        } catch (IOException e) {
            bomb(e);
        }
    }

    public void onSetup() throws Exception {
        p4dProcess = startP4dInRepo(tempRepo);
        waitForP4dToStartup();
    }

    @Override
    public Material material() {
        return createMaterial();
    }

    private void waitForP4dToStartup() throws Exception {
        CommandLine command = createCommandLine("p4").withArgs("-p", serverAndPort(), "info").withEncoding("utf-8");
        command.waitForSuccess(60 * 1000);
    }

    public void onTearDown() {
        Process process = (Process) ReflectionUtil.getField(p4dProcess, "process");
        process.destroy();
        FileUtils.deleteQuietly(tempRepo);
        FileUtils.deleteQuietly(clientFolder);
    }

    public String projectRepositoryUrl() {
        return serverAndPort();
    }


    public List<Modification> checkInOneFile(String fileName, String comment) throws Exception {
        return checkInOneFile(createMaterial(), fileName, comment);
    }

    private P4Material createMaterial() {
        P4Material p4Material1 = new P4Material(serverAndPort(), "//depot/... //something/...");
        p4Material1.setFolder("anything");
        return p4Material1;
    }

    public List<Modification> latestModification() throws IOException {
        File workingDir = temporaryFolder.newFolder();
        return createMaterial().latestModification(workingDir, new TestSubprocessExecutionContext());
    }

    public void stop() {
        CommandLine command = createCommandLine("p4").withArgs("-p", serverAndPort(), "admin", "stop").withEncoding("utf-8");
        ProcessOutputStreamConsumer outputStreamConsumer = inMemoryConsumer();
        command.run(outputStreamConsumer, null);
    }

    private ProcessWrapper startP4dInRepo(File tempRepo) throws IOException, CheckedCommandLineException {
        CommandLine command = createCommandLine("p4d").withArgs("-C0", "-r", tempRepo.getAbsolutePath(), "-p", String.valueOf(port)).withEncoding("utf-8");
        ProcessOutputStreamConsumer outputStreamConsumer = inMemoryConsumer();
        return command.execute(outputStreamConsumer, new EnvironmentVariableContext(), null);
    }

    public String serverAndPort() {
        return "localhost:" + port;
    }

    public static P4TestRepo createP4TestRepo(TemporaryFolder temporaryFolder, File clientFolder) throws IOException {
        String repo = "../common/src/test/resources/data/p4repo";
        if (SystemUtils.IS_OS_WINDOWS) {
            repo = "../common/src/test/resources/data/p4repoWindows";
        }
        return new P4TestRepo(RandomPort.find("P4TestRepo"), repo, "cceuser", null, PerforceFixture.DEFAULT_CLIENT_NAME, false, temporaryFolder, clientFolder);
    }

    public static P4TestRepo createP4TestRepoWithTickets(TemporaryFolder temporaryFolder, File clientFolder) throws IOException {
        String repo = "../common/src/test/resources/data/p4TicketedRepo";
        if (SystemUtils.IS_OS_WINDOWS) {
            repo = "../common/src/test/resources/data/p4TicketedRepoWindows";
        }
        return new P4TestRepo(RandomPort.find("P4TestRepoWithTickets"), repo, "cceuser", "1234abcd", PerforceFixture.DEFAULT_CLIENT_NAME, true, temporaryFolder, clientFolder);
    }

    public P4Material material(String p4view) {
        P4Material p4Material = new P4Material(serverAndPort(), p4view);
        p4Material.setUsername(user);
        p4Material.setPassword(password);
        p4Material.setUseTickets(useTickets);
        return p4Material;
    }

    public P4MaterialConfig materialConfig(String p4view) {
        P4MaterialConfig p4MaterialConfig = p4(serverAndPort(), p4view);
        p4MaterialConfig.setConfigAttributes(Collections.singletonMap(P4MaterialConfig.USERNAME, user));
        p4MaterialConfig.setPassword(password);
        p4MaterialConfig.setUseTickets(useTickets);
        return p4MaterialConfig;
    }

    public P4Client createClient() throws Exception {
        return createClientWith(clientName, null);
    }

    public P4Client createClientWith(String clientName, String view) throws Exception {
        String p4view = view == null ? "\n\t//depot/... //" + clientName + "/..." : "\n\t" + view;
        P4Client p4Client = P4Client.fromServerAndPort(null, serverAndPort(), user, password, clientName, useTickets, clientFolder, p4view, inMemoryConsumer(), true);
        return p4Client;
    }

    public void checkInOneFile(P4Material p4Material1, String fileName) throws Exception {
        String comment = "added file " + fileName;
        checkInOneFile(p4Material1, fileName, comment);
    }

    private List<Modification> checkInOneFile(P4Material p4Material1, String fileName, String comment) throws Exception {
        File workingDir = temporaryFolder.newFolder();

        P4Client client = createClient();

        client.client(clientConfig(clientName, workingDir), inMemoryConsumer(), true);

        File dir = new File(workingDir, p4Material1.getFolder());
        dir.mkdirs();
        checkout(dir);

        File newFile = new File(new File(workingDir, p4Material1.getFolder()), fileName);
        newFile.createNewFile();

        add(workingDir, newFile.getAbsolutePath());
        commit(comment, workingDir);

        client.removeClient();

        List<Modification> modifications = p4Material1.latestModification(workingDir, new TestSubprocessExecutionContext());

        return modifications;
    }

    protected static String clientConfig(String clientName, File clientFolder) {
        return "Client: " + clientName + "\n\n"
                + "Owner: cruise\n\n"
                + "Root: " + clientFolder.getAbsolutePath() + "\n\n"
                + "Options: rmdir\n\n"
                + "LineEnd: local\n\n"
                + "View:\n"
                + "\t//depot/... //" + clientName + "/...\n";

    }

    private void checkout(File workingDir) {
        runP4(workingDir, "-c", clientName, "sync");
    }

    private void add(File workingdir, String fileName) {
        runP4(workingdir, "-c", clientName, "add", fileName);
    }

    private void commit(String message, File workingDir) {
        runP4(workingDir, "-c", clientName, "-d", workingDir.getAbsolutePath(), "submit", "-d", message);
    }

    private void runP4(File workingDir, String... args) {
        InMemoryStreamConsumer consumer2 = inMemoryConsumer();
        List<String> arrays = new ArrayList<>();
        arrays.add("-p");
        arrays.add(serverAndPort());
        arrays.addAll(Arrays.asList(args));

        CommandLine command = createCommandLine("p4").withWorkingDir(workingDir).withArgs("-p", serverAndPort()).withArgs(args).withEncoding("utf-8");
        command.run(consumer2, null);
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

}

