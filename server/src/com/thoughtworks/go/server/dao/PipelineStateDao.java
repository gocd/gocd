/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.ibatis.sqlmap.client.SqlMapClient;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.PropertyProjection;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings({"ALL"})
@Component
public class PipelineStateDao extends SqlMapClientDaoSupport implements StageStatusListener {
    private static final Logger LOGGER = Logger.getLogger(PipelineStateDao.class);
    private StageDao stageDao;
    private MaterialRepository materialRepository;
    private EnvironmentVariableDao environmentVariableDao;
    private GoCache goCache;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private final SystemEnvironment systemEnvironment;
    private final GoConfigDao configFileDao;
    private SessionFactory sessionFactory;
    private final Cloner cloner = new Cloner();
    private final ReadWriteLock activePipelineRWLock = new ReentrantReadWriteLock();
    private final Lock activePipelineReadLock = activePipelineRWLock.readLock();
    private final Lock activePipelineWriteLock = activePipelineRWLock.writeLock();


    @Autowired
    public PipelineStateDao(StageDao stageDao, MaterialRepository materialRepository, GoCache goCache, EnvironmentVariableDao environmentVariableDao, TransactionTemplate transactionTemplate,
                            SqlMapClient sqlMapClient, TransactionSynchronizationManager transactionSynchronizationManager, SystemEnvironment systemEnvironment,
                            GoConfigDao configFileDao, Database database, SessionFactory sessionFactory) {
        super(goCache, sqlMapClient, systemEnvironment, database);
        this.stageDao = stageDao;
        this.materialRepository = materialRepository;
        this.goCache = goCache;
        this.environmentVariableDao = environmentVariableDao;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.systemEnvironment = systemEnvironment;
        this.configFileDao = configFileDao;
        this.sessionFactory = sessionFactory;
    }

    public void stageStatusChanged(Stage stage) {
        clearLockedPipelineStateCache(stage.getIdentifier().getPipelineName());
    }

    public void lockPipeline(final Pipeline pipeline) {
        synchronized (pipeline.getName().toLowerCase()) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    final String pipelineName = pipeline.getName();
                    PipelineState state = lockedPipeline(pipelineName);
                    if (state != null && state.isLocked() && !pipeline.getIdentifier().equals(state.getLockedBy().pipelineIdentifier())) {
                        throw new RuntimeException(String.format("Pipeline '%s' is already locked (counter = %s)", pipelineName, state.getLockedBy().getPipelineCounter()));
                    }
                    if (state == null) {
                        state = new PipelineState(pipelineName);
                    }
                    state.lock(pipeline.getId());
                    sessionFactory.getCurrentSession().saveOrUpdate(state);
                    clearLockedPipelineStateCache(pipelineName);
                }
            });
        }
    }

    private void clearLockedPipelineStateCache(String pipelineName) {
        goCache.remove(pipelineLockStateCacheKey(pipelineName));
    }

    public void unlockPipeline(final String pipelineName) {
        synchronized (pipelineName.toLowerCase()) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    final String cacheKey = pipelineLockStateCacheKey(pipelineName);
                    PipelineState state = lockedPipeline(pipelineName);
                    if (state == null) {
                        state = new PipelineState(pipelineName);
                    }
                    state.unlock();
                    sessionFactory.getCurrentSession().saveOrUpdate(state);
                    clearLockedPipelineStateCache(pipelineName);
                }
            });
        }
    }

    public PipelineState lockedPipeline(String pipelineName) {
        String cacheKey = pipelineLockStateCacheKey(pipelineName);
        PipelineState pipelineState = (PipelineState) goCache.get(cacheKey);
        if (pipelineState != null) {
            return pipelineState.equals(PipelineState.NOT_LOCKED) ? null : pipelineState;
        }
        synchronized (pipelineName.toLowerCase()) {
            pipelineState = (PipelineState) goCache.get(cacheKey);
            if (pipelineState != null) {
                return pipelineState.equals(PipelineState.NOT_LOCKED) ? null : pipelineState;
            }

            pipelineState = (PipelineState) transactionTemplate.execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(TransactionStatus transactionStatus) {
                    return sessionFactory.getCurrentSession()
                            .createCriteria(PipelineState.class)
                            .add(Restrictions.eq("pipelineName", pipelineName))
                            .setCacheable(true).uniqueResult();
                }
            });

            if (pipelineState != null && pipelineState.isLocked()) {
                StageIdentifier lockedBy = (StageIdentifier) getSqlMapClientTemplate().queryForObject("lockedPipeline", pipelineState.getLockedByPipelineId());
                pipelineState.setLockedBy(lockedBy);
            }
            goCache.put(cacheKey, pipelineState == null ? PipelineState.NOT_LOCKED : pipelineState);
            return pipelineState;
        }
    }

    String pipelineLockStateCacheKey(String pipelineName) {
        // we intern() it because we synchronize on the returned String
        return (PipelineStateDao.class.getName() + "_lockedPipeline_" + pipelineName.toLowerCase()).intern();
    }

    public List<String> lockedPipelines() {
        return (List<String>) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                PropertyProjection pipelineName = Projections.property("pipelineName");
                Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PipelineState.class).setProjection(pipelineName).add(
                        Restrictions.eq("locked", true));
                criteria.setCacheable(true);
                List<String> list = criteria.list();
                return list;
            }
        });
    }
}
