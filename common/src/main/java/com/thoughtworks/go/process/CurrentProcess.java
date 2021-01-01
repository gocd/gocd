/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CurrentProcess implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(CurrentProcess.class);

    public void infanticide() {
        kill(processHandle -> {
            LOG.debug("Attempting to destroy process {}", processHandle.pid());
            if (processHandle.isAlive()) {
                processHandle.destroy();
                LOG.debug("Destroyed process {}", processHandle.pid());
            }
        });
    }

    public void forceKillChildren() {
        kill(processHandle -> {
            LOG.debug("Attempting to force destroy process {}", processHandle.pid());
            if (processHandle.isAlive()) {
                processHandle.destroyForcibly();
                LOG.debug("Forcibly destroyed process {}", processHandle.pid());
            }
        });
    }

    public long currentPid() {
        return ProcessHandle.current().pid();
    }

    public List<ProcessHandle> immediateChildren() {
        return ProcessHandle.current().children().collect(Collectors.toList());
    }

    private void kill(Consumer<ProcessHandle> processHandle) {
        try {
            ProcessHandle current = ProcessHandle.current();
            Stream<ProcessHandle> processHandleStream = current.descendants();
            processHandleStream.forEach(processHandle);
        } catch (Exception e) {
            LOG.error("Unable to infanticide", e);
        }
    }
}
