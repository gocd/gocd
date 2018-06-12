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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao.DuplicateMetricReporting;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DataSharingServiceTest {
    private DataSharingService service;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;
    @Mock
    private UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    @Mock
    private DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    @Mock
    private EntityHashingService entityHashingService;

    private BasicCruiseConfig goConfig;
    private JobStateTransition oldestBuild;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new DataSharingService(goConfigService, jobInstanceSqlMapDao, usageStatisticsReportingSqlMapDao, dataSharingSettingsSqlMapDao, entityHashingService);
        goConfig = GoConfigMother.configWithPipelines("p1", "p2");
        goConfig.agents().add(new AgentConfig("agent1"));
        when(goConfigService.cruiseConfig()).thenReturn(goConfig);
        oldestBuild = new JobStateTransition(JobState.Scheduled, new Date());
        when(jobInstanceSqlMapDao.oldestBuild()).thenReturn(oldestBuild);
    }

    @Test
    public void shouldGetUsageStatistics() {
        UsageStatistics usageStatistics = service.getUsageStatistics();
        assertThat(usageStatistics.pipelineCount(), is(2l));
        assertThat(usageStatistics.agentCount(), is(1l));
        assertThat(usageStatistics.oldestPipelineExecutionTime(), is(oldestBuild.getStateChangeTime().getTime()));
    }

    @Test
    public void shouldReturnOldestPipelineExecutionTimeAsZeroIfNoneOfThePipelinesHaveEverRun() {
        when(jobInstanceSqlMapDao.oldestBuild()).thenReturn(null);
        UsageStatistics usageStatistics = service.getUsageStatistics();
        assertThat(usageStatistics.pipelineCount(), is(2l));
        assertThat(usageStatistics.agentCount(), is(1l));
        assertThat(usageStatistics.oldestPipelineExecutionTime(), is(0l));
    }

    @Test
    public void shouldNotIncludeElasticAgentsInTheCount() {
        goConfig.agents().add(AgentMother.elasticAgent());
        UsageStatistics usageStatistics = service.getUsageStatistics();
        assertThat(usageStatistics.agentCount(), is(1l));
    }

    @Test
    public void shouldThrowErrorWhenStatsSharedAtTimeIsNotProvidedWhileUpdatingStatsSharedAtTime() throws DuplicateMetricReporting {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.updateUsageStatisticsReporting(new UsageStatisticsReporting(), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Validations failed. Please correct and resubmit."));
    }

    @Test
    public void shouldThrowErrorWhenInvalidSharedAtTimeIsProvidedWhileUpdatingStatsSharedAtTime() throws DuplicateMetricReporting {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UsageStatisticsReporting reporting = new UsageStatisticsReporting();
        reporting.setLastReportedAt(new Date(-1));
        service.updateUsageStatisticsReporting(reporting, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Validations failed. Please correct and resubmit."));
    }

    @Test
    public void shouldUpdateStatsSharedAtTime() throws Exception {
        Date statsUpdatedAtTime = new Date();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArgumentCaptor<UsageStatisticsReporting> argumentCaptor = ArgumentCaptor.forClass(UsageStatisticsReporting.class);

        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting("server-id", new Date());
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);

        UsageStatisticsReporting reporting = new UsageStatisticsReporting();
        reporting.setLastReportedAt(statsUpdatedAtTime);
        service.updateUsageStatisticsReporting(reporting, result);

        assertThat(result.isSuccessful(), is(true));
        verify(usageStatisticsReportingSqlMapDao).saveOrUpdate(argumentCaptor.capture());
        UsageStatisticsReporting newMetricReporting = argumentCaptor.getValue();
        assertThat(newMetricReporting.lastReportedAt().toInstant(), is(statsUpdatedAtTime.toInstant()));
    }

    @Test
    public void shouldUpdateDataSharingSettings() throws Exception {
        boolean newConsent = false;
        String consentedBy = "Bob";
        ArgumentCaptor<DataSharingSettings> argumentCaptor = ArgumentCaptor.forClass(DataSharingSettings.class);

        service.updateDataSharingSettings(new DataSharingSettings(newConsent, consentedBy, new Date()));

        verify(dataSharingSettingsSqlMapDao).saveOrUpdate(argumentCaptor.capture());
        DataSharingSettings updatedDataSharingSettings = argumentCaptor.getValue();

        assertThat(updatedDataSharingSettings.allowSharing(), is(newConsent));
        assertThat(updatedDataSharingSettings.updatedBy(), is(consentedBy));
    }
}
