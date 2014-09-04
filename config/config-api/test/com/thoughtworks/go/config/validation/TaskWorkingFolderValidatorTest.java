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

package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TaskWorkingFolderValidatorTest {
    private CruiseConfig cruiseConfig;

    @Before public void setUp() throws Exception {
        cruiseConfig = GoConfigMother.configWithPipelines("first");
    }

    @Test
    public void testShouldValidateBuildTaskSandboxIsInsideSandobox() {
        AntTask task = new AntTask();
        task.setWorkingDirectory("/pavan");
        assertTaskInvalid(task, "/pavan");
    }

    @Test
    public void testShouldValidateFetchTaskDestinationSandboxIsInsideSandobox() {
        assertTaskInvalid(new FetchTask(new CaseInsensitiveString("first"), new CaseInsensitiveString("stage"),
                new CaseInsensitiveString("job"), "first-level/../going-back", "/some/path"), "/some/path");
    }

    @Test
    public void testShouldValidateFetchTaskSourceSandboxIsInsideSandobox() {
        assertTaskInvalid(new FetchTask(new CaseInsensitiveString("first"), new CaseInsensitiveString("stage"),
                new CaseInsensitiveString("job"), "first-level/../../../going-back", "some/path"), "first-level/../../../going-back");
    }

    private void assertTaskInvalid(Task task, String path) {
        cruiseConfig.findJob("first", "stage", "job").addTask(task);
        try {
            new TaskWorkingFolderValidator().validate(cruiseConfig);
            fail("Should not allow absolute path for task");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Task of job 'job' in stage 'stage' of pipeline 'first' has path '" + path + "' which is outside the working directory."));
        }
    }
}
