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

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineState;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.StubGoCache;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PipelineStateDaoTest {
    private GoCache goCache;
    private PipelineStateDao pipelineStateDao;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private GoConfigDao configFileDao;
    private org.hibernate.SessionFactory mockSessionFactory;
    private SqlMapClientTemplate mockTemplate;
    private Session session;

    @Before
    public void setup() throws Exception {
        transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        goCache = new StubGoCache(new TestTransactionSynchronizationManager());
        goCache.clear();
        mockTemplate = mock(SqlMapClientTemplate.class);
        mockSessionFactory = mock(SessionFactory.class);
        transactionTemplate = mock(TransactionTemplate.class);
        configFileDao = mock(GoConfigDao.class);
        pipelineStateDao = new PipelineStateDao(goCache, transactionTemplate, null,
                transactionSynchronizationManager, null, mock(Database.class), mockSessionFactory);
        pipelineStateDao.setSqlMapClientTemplate(mockTemplate);
        session = mock(Session.class);
        when(mockSessionFactory.getCurrentSession()).thenReturn(session);
        when(configFileDao.load()).thenReturn(GoConfigMother.defaultCruiseConfig());
    }

    @Test
    public void lockedPipelineCacheKey_shouldReturnTheSameInstanceForAGivenPipeline() {
        assertThat(pipelineStateDao.mutexForLockPipeline("DEV")).isSameAs(pipelineStateDao.mutexForLockPipeline("dev"));
    }

    @Test
    public void lockedPipeline_shouldReturnNullIfPipelineIsNotLocked() throws Exception {
        String pipelineName = UUID.randomUUID().toString();
        PipelineState pipelineState = pipelineStateDao.pipelineStateFor(pipelineName);

        assertThat(pipelineState).isNull();

        verify(transactionTemplate, times(1)).execute(any(org.springframework.transaction.support.TransactionCallback.class));
    }

    @Test
    public void lockPipeline_ShouldSavePipelineStateAndInvalidateCache() throws Exception {
        final List<TransactionSynchronizationAdapter> transactionSynchronizationAdapters = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TransactionSynchronizationAdapter adapter = (TransactionSynchronizationAdapter) invocation.getArguments()[0];
                transactionSynchronizationAdapters.add(adapter);
                return null;
            }
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));
        setupTransactionTemplate(transactionSynchronizationAdapters);

        final Pipeline pipeline = PipelineMother.pipeline("mingle");
        PipelineState pipelineState = new PipelineState(pipeline.getName(), pipeline.getFirstStage().getIdentifier());
        when(session.load(PipelineState.class, pipeline.getId())).thenReturn(pipelineState);
        pipelineStateDao.lockPipeline(pipeline);

        ArgumentCaptor<PipelineState> pipelineStateArgumentCaptor = ArgumentCaptor.forClass(PipelineState.class);
        verify(session).saveOrUpdate(pipelineStateArgumentCaptor.capture());
        PipelineState savedPipelineState = pipelineStateArgumentCaptor.getValue();
        assertThat(savedPipelineState.isLocked()).isTrue();
        assertThat(savedPipelineState.getLockedByPipelineId()).isEqualTo(pipeline.getId());
    }

    @Test
    public void unlockPipeline_shouldSavePipelineStateAndInvalidateCache() throws Exception {
        final List<TransactionSynchronizationAdapter> transactionSynchronizationAdapters = new ArrayList<>();
        doAnswer((invocation) -> {
            TransactionSynchronizationAdapter adapter = (TransactionSynchronizationAdapter) invocation.getArguments()[0];
            transactionSynchronizationAdapters.add(adapter);
            return null;
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));
        setupTransactionTemplate(transactionSynchronizationAdapters);

        final Pipeline pipeline = PipelineMother.pipeline("mingle");
        PipelineState pipelineState = new PipelineState(pipeline.getName(), pipeline.getFirstStage().getIdentifier());
        when(session.load(PipelineState.class, pipeline.getId())).thenReturn(pipelineState);
        pipelineStateDao.unlockPipeline(pipeline.getName());


        ArgumentCaptor<PipelineState> pipelineStateArgumentCaptor = ArgumentCaptor.forClass(PipelineState.class);
        verify(session).saveOrUpdate(pipelineStateArgumentCaptor.capture());
        PipelineState savedPipelineState = pipelineStateArgumentCaptor.getValue();
        assertThat(savedPipelineState.isLocked()).isFalse();
        assertThat(savedPipelineState.getLockedBy()).isNull();
    }

    private void setupTransactionTemplate(List<TransactionSynchronizationAdapter> transactionSynchronizationAdapters) {
        when(transactionTemplate.execute(any(org.springframework.transaction.support.TransactionCallbackWithoutResult.class))).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallbackWithoutResult callback = (org.springframework.transaction.support.TransactionCallbackWithoutResult) invocation.getArguments()[0];
            callback.doInTransaction(new SimpleTransactionStatus());
            for (TransactionSynchronizationAdapter synchronizationAdapter : transactionSynchronizationAdapters) {
                synchronizationAdapter.afterCommit();
            }
            return null;
        });
    }

}
