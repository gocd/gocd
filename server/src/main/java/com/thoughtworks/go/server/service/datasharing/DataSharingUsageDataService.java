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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.datasharing.DataSharingUsageStatisticsReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class DataSharingUsageDataService {
    private final GoConfigService goConfigService;
    private final JobInstanceSqlMapDao jobInstanceSqlMapDao;
    private final DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService;

    @Autowired
    public DataSharingUsageDataService(GoConfigService goConfigService, JobInstanceSqlMapDao jobInstanceSqlMapDao,
                                       DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService) {
        this.goConfigService = goConfigService;
        this.jobInstanceSqlMapDao = jobInstanceSqlMapDao;
        this.dataSharingUsageStatisticsReportingService = dataSharingUsageStatisticsReportingService;
    }

    public UsageStatistics get() {
        CruiseConfig config = goConfigService.getCurrentConfig();
        JobStateTransition jobStateTransition = jobInstanceSqlMapDao.oldestBuild();
        Long oldestPipelineExecutionTime = jobStateTransition == null ? null : jobStateTransition.getStateChangeTime().getTime();
        long nonElasticAgentCount = config.agents().parallelStream().filter(agentConfig -> !agentConfig.isElastic()).count();
        long pipelineCount = config.getAllPipelineConfigs().size();
        String serverId = dataSharingUsageStatisticsReportingService.get().getServerId();
        return new UsageStatistics(pipelineCount, nonElasticAgentCount, oldestPipelineExecutionTime, serverId, CurrentGoCDVersion.getInstance().fullVersion());
    }
}
