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

package com.thoughtworks.go.server.service.datasharing;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
        goConfig.getElasticConfig().getProfiles().add(new ElasticProfile("docker-profile", "prod-cluster-2"));
        goConfig.getElasticConfig().getProfiles().add(new ElasticProfile("ecs-profile", "prod-cluster-1"));
        goConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod-cluster-1", "ecs-plugin"));
        goConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod-cluster-2", "docker-plugin"));

        PipelineConfig configRepoPipeline = PipelineConfigMother.createPipelineConfig("p3", "s1");
        JobConfig elasticJob1 = JobConfigMother.elasticJob("docker-profile");
        JobConfig elasticJob2 = JobConfigMother.elasticJob("ecs-profile");
        configRepoPipeline.getFirstStageConfig().getJobs().addAll(Arrays.asList(elasticJob1, elasticJob2));
        configRepoPipeline.setOrigin(new RepoConfigOrigin());
        goConfig.addPipeline("first", configRepoPipeline);
        goConfig.agents().add(new AgentConfig("agent1"));
        when(goConfigService.getCurrentConfig()).thenReturn(goConfig);
        when(goConfigService.getElasticConfig()).thenReturn(goConfig.getElasticConfig());
        oldestBuild = new JobStateTransition(JobState.Scheduled, new Date());
        when(jobInstanceSqlMapDao.oldestBuild()).thenReturn(oldestBuild);
        when(dataSharingUsageStatisticsReportingService.get()).thenReturn(new UsageStatisticsReporting("server-id", new Date()));
    }

    @Test
    public void shouldGetUsageStatistics() {
        UsageStatistics usageStatistics = service.get();
        assertThat(usageStatistics.pipelineCount()).isEqualTo(3L);
        assertThat(usageStatistics.configRepoPipelineCount()).isEqualTo(1L);
        assertThat(usageStatistics.agentCount()).isEqualTo(1L);
        assertThat(usageStatistics.jobCount()).isEqualTo(4L);
        Map<String, Long> elasticAgentPluginToJobCount = usageStatistics.elasticAgentPluginToJobCount();
        assertThat(elasticAgentPluginToJobCount).hasSize(2)
                .containsEntry("ecs-plugin", 1L)
                .containsEntry("docker-plugin", 1L);
        assertThat(usageStatistics.oldestPipelineExecutionTime()).isEqualTo(oldestBuild.getStateChangeTime().getTime());
        assertThat(usageStatistics.serverId()).isEqualTo("server-id");
        assertThat(usageStatistics.gocdVersion()).isEqualTo(CurrentGoCDVersion.getInstance().fullVersion());
    }

    @Test
    public void shouldReturnOldestPipelineExecutionTimeAsZeroIfNoneOfThePipelinesHaveEverRun() {
        when(jobInstanceSqlMapDao.oldestBuild()).thenReturn(null);
        UsageStatistics usageStatistics = service.get();
        assertThat(usageStatistics.pipelineCount()).isEqualTo(3L);
        assertThat(usageStatistics.agentCount()).isEqualTo(1L);
        assertThat(usageStatistics.oldestPipelineExecutionTime()).isEqualTo(0L);
    }

    @Test
    public void shouldNotIncludeElasticAgentsInTheCount() {
        goConfig.agents().add(AgentMother.elasticAgent());
        UsageStatistics usageStatistics = service.get();
        assertThat(usageStatistics.agentCount()).isEqualTo(1L);
    }
}
