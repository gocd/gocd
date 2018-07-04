/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.domain.UsageStatistics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DataSharingUsageDataServiceTest {
    private DataSharingUsageDataService service;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;

    private BasicCruiseConfig goConfig;
    private JobStateTransition oldestBuild;
    @Mock
    private DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new DataSharingUsageDataService(goConfigService, jobInstanceSqlMapDao, dataSharingUsageStatisticsReportingService);
        goConfig = GoConfigMother.configWithPipelines("p1", "p2");
        goConfig.agents().add(new AgentConfig("agent1"));
        when(goConfigService.cruiseConfig()).thenReturn(goConfig);
        oldestBuild = new JobStateTransition(JobState.Scheduled, new Date());
        when(jobInstanceSqlMapDao.oldestBuild()).thenReturn(oldestBuild);
        when(dataSharingUsageStatisticsReportingService.get()).thenReturn(new UsageStatisticsReporting("server-id", new Date()));
    }

    @Test
    public void shouldGetUsageStatistics() {
        UsageStatistics usageStatistics = service.get();
        assertThat(usageStatistics.pipelineCount(), is(2l));
        assertThat(usageStatistics.agentCount(), is(1l));
        assertThat(usageStatistics.oldestPipelineExecutionTime(), is(oldestBuild.getStateChangeTime().getTime()));
        assertThat(usageStatistics.serverId(), is("server-id"));
        assertThat(usageStatistics.gocdVersion(), is(CurrentGoCDVersion.getInstance().goVersion()));
    }

    @Test
    public void shouldReturnOldestPipelineExecutionTimeAsZeroIfNoneOfThePipelinesHaveEverRun() {
        when(jobInstanceSqlMapDao.oldestBuild()).thenReturn(null);
        UsageStatistics usageStatistics = service.get();
        assertThat(usageStatistics.pipelineCount(), is(2l));
        assertThat(usageStatistics.agentCount(), is(1l));
        assertThat(usageStatistics.oldestPipelineExecutionTime(), is(0l));
    }

    @Test
    public void shouldNotIncludeElasticAgentsInTheCount() {
        goConfig.agents().add(AgentMother.elasticAgent());
        UsageStatistics usageStatistics = service.get();
        assertThat(usageStatistics.agentCount(), is(1l));
    }
}
