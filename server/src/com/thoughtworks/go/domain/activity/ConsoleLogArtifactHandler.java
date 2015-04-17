/**
 * **********************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END**********************************
 */

package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.util.GoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class ConsoleLogArtifactHandler implements JobStatusListener {
    private ConsoleService consoleService;

    @Autowired
    public ConsoleLogArtifactHandler(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        if (job.getState().isCompleted()) {
            try {
                JobIdentifier identifier = job.getIdentifier();
                consoleService.moveConsoleArtifacts(identifier);
                // TODO: Put correct timestamp of job completion the agent and the server maybe on different timezones.
                consoleService.appendToConsoleLog(identifier, format("[%s] %s %s", GoConstants.PRODUCT_NAME, "Job Completed"
                        , identifier.buildLocatorForDisplay()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
