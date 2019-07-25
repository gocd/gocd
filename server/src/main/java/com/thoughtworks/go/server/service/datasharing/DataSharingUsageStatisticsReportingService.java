/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao;
import com.thoughtworks.go.server.service.datasharing.DataSharingSettingsService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import static com.thoughtworks.go.util.DateUtils.isToday;


@Service
public class DataSharingUsageStatisticsReportingService {
    private final UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    private final Clock clock;
    private DataSharingSettingsService dataSharingSettingsService;
    private final SystemEnvironment systemEnvironment;
    private final Object mutexForReportingUsageData = new Object();

    private DateTime reportingStartedTime = null;

    @Autowired
    public DataSharingUsageStatisticsReportingService(UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao,
                                                      DataSharingSettingsService dataSharingSettingsService,
                                                      SystemEnvironment systemEnvironment,
                                                      Clock clock) {
        this.usageStatisticsReportingSqlMapDao = usageStatisticsReportingSqlMapDao;
        this.dataSharingSettingsService = dataSharingSettingsService;
        this.systemEnvironment = systemEnvironment;
        this.clock = clock;
    }

    public void initialize() {
        UsageStatisticsReporting existingUsageStatisticsReporting = usageStatisticsReportingSqlMapDao.load();

        if (existingUsageStatisticsReporting == null) {
            create();
        }

        if (new SystemEnvironment().shouldFailStartupOnDataError()) {
            assert get() != null;
        }
    }

    private void create() {
        UsageStatisticsReporting reporting = new UsageStatisticsReporting(UUID.randomUUID().toString(), new Date(0));
        usageStatisticsReportingSqlMapDao.saveOrUpdate(reporting);
    }

    public UsageStatisticsReporting get() {
        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        loaded.setDataSharingServerUrl(systemEnvironment.getGoDataSharingServerUrl());
        loaded.setDataSharingGetEncryptionKeysUrl(systemEnvironment.getGoDataSharingGetEncryptionKeysUrl());
        boolean canReport = !isDevelopmentServer() && dataSharingSettingsService.get().allowSharing()
                && !isToday(loaded.lastReportedAt()) && !isReportingInProgress();
        loaded.canReport(canReport);
        return loaded;
    }

    private boolean isReportingInProgress() {
        if (reportingStartedTime == null) {
            return false;
        }

        DateTime halfHourAgo = new DateTime(clock.currentTimeMillis() - 30 * 60 * 1000);
        return reportingStartedTime.isAfter(halfHourAgo);
    }

    public void startReporting(HttpLocalizedOperationResult result) {
        synchronized (mutexForReportingUsageData) {
            if (isReportingInProgress()) {
                String err = "Cannot start usage statistics reporting as it is already in progress.";
                result.unprocessableEntity(err);
                return;
            }
            reportingStartedTime = new DateTime();
        }
    }

    public void completeReporting(HttpLocalizedOperationResult result) {
        synchronized (mutexForReportingUsageData) {
            if (reportingStartedTime == null) {
                String err = "Cannot complete usage statistics reporting as it has already completed.";
                result.unprocessableEntity(err);
                return;
            }

            UsageStatisticsReporting reporting = usageStatisticsReportingSqlMapDao.load();
            reporting.setLastReportedAt(new Date());
            usageStatisticsReportingSqlMapDao.saveOrUpdate(reporting);
            reportingStartedTime = null;
        }
    }

    private boolean isDevelopmentServer() {
        return !systemEnvironment.isProductionMode();
    }
}
