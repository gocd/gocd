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

package com.thoughtworks.go.mail;

import java.io.File;
import java.io.IOException;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.GoSmtpMailSender;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.helper.RandomPort;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.ProcessWrapper;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.utils.Pop3MailClient;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Ignore("This test is flaky due to process issues")
@RunWith(JunitExtRunner.class)
public class SmtpJes2MailSenderIntegrationTest {
    private ProcessWrapper processWrapper;

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    @Ignore(value = "Failing on linux for astrange reason - working on it")
    public void shouldBeAbleToSendEmailWithoutUsernameAndPassword() throws Exception {
        try {
            startJesServer();

            Pop3MailClient client = new Pop3MailClient("localhost", 1101, "cruise@localhost", "password");
            client.deleteAllMessages();
            assertThat(client.numberOfMessages(), is(0));

            int count = 5;

            System.out.println("Sending test messages");
            for (int i=0; i< count; i++) {
                sendMessage(i);
            }

            System.out.println("Waiting for test messages to arrive");
            for (int i=0; i< count; i++) {
                shouldReceiveMessage(client, "from-" + i + "@localhost", "This is a test " + i);
            }
        }
        finally {
            stopServer();
        }
    }

    private void sendMessage(final int count) {
        new Thread(new Runnable() {
            public void run() {
                send("from-" + count + "@localhost", "This is a test " + count);
            }
        }).run();
    }

    private void stopServer() throws InterruptedException, IOException {
        File jesDir = new File("../tools/jes-2.0-beta2");
        SysOutStreamConsumer output = new SysOutStreamConsumer();
        CommandLine command = CommandLine.createCommandLine("/bin/bash")
                .withWorkingDir(jesDir)
                .withArg("stop-server.sh");
        processWrapper = command.execute(output, new EnvironmentVariableContext(), null);

        System.out.println("Waiting for server to stop.");
        try {
            RandomPort.waitForPortToBeFree(2525, 10000L);
            RandomPort.waitForPortToBeFree(1101, 10000L);
        }
        catch (Exception ex) {
            System.out.println("Killing server.");
            command = CommandLine.createCommandLine("/bin/bash")
                    .withWorkingDir(jesDir)
                    .withArg("kill-server.sh");
            processWrapper = command.execute(output, new EnvironmentVariableContext(), null);
        }
    }

    private void startJesServer() throws IOException, InterruptedException {
        File jesDir = new File("../tools/jes-2.0-beta2");
        FileUtils.deleteDirectory(new File(jesDir, "conf.run"));
        File runDir = new File(jesDir, "conf.run");
        runDir.mkdir();
        FileUtils.copyDirectory(new File(jesDir, "conf.noauth"), new File(runDir, "conf"));
        FileUtils.copyDirectory(new File(jesDir, "security"), new File(runDir, "security"));

        SysOutStreamConsumer output = new SysOutStreamConsumer();
        CommandLine command = CommandLine.createCommandLine("/bin/bash")
                .withWorkingDir(jesDir)
                .withArg("start-server.sh");
        System.out.println("command.toString() = " + command.toString());
        processWrapper = command.execute(output, new EnvironmentVariableContext(), null);
        RandomPort.waitForPort(2525, 20000L);
        RandomPort.waitForPort(1101, 20000L);
    }

    private void send(String from, String body) {
        GoSmtpMailSender sender =
                new GoSmtpMailSender("localhost", 2525, "", "", false, from, "cruise@localhost");
        ValidationBean result = sender.send("Hello", body, "cruise@localhost");
        System.out.println("result = " + result);
    }

    private void shouldReceiveMessage(Pop3MailClient client, String from, String body) throws Exception {
        Timeout timeout = new Timeout("Waiting for mail to arrive", 50 * 1000);
        while (timeout.check()) {
            if (client.numberOfMessages() > 0) {
                String content = client.findMessageFrom(from);
                assertThat(content, containsString(body));
                return;
            }
        }
        fail("Never received a message from " + from);
    }
}