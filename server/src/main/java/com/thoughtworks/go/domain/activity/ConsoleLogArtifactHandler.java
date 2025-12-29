/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.service.ConsoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsoleLogArtifactHandler implements JobStatusListener {
    private final ConsoleService consoleService;

    @Autowired
    public ConsoleLogArtifactHandler(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        /*
            We are checking for jobs which are a copy because the completion event is fired for a job that's a part
            of a stage which have rerun jobs and the log should already be in the target location from the first run.
            A better fix may be to not raise this event for those jobs.
         */
        if (!job.isCopy() && job.isCompleted()) {
            JobIdentifier identifier = job.getIdentifier();
            consoleService.moveConsoleArtifacts(identifier);
        }
    }
}
