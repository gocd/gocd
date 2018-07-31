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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

@Component
public class DataSharingSettingsSqlMapDao extends HibernateDaoSupport {
    private final CacheKeyGenerator cacheKeyGenerator;
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private GoCache goCache;

    @Autowired
    public DataSharingSettingsSqlMapDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate, TransactionSynchronizationManager manager, GoCache goCache) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = manager;
        this.goCache = goCache;
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
        setSessionFactory(sessionFactory);
    }

    public void saveOrUpdate(DataSharingSettings dataSharingSettings) throws DuplicateDataSharingSettingsException {
        DataSharingSettings existing = load();

        if (dataSharingSettings.hasId() && dataSharingSettings.getId() != existing.getId()) {
            throw new DuplicateDataSharingSettingsException();
        }

        if (existing != null) {
            existing.copyFrom(dataSharingSettings);
        } else {
            existing = dataSharingSettings;
        }

        DataSharingSettings toSave = existing;
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        String key = cacheKeyForDataSharingSettings();
                        synchronized (key) {
                            goCache.remove(key);
                        }
                    }
                });

                sessionFactory.getCurrentSession().saveOrUpdate(toSave);
            }
        });
    }

    public DataSharingSettings load() {
        String cacheKey = cacheKeyForDataSharingSettings();
        DataSharingSettings settings = (DataSharingSettings) goCache.get(cacheKey);
        if (settings == null) {
            synchronized (cacheKey) {
                settings = transactionTemplate.execute(status -> (DataSharingSettings) sessionFactory.getCurrentSession().getNamedQuery("load.datasharing.settings").uniqueResult());
                goCache.put(cacheKey, settings);
            }
        }

        return settings;
    }

    private String cacheKeyForDataSharingSettings() {
        return cacheKeyGenerator.generate("dataSharing_settings");
    }

    public class DuplicateDataSharingSettingsException extends Exception {
    }
}
