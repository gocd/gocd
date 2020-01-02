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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
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
public class UsageStatisticsReportingSqlMapDao extends HibernateDaoSupport {
    private final CacheKeyGenerator cacheKeyGenerator;
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager synchronizationManager;
    private GoCache goCache;

    @Autowired
    public UsageStatisticsReportingSqlMapDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate, TransactionSynchronizationManager synchronizationManager, GoCache goCache) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        this.synchronizationManager = synchronizationManager;
        this.goCache = goCache;
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
        setSessionFactory(sessionFactory);
    }

    public void saveOrUpdate(UsageStatisticsReporting usageStatisticsReporting) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        String cacheKey = cacheKeyForUsageStatisticsReporting();
                        synchronized (cacheKey) {
                            goCache.remove(cacheKey);
                        }
                    }
                });

                sessionFactory.getCurrentSession().saveOrUpdate(usageStatisticsReporting);
            }
        });
    }

    public UsageStatisticsReporting load() {
        String cacheKey = cacheKeyForUsageStatisticsReporting();
        UsageStatisticsReporting reporting = (UsageStatisticsReporting) goCache.get(cacheKey);
        if (reporting == null) {
            synchronized (cacheKey) {
                if (reporting == null) {
                    reporting = transactionTemplate.execute(status -> (UsageStatisticsReporting) sessionFactory.getCurrentSession().getNamedQuery("load.usagestatistics.reporting.information").uniqueResult());
                    goCache.put(cacheKey, reporting);
                }
            }
        }

        return reporting;
    }

    private String cacheKeyForUsageStatisticsReporting() {
        return cacheKeyGenerator.generate("dataSharing_reporting");
    }

    public class DuplicateMetricReporting extends Exception {
    }
}
