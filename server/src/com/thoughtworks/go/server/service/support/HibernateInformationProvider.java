/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.support;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;

@Component
public class HibernateInformationProvider implements ServerInfoProvider {

    private SessionFactory sessionFactory;

    @Autowired
    public HibernateInformationProvider(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public double priority() {
        return 7.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder builder) {
        builder.addSection(name());
        Statistics statistics = sessionFactory.getStatistics();
        builder.append(statistics);
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        Statistics statistics = sessionFactory.getStatistics();
        json.put("EntityDeleteCount", statistics.getEntityDeleteCount());
        json.put("EntityInsertCount", statistics.getEntityInsertCount());
        json.put("EntityLoadCount", statistics.getEntityLoadCount());
        json.put("EntityFetchCount", statistics.getEntityFetchCount());
        json.put("EntityUpdateCount", statistics.getEntityUpdateCount());
        json.put("QueryExecutionCount", statistics.getQueryExecutionCount());
        json.put("QueryExecutionMaxTime", statistics.getQueryExecutionMaxTime());
        json.put("QueryExecutionMaxTimeQueryString", statistics.getQueryExecutionMaxTimeQueryString());
        json.put("QueryCacheHitCount", statistics.getQueryCacheHitCount());
        json.put("QueryCacheMissCount", statistics.getQueryCacheMissCount());
        json.put("QueryCachePutCount", statistics.getQueryCachePutCount());
        json.put("FlushCount", statistics.getFlushCount());
        json.put("ConnectCount", statistics.getConnectCount());
        json.put("SecondLevelCacheHitCount", statistics.getSecondLevelCacheHitCount());
        json.put("SecondLevelCacheMissCount", statistics.getSecondLevelCacheMissCount());
        json.put("SecondLevelCachePutCount", statistics.getSecondLevelCachePutCount());
        json.put("SessionCloseCount", statistics.getSessionCloseCount());
        json.put("SessionOpenCount", statistics.getSessionOpenCount());
        json.put("CollectionLoadCount", statistics.getCollectionLoadCount());
        json.put("CollectionFetchCount", statistics.getCollectionFetchCount());
        json.put("CollectionUpdateCount", statistics.getCollectionUpdateCount());
        json.put("CollectionRemoveCount", statistics.getCollectionRemoveCount());
        json.put("CollectionRecreateCount", statistics.getCollectionRecreateCount());
        json.put("StartTime", statistics.getStartTime());
        json.put("SecondLevelCacheRegionNames", statistics.getSecondLevelCacheRegionNames());
        json.put("SuccessfulTransactionCount", statistics.getSuccessfulTransactionCount());
        json.put("TransactionCount", statistics.getTransactionCount());
        json.put("PrepareStatementCount", statistics.getPrepareStatementCount());
        json.put("CloseStatementCount", statistics.getCloseStatementCount());
        json.put("OptimisticFailureCount", statistics.getOptimisticFailureCount());

        LinkedHashMap<String, Object> queryStats = new LinkedHashMap<>();
        json.put("Queries", queryStats);

        String[] queries = statistics.getQueries();
        for (String query : queries) {
            queryStats.put(query, statistics.getQueryStatistics(query));
        }

        LinkedHashMap<String, Object> entityStatistics = new LinkedHashMap<>();
        json.put("EntityStatistics", entityStatistics);

        String[] entityNames = statistics.getEntityNames();
        for (String entityName : entityNames) {
            entityStatistics.put(entityName, statistics.getEntityStatistics(entityName));
        }

        LinkedHashMap<String, Object> roleStatistics = new LinkedHashMap<>();
        json.put("RoleStatistics", roleStatistics);

        String[] roleNames = statistics.getCollectionRoleNames();
        for (String roleName : roleNames) {
            roleStatistics.put(roleName, statistics.getCollectionStatistics(roleName));
        }

        return json;
    }

    @Override
    public String name() {
        return "Hibernate Statistics";
    }
}
