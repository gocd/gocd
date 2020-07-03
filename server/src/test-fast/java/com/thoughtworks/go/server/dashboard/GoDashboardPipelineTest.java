/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.permissions.EveryonePermission;
import com.thoughtworks.go.config.security.permissions.NoOnePermission;
import com.thoughtworks.go.config.security.permissions.PipelinePermission;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import org.junit.Test;

import java.util.Collections;

import static com.thoughtworks.go.domain.PipelinePauseInfo.notPaused;
import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoDashboardPipelineTest {
    @Test
    public void shouldKnowWhetherAUserCanViewIt() throws Exception {
        Permissions permissions = new Permissions(
                new AllowedUsers(s("viewer1", "viewer2"), Collections.emptySet()),
                Everyone.INSTANCE,
                new AllowedUsers(s("admin", "root"), Collections.emptySet()),
                EveryonePermission.INSTANCE);

        GoDashboardPipeline pipeline = new GoDashboardPipeline(new PipelineModel("pipeline1", false, false, notPaused()), permissions, "group1", mock(TimeStampBasedCounter.class), PipelineConfigMother.pipelineConfig("pipeline1"));

        assertThat(pipeline.canBeViewedBy("viewer1"), is(true));
        assertThat(pipeline.canBeViewedBy("viewer2"), is(true));

        assertThat(pipeline.canBeViewedBy("some-other-user-not-in-viewers-list"), is(false));
        assertThat(pipeline.canBeViewedBy("admin"), is(false));
    }

    @Test
    public void shouldKnowWhetherAUserCanOperateIt() throws Exception {
        Permissions permissions = new Permissions(
                NoOne.INSTANCE,
                new AllowedUsers(s("operator1"), Collections.emptySet()),
                NoOne.INSTANCE,
                NoOnePermission.INSTANCE);

        GoDashboardPipeline pipeline = new GoDashboardPipeline(new PipelineModel("pipeline1", false, false, notPaused()),
                permissions, "group1", mock(TimeStampBasedCounter.class), PipelineConfigMother.pipelineConfig("pipeline1"));

        assertTrue(pipeline.canBeOperatedBy("operator1"));
        assertFalse(pipeline.canBeOperatedBy("viewer1"));
    }

    @Test
    public void shouldKnowWhetherAUserCanAdministerIt() throws Exception {
        Permissions permissions = new Permissions(
                NoOne.INSTANCE,
                NoOne.INSTANCE,
                new AllowedUsers(s("admin1"), Collections.emptySet()),
                NoOnePermission.INSTANCE);

        GoDashboardPipeline pipeline = new GoDashboardPipeline(new PipelineModel("pipeline1", false, false, notPaused()),
                permissions, "group1", mock(TimeStampBasedCounter.class), PipelineConfigMother.pipelineConfig("pipeline1"));

        assertTrue(pipeline.canBeAdministeredBy("admin1"));
        assertFalse(pipeline.canBeAdministeredBy("viewer1"));
    }

    @Test
    public void shouldKnowWhetherAUserIsPipelineLevelOperator() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        Permissions permissions = new Permissions(
                NoOne.INSTANCE,
                NoOne.INSTANCE,
                NoOne.INSTANCE,
                PipelinePermission.from(pipelineConfig, new AllowedUsers(s("pipeline_operator"), Collections.emptySet())));

        GoDashboardPipeline pipeline = new GoDashboardPipeline(new PipelineModel("pipeline1", false, false, notPaused()),
                permissions, "group1", mock(TimeStampBasedCounter.class), pipelineConfig);

        assertTrue(pipeline.isPipelineOperator("pipeline_operator"));
        assertFalse(pipeline.canBeAdministeredBy("viewer1"));
    }

    @Test
    public void shouldSetTheLastUpdateTime() throws Exception {
        TimeStampBasedCounter provider = mock(TimeStampBasedCounter.class);
        when(provider.getNext()).thenReturn(1000L);
        GoDashboardPipeline pipeline = new GoDashboardPipeline(new PipelineModel("pipeline1", false, false, notPaused()), null, "group1", provider, PipelineConfigMother.pipelineConfig("pipeline1"));

        assertThat(pipeline.getLastUpdatedTimeStamp(), is(1000L));
    }
}
