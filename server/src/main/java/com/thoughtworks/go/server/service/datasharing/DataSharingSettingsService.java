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

import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;

@Service
public class DataSharingSettingsService {

    private final DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;

    @Autowired
    public DataSharingSettingsService(DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao) {
        this.dataSharingSettingsSqlMapDao = dataSharingSettingsSqlMapDao;
    }

    public void initialize() {
        DataSharingSettings existingDataSharingSettings = load();

        if (existingDataSharingSettings == null) {
            createOrUpdate(new DataSharingSettings().setAllowSharing(true)
                    .setUpdatedBy("Default")
                    .setUpdatedOn(new Timestamp(new Date().getTime())));
        }
    }

    public DataSharingSettings load() {
        return dataSharingSettingsSqlMapDao.load();
    }

    public void createOrUpdate(DataSharingSettings dataSharingSettings) {
        dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings);
    }
}
