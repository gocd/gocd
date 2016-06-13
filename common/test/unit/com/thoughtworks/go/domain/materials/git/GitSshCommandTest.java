/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import org.apache.commons.codec.binary.Base64;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.InvertedShellWrapper;
import org.apache.sshd.server.shell.ProcessShell;
import org.apache.tools.ant.types.Commandline;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GitSshCommandTest extends GitCommandTest {
    private SshServer sshd;
    private KeyPair keyPair;

    @Before
    public void setup() throws Exception {
        keyPair = getKeyPair();
        sshd = SshServer.setUpDefaultServer();
        sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(Collections.singletonList(keyPair.getPublic())));
        sshd.setCommandFactory(new CommandFactory() {
            @Override
            public Command createCommand(String command) {
                List<String> args = Arrays.asList(Commandline.translateCommandline(command));
                return new InvertedShellWrapper(new ProcessShell(args));
            }
        });
        SimpleGeneratorHostKeyProvider keyPairProvider = new SimpleGeneratorHostKeyProvider((Path) null);
        keyPairProvider.setAlgorithm("RSA");
        sshd.setKeyPairProvider(keyPairProvider);

        sshd.setPort(findRandomOpenPortOnAllLocalInterfaces());
        sshd.start();

        System.setProperty("SSH_PORT", Integer.toString(sshd.getPort()));
        super.setup();
    }


    @Override
    protected String repoUrl() {
        String s = super.repoUrl();
        return s.replaceAll("file://", "ssh://localhost:");
    }

    @Override
    protected GitCommand getGitCommand() {
        return new GitCommand(null, gitLocalRepoDir, GitMaterialConfig.DEFAULT_BRANCH, false, new HashMap<String, String>(), pemFormattedPrivateKey(keyPair));
    }

    private String pemFormattedPrivateKey(KeyPair keyPair) {
        PrivateKey aPrivate = keyPair.getPrivate();
        String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n";

        privateKey += new Base64().encodeToString(aPrivate.getEncoded());

        privateKey += "-----END RSA PRIVATE KEY-----\n";
        return privateKey;
    }


    private Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator;
        generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.genKeyPair();
    }


    @After
    public void teardown() throws Exception {
        super.teardown();
        sshd.stop();
    }
}
