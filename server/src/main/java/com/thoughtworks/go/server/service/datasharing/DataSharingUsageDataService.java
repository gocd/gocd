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
package com.thoughtworks.go.server.service.datasharing;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class DataSharingUsageDataService {
    private final GoConfigService goConfigService;
    private final JobInstanceSqlMapDao jobInstanceSqlMapDao;
    private final DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService;
    private final GoCache goCache;
    public static String ADD_PIPELINE_CTA = "add-pipeline-cta";
    public static String SAVE_AND_RUN_CTA = "save-and-run-cta";
    private AgentService agentService;


    @Autowired
    public DataSharingUsageDataService(GoConfigService goConfigService, JobInstanceSqlMapDao jobInstanceSqlMapDao,
                                       DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService,GoCache goCache, AgentService agentService) {
        this.goConfigService = goConfigService;
        this.jobInstanceSqlMapDao = jobInstanceSqlMapDao;
        this.dataSharingUsageStatisticsReportingService = dataSharingUsageStatisticsReportingService;
        this.goCache = goCache;
        this.agentService = agentService;
    }

    public UsageStatistics get() {
        CruiseConfig config = goConfigService.getCurrentConfig();
        JobStateTransition jobStateTransition = jobInstanceSqlMapDao.oldestBuild();
        long oldestPipelineExecutionTime = jobStateTransition == null ? 0l : jobStateTransition.getStateChangeTime().getTime();
        long nonElasticAgentCount = agentService.agents().parallelStream().filter(agent -> !agent.isElastic()).count();
        List<PipelineConfig> pipelineConfigs = config.getAllPipelineConfigs();
        long pipelineCount = pipelineConfigs.size();
        long configRepoPipelineCount = pipelineConfigs.stream().filter(PipelineConfig::isConfigDefinedRemotely).count();

        long jobsCount = 0L;
        Map<String, Long> elasticAgentPluginToJobCount = new HashMap<>(1024);

        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            for (StageConfig stageConfig : pipelineConfig) {
                jobsCount += stageConfig.getJobs().size();
                for (JobConfig job : stageConfig.getJobs()) {
                    String elasticProfileId = job.getElasticProfileId();
                    if (elasticProfileId != null) {
                        ElasticProfile elasticProfile = config.getElasticConfig().getProfiles().find(elasticProfileId);
                        String key = findPluginIdFromReferencedCluster(elasticProfile);
                        elasticAgentPluginToJobCount.put(key, elasticAgentPluginToJobCount.getOrDefault(key, 0L) + 1);
                    }
                }
            }
        }

        String serverId = dataSharingUsageStatisticsReportingService.get().getServerId();

        return UsageStatistics.newUsageStatistics()
                .pipelineCount(pipelineCount)
                .configRepoPipelineCount(configRepoPipelineCount)
                .agentCount(nonElasticAgentCount)
                .jobCount(jobsCount)
                .elasticAgentPluginToJobCount(elasticAgentPluginToJobCount)
                .oldestPipelineExecutionTime(oldestPipelineExecutionTime)
                .serverId(serverId)
                .gocdVersion(CurrentGoCDVersion.getInstance().fullVersion())
                .testDrive(Toggles.isToggleOn(Toggles.TEST_DRIVE))
                .addCTA(goCache.getOrDefault(ADD_PIPELINE_CTA, false))
                .saveAndRunCTA(goCache.getOrDefault(SAVE_AND_RUN_CTA, false))
                .build();
    }

    private String findPluginIdFromReferencedCluster(ElasticProfile elasticProfile) {
        return goConfigService.getElasticConfig().getClusterProfiles().find(elasticProfile.getClusterProfileId()).getPluginId();
    }
}
