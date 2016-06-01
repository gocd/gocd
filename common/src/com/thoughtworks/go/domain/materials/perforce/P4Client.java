/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.materials.perforce;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.SCMCommand;
import com.thoughtworks.go.util.command.ProgramExitCode;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.util.command.PasswordArgument;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.apache.log4j.Logger;

import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;

public class P4Client extends SCMCommand {
    private static final Logger LOG = Logger.getLogger(P4Client.class);
    private final String p4Port;
    private final String p4user;
    private String p4passwd;
    private final String p4ClientName;

    private boolean useTickets;
    private boolean loggedIn;

    public static P4Client fromServerAndPort(String materialFingerprint, String serverAndPort, String userName, String password,
                                             String clientName, boolean useTickets, File workingdir, String p4view, ProcessOutputStreamConsumer consumer, boolean failOnError) throws Exception {
        return new P4Client(materialFingerprint,serverAndPort, userName, password, clientName,useTickets, workingdir, p4view, consumer, failOnError);
    }

    private P4Client(String materialFingerprint, String serverAndPort, String userName, String password, String p4ClientName, boolean useTickets, File workingDirectory, String p4view, ProcessOutputStreamConsumer consumer,
                     boolean failOnError) throws Exception {
        super(materialFingerprint);
        this.p4Port = serverAndPort;
        this.p4user = userName;
        this.p4passwd = password;
        this.p4ClientName = p4ClientName;
        this.useTickets = useTickets;
        client(clientSpec(p4ClientName, workingDirectory, p4view), consumer, failOnError);
    }

    private String clientSpec(String clientName, File workingFolder, String view) {
        return "Client: " + clientName + "\n\n"
                + "Root: " + workingFolder.getAbsolutePath() + "\n\n"
                + "Options: clobber rmdir\n\n"
                + "LineEnd: local\n\n"
                + "View:\n"
                + view;
    }

    public ConsoleResult checkConnection() {
        execute(p4("login", "-s"));
        return execute(p4("sync", "-n", "-m", "1", clientView()));
    }

    public ConsoleResult version() {
        CommandLine p4 = createCommandLine("p4").withArgs("-V");
        return execute(p4);
    }

    public int client(String clientSpec, ProcessOutputStreamConsumer consumer, boolean failOnError) throws Exception {
        return execute(p4("client", "-i"), clientSpec, consumer, failOnError);
    }

    public void removeClient() {
        execute(p4("client", "-f", "-d", p4ClientName));
    }

    public List<Modification> latestChange() {
        ConsoleResult result = execute(p4("changes", "-m", "1", clientView()));
        P4OutputParser parser = new P4OutputParser(this);
        return parser.modifications(result);
    }

    public List<Modification> changesSince(Revision revision) {
        CommandLine p4 = p4("changes", clientView()
                + "@" + revision.getRevision()
                + ",#head");
        ConsoleResult result = execute(p4);
        P4OutputParser parser = new P4OutputParser(this);
        return Modifications.filterOutRevision(parser.modifications(result), revision);
    }

    public void sync(long revision, boolean shouldForce, ProcessOutputStreamConsumer outputStreamConsumer) {
        if (shouldForce) {
            execute(p4("sync", "-f", clientView() + "@" + revision), "", outputStreamConsumer, true);
        } else {
            execute(p4("sync", clientView() + "@" + revision), "", outputStreamConsumer, true);
        }
    }

    public String describe(long revision) {
        return execute(p4("describe", "-s", String.valueOf(revision))).outputAsString();
    }

    private ConsoleResult execute(CommandLine p4, String input) {
        login();
        String[] input1 = new String[]{input};
        ConsoleResult result = runOrBomb(p4, input1);
        if (result.error().size() > 0) throw new RuntimeException(result.describe());
        return result;
    }

    int execute(CommandLine p4, String input, ProcessOutputStreamConsumer outputStreamConsumer, boolean failOnError) {
        login();
        int returnCode = run(p4, outputStreamConsumer, input);
        if (failOnError) {
            if (ProgramExitCode.COMMAND_NOT_FOUND == returnCode) throw new RuntimeException("Failed to find 'p4' on your PATH. Please ensure 'p4' is executable by the Go Server and on the Go Agents where this material will be used.");
            if (ProgramExitCode.SUCCESS != returnCode) throw new RuntimeException("Failed to run : " + p4.describe());
        }
        return returnCode;
    }

    private ConsoleResult execute(CommandLine p4) {
        if(LOG.isDebugEnabled()) LOG.debug("about to execute "+ p4.describe());
        return execute(p4, "");
    }


    private void login() {
        if (useTickets && !loggedIn) {
            loggedIn = true;//To change body of created methods use File | Settings | File Templates.
            execute(p4("login"), p4passwd + "\n");
        }
    }

    CommandLine p4(String command, String... params) {
        Map<String, String> env = new HashMap<>();
        env.put("P4PORT", p4Port);
        env.put("P4CLIENT", p4ClientName);
        if (hasUser()) {
            env.put("P4USER", p4user);
        }
        if (usingPassword()) {
            env.put("P4PASSWD", p4passwd);
        }

        CommandLine line = createCommandLine("p4")
                .withArgs(command).withArgs(params)
                .withEnv(env);
        if (hasPassword()) {
            line = line.withNonArgSecret(new PasswordArgument(p4passwd));
        }
        return line;
    }

    private boolean hasUser() {
        return p4user != null && !p4user.trim().isEmpty();
    }

    private boolean usingPassword() {
        return !useTickets && hasPassword();
    }

    private boolean hasPassword() {
        return p4passwd != null && !p4passwd.trim().isEmpty();
    }

    private String clientView() {
        return "//" + p4ClientName + "/...";
    }

    /**
     * @deprecated returns the output stream as is, without smudging password arguments, this is security problem, and should not be used in production code as is
     * This is left here only for tests
     */
    public String admin(String command) {
        return execute(p4("admin", command)).outputAsString();
    }

    public void useTickets(boolean useTickets) {
        this.useTickets = useTickets;
    }

    public String user() {
        return p4user;
    }

    public String password() {
        return p4passwd;
    }
}

