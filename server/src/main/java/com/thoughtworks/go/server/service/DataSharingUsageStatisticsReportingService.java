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
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao.DuplicateMetricReporting;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service
public class DataSharingUsageStatisticsReportingService {
    private final UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    private final EntityHashingService entityHashingService;
    private final SystemEnvironment systemEnvironment;
    private final Object mutexForUsageStatisticsReporting = new Object();

    @Autowired
    public DataSharingUsageStatisticsReportingService(UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao,
                                                      EntityHashingService entityHashingService,
                                                      SystemEnvironment systemEnvironment) {
        this.usageStatisticsReportingSqlMapDao = usageStatisticsReportingSqlMapDao;
        this.entityHashingService = entityHashingService;
        this.systemEnvironment = systemEnvironment;
    }

    public void initialize() throws DuplicateMetricReporting {
        UsageStatisticsReporting existingUsageStatisticsReporting = usageStatisticsReportingSqlMapDao.load();

        if (existingUsageStatisticsReporting == null) {
            update(new UsageStatisticsReporting(UUID.randomUUID().toString(), new Date()));
        }

        if (new SystemEnvironment().shouldFailStartupOnDataError()) {
            assert get() != null;
        }
    }

    public UsageStatisticsReporting get() {
        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        loaded.setDataSharingServerUrl(systemEnvironment.getGoDataSharingServerUrl());

        return loaded;
    }

    public void update(UsageStatisticsReporting reporting) throws DuplicateMetricReporting {
        synchronized (mutexForUsageStatisticsReporting) {
            usageStatisticsReportingSqlMapDao.saveOrUpdate(reporting);
            entityHashingService.removeFromCache(reporting, reporting.getServerId());
        }
    }
}
