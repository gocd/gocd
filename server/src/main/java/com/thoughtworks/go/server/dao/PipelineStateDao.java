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

import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineState;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.AfterCompletionCallback;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.List;

@Component
public class PipelineStateDao extends SqlMapClientDaoSupport {
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private SessionFactory sessionFactory;
    private CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    public PipelineStateDao(GoCache goCache,
                            TransactionTemplate transactionTemplate,
                            SqlSessionFactory sqlSessionFactory,
                            TransactionSynchronizationManager transactionSynchronizationManager,
                            SystemEnvironment systemEnvironment,
                            Database database,
                            SessionFactory sessionFactory) {
        super(goCache, sqlSessionFactory, systemEnvironment, database);
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.sessionFactory = sessionFactory;
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
    }

    public void lockPipeline(final Pipeline pipeline, AfterCompletionCallback... callbacks) {
        synchronized (mutexForLockPipeline(pipeline.getName())) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCompletion(int status) {
                            for (AfterCompletionCallback callback : callbacks) {
                                callback.execute(status);
                            }
                        }
                    });
                    final String pipelineName = pipeline.getName();
                    PipelineState fromCache = pipelineStateFor(pipelineName);
                    if (fromCache != null && fromCache.isLocked() && !pipeline.getIdentifier().equals(fromCache.getLockedBy().pipelineIdentifier())) {
                        throw new RuntimeException(String.format("Pipeline '%s' is already locked (counter = %s)", pipelineName, fromCache.getLockedBy().getPipelineCounter()));
                    }
                    PipelineState toBeSaved;
                    if (fromCache == null) {
                        toBeSaved = new PipelineState(pipelineName);
                    } else {
                        toBeSaved = (PipelineState) sessionFactory.getCurrentSession().get(PipelineState.class, fromCache.getId());
                    }
                    toBeSaved.lock(pipeline.getId());
                    sessionFactory.getCurrentSession().saveOrUpdate(toBeSaved);
                }
            });
        }
    }

    public void unlockPipeline(final String pipelineName, AfterCompletionCallback... afterCompletionCallbacks) {
        synchronized (mutexForLockPipeline(pipelineName)) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCompletion(int status) {
                            for (AfterCompletionCallback callback : afterCompletionCallbacks) {
                                callback.execute(status);
                            }
                        }
                    });

                    PipelineState fromCache = pipelineStateFor(pipelineName);
                    PipelineState toBeSaved;
                    if (fromCache == null) {
                        toBeSaved = new PipelineState(pipelineName);
                    } else {
                        toBeSaved = (PipelineState) sessionFactory.getCurrentSession().get(PipelineState.class, fromCache.getId());
                    }
                    toBeSaved.unlock();
                    sessionFactory.getCurrentSession().saveOrUpdate(toBeSaved);
                }
            });
        }
    }

    public PipelineState pipelineStateFor(String pipelineName) {
        synchronized (mutexForLockPipeline(pipelineName)) {
            PipelineState pipelineState = transactionTemplate.execute(status ->
                    (PipelineState) sessionFactory.getCurrentSession()
                            .createCriteria(PipelineState.class)
                            .add(Restrictions.eq("pipelineName", pipelineName))
                            .setCacheable(true)
                            .uniqueResult());

            if (pipelineState != null && pipelineState.isLocked()) {
                StageIdentifier lockedBy = (StageIdentifier) getSqlMapClientTemplate().queryForObject("lockedPipeline", pipelineState.getLockedByPipelineId());
                pipelineState.setLockedBy(lockedBy);
            }
            return pipelineState;
        }
    }

    String mutexForLockPipeline(String pipelineName) {
        return cacheKeyGenerator.generate("lockedPipeline", pipelineName.toLowerCase());
    }

    public List<String> lockedPipelines() {
        return transactionTemplate.execute(status -> {

            Criteria criteria = sessionFactory.getCurrentSession()
                    .createCriteria(PipelineState.class)
                    .setProjection(Projections.property("pipelineName"))
                    .add(Restrictions.eq("locked", true))
                    .setCacheable(true);

            return (List<String>) criteria.list();
        });
    }
}
