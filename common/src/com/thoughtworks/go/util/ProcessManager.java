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

package com.thoughtworks.go.util;

import com.thoughtworks.go.util.command.CommandLineException;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class ProcessManager {

    private static final Logger LOG = Logger.getLogger(ProcessManager.class);
    private static final ProcessManager processManager = new ProcessManager();

    private ConcurrentMap<Process, ProcessWrapper> processMap = new ConcurrentHashMap<>();

    ProcessManager() {
    }

    public static ProcessManager getInstance() {
        return processManager;
    }

    public ProcessWrapper createProcess(String[] commandLine, String commandLineForDisplay, File workingDir, Map<String, String> envMap, EnvironmentVariableContext environmentVariableContext,
                                        ConsoleOutputStreamConsumer consumer, String processTag, String encoding, String errorPrefix) {
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing: " + commandLineForDisplay);
        }
        if (workingDir != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("[Command Line] Using working directory %s to start the process.", workingDir.getAbsolutePath()));
            }
            processBuilder.directory(workingDir);
        }

        processBuilder.environment().putAll(environmentVariableContext.getProperties());
        processBuilder.environment().putAll(envMap);

        Process process = startProcess(processBuilder, commandLineForDisplay);
        ProcessWrapper processWrapper = new ProcessWrapper(process, processTag, commandLineForDisplay, consumer, encoding, errorPrefix);
        processMap.putIfAbsent(process, processWrapper);
        return processWrapper;
    }

    public void processKilled(Process process) {
        if (process != null && processMap.containsKey(process)) {
            processMap.remove(process);
        }
    }

    public long getIdleTimeFor(String processTag) {
        for (ProcessWrapper processWrapper : processMap.values()) {
            String tag = processWrapper.getProcessTag();
            if (!isEmpty(tag) && tag.equalsIgnoreCase(processTag)) {
                return processWrapper.getIdleTime();
            }
        }
        return 0;
    }

    public Collection<ProcessWrapper> currentProcessListForDisplay() {
        return processMap.values();
    }

    public Set<String> environmentVariableNames() {
        return System.getenv().keySet();
    }

    //should be used only for tests
    ConcurrentMap<Process, ProcessWrapper> getProcessMap() {
        return processMap;
    }

    Process startProcess(ProcessBuilder processBuilder, String commandLineForDisplay) {
        Process process;
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[Command Line] START command " + commandLineForDisplay);
            }
            process = processBuilder.start();
            if (LOG.isDebugEnabled()) {
                LOG.debug("[Command Line] END command " + commandLineForDisplay);
            }
        } catch (IOException e) {
            LOG.error(String.format("[Command Line] Failed executing [%s]", commandLineForDisplay));
            LOG.error(String.format("[Command Line] Agent's Environment Variables: %s", System.getenv()));
            throw new CommandLineException(String.format("Error while executing [%s] \n Make sure this command can execute manually.", commandLineForDisplay), e);
        }
        return process;
    }

}
