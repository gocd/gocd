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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao.DuplicateMetricReporting;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service
public class DataSharingService {
    private final GoConfigService goConfigService;
    private final JobInstanceSqlMapDao jobInstanceSqlMapDao;
    private final UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    private final DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    private final EntityHashingService entityHashingService;
    private Object mutexForDataSharingSettings = new Object();
    private Object mutexForUsageStatisticsReporting = new Object();

    @Autowired
    public DataSharingService(GoConfigService goConfigService, JobInstanceSqlMapDao jobInstanceSqlMapDao,
                              UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao,
                              DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao,
                              EntityHashingService entityHashingService) {
        this.goConfigService = goConfigService;
        this.jobInstanceSqlMapDao = jobInstanceSqlMapDao;
        this.usageStatisticsReportingSqlMapDao = usageStatisticsReportingSqlMapDao;
        this.dataSharingSettingsSqlMapDao = dataSharingSettingsSqlMapDao;
        this.entityHashingService = entityHashingService;
    }

    public UsageStatistics getUsageStatistics() {
        CruiseConfig config = goConfigService.cruiseConfig();
        JobStateTransition jobStateTransition = jobInstanceSqlMapDao.oldestBuild();
        Long oldestPipelineExecutionTime = jobStateTransition == null ? null : jobStateTransition.getStateChangeTime().getTime();
        long nonElasticAgentCount = config.agents().parallelStream().filter(agentConfig -> !agentConfig.isElastic()).count();
        long size = config.getAllPipelineConfigs().size();
        return new UsageStatistics(size, nonElasticAgentCount, oldestPipelineExecutionTime);
    }

    public void initialize() throws DuplicateMetricReporting, DuplicateDataSharingSettingsException {
        UsageStatisticsReporting existingUsageStatisticsReporting = usageStatisticsReportingSqlMapDao.load();
        DataSharingSettings existingDataSharingSettings = dataSharingSettingsSqlMapDao.load();

        if (existingDataSharingSettings == null) {
            updateDataSharingSettings(new DataSharingSettings(true, "Default", new Date()));
        }

        if (existingUsageStatisticsReporting == null) {
            UsageStatisticsReporting usageStatisticsReporting = new UsageStatisticsReporting(UUID.randomUUID().toString(), new Date());
            updateUsageStatisticsReporting(usageStatisticsReporting, new HttpLocalizedOperationResult());
        }
        if (new SystemEnvironment().shouldFailStartupOnDataError()) {
            assert getDataSharingSettings() != null;
            assert getUsageStatisticsReporting() != null;
        }
    }

    public UsageStatisticsReporting getUsageStatisticsReporting() {
        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        loaded.setDataSharingServerUrl(SystemEnvironment.getGoDataSharingServerUrl());

        return loaded;
    }

    public DataSharingSettings getDataSharingSettings() {
        return dataSharingSettingsSqlMapDao.load();
    }

    public void updateUsageStatisticsReporting(UsageStatisticsReporting reporting, HttpLocalizedOperationResult result) throws DuplicateMetricReporting {
        reporting.validate(null);
        if (!reporting.errors().isEmpty()) {
            result.unprocessableEntity("Validations failed. Please correct and resubmit.");
            return;
        }
        synchronized (mutexForUsageStatisticsReporting) {
            usageStatisticsReportingSqlMapDao.saveOrUpdate(reporting);
            entityHashingService.removeFromCache(reporting, reporting.getServerId());
        }
    }

    public void updateDataSharingSettings(DataSharingSettings dataSharingSettings) throws DuplicateDataSharingSettingsException {
        synchronized (mutexForDataSharingSettings) {
            dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings);
            entityHashingService.removeFromCache(dataSharingSettings, Long.toString(dataSharingSettings.getId()));
        }
    }
}
