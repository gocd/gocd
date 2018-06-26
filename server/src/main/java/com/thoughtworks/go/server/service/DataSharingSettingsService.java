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

import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DataSharingSettingsService {
    private final Object mutexForDataSharingSettings = new Object();

    private final DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    private final EntityHashingService entityHashingService;

    @Autowired
    public DataSharingSettingsService(DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao, EntityHashingService entityHashingService) {
        this.dataSharingSettingsSqlMapDao = dataSharingSettingsSqlMapDao;
        this.entityHashingService = entityHashingService;
    }

    public void initialize() throws DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException {
        DataSharingSettings existingDataSharingSettings = dataSharingSettingsSqlMapDao.load();

        if (existingDataSharingSettings == null) {
            update(new DataSharingSettings(true, "Default", new Date()));
        }

        if (new SystemEnvironment().shouldFailStartupOnDataError()) {
            assert get() != null;
        }
    }

    public DataSharingSettings get() {
        return dataSharingSettingsSqlMapDao.load();
    }

    public void update(DataSharingSettings dataSharingSettings) throws DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException {
        synchronized (mutexForDataSharingSettings) {
            dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings);
            entityHashingService.removeFromCache(dataSharingSettings, Long.toString(dataSharingSettings.getId()));
        }
    }
}
