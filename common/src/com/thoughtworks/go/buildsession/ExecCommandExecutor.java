/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.CommandLineException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class ExecCommandExecutor implements BuildCommandExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ExecCommandExecutor.class);

    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        File workingDir = buildSession.resolveRelativeDir(command.getWorkingDirectory());
        if (!workingDir.isDirectory()) {
            String message = "Working directory \"" + workingDir.getAbsolutePath() + "\" is not a directory!";
            LOG.error(message);
            buildSession.println(message);
            return false;
        }

        String cmd = command.getStringArg("command");
        String[] args = command.getArrayArg("args");

        Map<String, String> secrets = buildSession.getSecretSubstitutions();
        Set<String> leftSecrets = new HashSet<>(secrets.keySet());

        CommandLine commandLine = createCommandLine(cmd);

        for (String arg : args) {
            if(secrets.containsKey(arg)) {
                leftSecrets.remove(arg);
                commandLine.withArg(new SubstitutableCommandArgument(arg, secrets.get(arg)));
            } else {
                commandLine.withArg(arg);
            }
        }

        for (String secret: leftSecrets) {
            commandLine.withNonArgSecret(new SecretSubstitution(secret, secrets.get(secret)));
        }

        commandLine.withWorkingDir(workingDir);
        commandLine.withEnv(buildSession.getEnvs());

        return executeCommandLine(buildSession, commandLine) == 0;
    }

    private CommandLine createCommandLine(String cmd) {
        CommandLine commandLine;
        if (SystemUtil.isWindows()) {
            commandLine = CommandLine.createCommandLine("cmd");
            commandLine.withArg("/c");
            commandLine.withArg(StringUtils.replace(cmd, "/", "\\"));
        } else {
            commandLine = CommandLine.createCommandLine(cmd);
        }
        return commandLine;
    }

    private int executeCommandLine(final BuildSession buildSession, final CommandLine commandLine) {
        final AtomicInteger exitCode = new AtomicInteger(-1);
        final CountDownLatch canceledOrDone = new CountDownLatch(1);
        buildSession.submitRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    exitCode.set(commandLine.run(buildSession.processOutputStreamConsumer(), null));
                } catch (CommandLineException e) {
                    LOG.error("Command failed", e);
                    String message = format("Error happened while attempting to execute '%s'. \nPlease make sure [%s] can be executed on this agent.\n", commandLine.toStringForDisplay(), commandLine.getExecutable());
                    String path = System.getenv("PATH");
                    buildSession.println(message);
                    buildSession.println(format("[Debug Information] Environment variable PATH: %s", path));
                    LOG.error(format("[Command Line] %s. Path: %s", message, path));
                } finally {
                    canceledOrDone.countDown();
                }
            }
        });

        Future<?> cancelMonitor = buildSession.submitRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    buildSession.waitUntilCanceled();
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    canceledOrDone.countDown();
                }
            }
        });

        try {
            canceledOrDone.await();
        } catch (InterruptedException e) {
            LOG.error("Building thread interrupted", e);
        }
        cancelMonitor.cancel(true);
        return exitCode.get();
    }
}
