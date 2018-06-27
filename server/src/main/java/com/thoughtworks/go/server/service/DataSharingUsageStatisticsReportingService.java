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

import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao;
import com.thoughtworks.go.util.SystemEnvironment;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static com.thoughtworks.go.util.DateUtils.isToday;


@Service
public class DataSharingUsageStatisticsReportingService {
    private final UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    private DataSharingSettingsService dataSharingSettingsService;
    private final SystemEnvironment systemEnvironment;
    private final Object mutexForReportingUsageData = new Object();

    private DateTime reportingStartedTime = null;

    @Autowired
    public DataSharingUsageStatisticsReportingService(UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao,
                                                      DataSharingSettingsService dataSharingSettingsService,
                                                      SystemEnvironment systemEnvironment) {
        this.usageStatisticsReportingSqlMapDao = usageStatisticsReportingSqlMapDao;
        this.dataSharingSettingsService = dataSharingSettingsService;
        this.systemEnvironment = systemEnvironment;
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
        UsageStatisticsReporting reporting = new UsageStatisticsReporting(UUID.randomUUID().toString(), new Date());
        usageStatisticsReportingSqlMapDao.saveOrUpdate(reporting);
    }

    public UsageStatisticsReporting get() {
        synchronized (mutexForReportingUsageData) {
            UsageStatisticsReporting loaded = getUsageStatisticsReportingInfo();
            if (loaded.canReport()) {
                reportingStartedTime = new DateTime();
            }
            return loaded;
        }
    }

    private UsageStatisticsReporting getUsageStatisticsReportingInfo() {
        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        loaded.setDataSharingServerUrl(systemEnvironment.getGoDataSharingServerUrl());
        boolean canReport = !isDevelopmentServer() && dataSharingSettingsService.get().allowSharing()
                && !isToday(loaded.lastReportedAt()) && !isReportingInProgressFresh();
        loaded.canReport(canReport);
        return loaded;
    }

    private boolean isReportingInProgressFresh() {
        if (reportingStartedTime == null) {
            return false;
        }

        DateTime halfHourAgo = new DateTime(System.currentTimeMillis() - 30 * 60 * 1000);
        return reportingStartedTime.isAfter(halfHourAgo);
    }

    public UsageStatisticsReporting updateLastReportedTime() {
        synchronized (mutexForReportingUsageData) {
            UsageStatisticsReporting reporting = usageStatisticsReportingSqlMapDao.load();
            reporting.setLastReportedAt(new Date());
            usageStatisticsReportingSqlMapDao.saveOrUpdate(reporting);
            reportingStartedTime = null;
            return getUsageStatisticsReportingInfo();
        }
    }

    private boolean isDevelopmentServer() {
        return !systemEnvironment.isProductionMode();
    }
}
