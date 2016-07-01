/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import org.junit.Test;

import static com.thoughtworks.go.domain.PipelinePauseInfo.notPaused;
import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GoDashboardPipelineTest {
    @Test
    public void shouldKnowWhetherAUserCanViewIt() throws Exception {
        Permissions permissions = new Permissions(
                new AllowedUsers(s("viewer1", "viewer2")),
                Everyone.INSTANCE,
                new AllowedUsers(s("admin", "root")),
                Everyone.INSTANCE);

        GoDashboardPipeline pipeline = new GoDashboardPipeline(new PipelineModel("pipeline1", false, false, notPaused()), permissions, "group1");

        assertThat(pipeline.canBeViewedBy("viewer1"), is(true));
        assertThat(pipeline.canBeViewedBy("viewer2"), is(true));

        assertThat(pipeline.canBeViewedBy("some-other-user-not-in-viewers-list"), is(false));
        assertThat(pipeline.canBeViewedBy("admin"), is(false));
    }
}