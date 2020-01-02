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

import com.thoughtworks.go.listener.DataSharingSettingsChangeListener;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DataSharingSettingsService implements DataSharingSettingsChangeListener {
    private final Object mutexForDataSharingSettings = new Object();

    private final DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private final EntityHashingService entityHashingService;
    private final List<DataSharingSettingsChangeListener> dataSharingSettingsChangeListeners;

    @Autowired
    public DataSharingSettingsService(DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao,
                                      TransactionTemplate transactionTemplate,
                                      TransactionSynchronizationManager transactionSynchronizationManager,
                                      EntityHashingService entityHashingService) {
        this.dataSharingSettingsSqlMapDao = dataSharingSettingsSqlMapDao;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.entityHashingService = entityHashingService;
        dataSharingSettingsChangeListeners = new ArrayList<>();
    }

    public void initialize() {
        DataSharingSettings existingDataSharingSettings = get();

        if (existingDataSharingSettings == null) {
            createOrUpdate(new DataSharingSettings(true, "Default", new Date()));
        }

        if (new SystemEnvironment().shouldFailStartupOnDataError()) {
            assert get() != null;
        }

        register(this);
    }

    public DataSharingSettings get() {
        return dataSharingSettingsSqlMapDao.load();
    }

    public void createOrUpdate(DataSharingSettings dataSharingSettings) {
        synchronized (mutexForDataSharingSettings) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            entityHashingService.removeFromCache(dataSharingSettings, Long.toString(dataSharingSettings.getId()));
                            dataSharingSettingsSqlMapDao.invalidateCache();
                        }
                    });
                    try {
                        dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings);
                    } catch (DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            dataSharingSettingsChangeListeners
                    .stream()
                    .forEach(listener -> listener.onDataSharingSettingsChange(dataSharingSettings));
        }
    }

    @Override
    public void onDataSharingSettingsChange(DataSharingSettings updatedSettings) {
        entityHashingService.removeFromCache(updatedSettings, "data_sharing_settings");
    }

    public void register(DataSharingSettingsChangeListener listener) {
        dataSharingSettingsChangeListeners.add(listener);
    }
}
