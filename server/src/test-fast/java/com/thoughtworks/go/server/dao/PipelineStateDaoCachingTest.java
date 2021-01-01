/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineState;
import com.thoughtworks.go.domain.StageIdentifier;
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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.domain.PipelineState.NOT_LOCKED;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PipelineStateDaoCachingTest {
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
        assertSame(
                pipelineStateDao.pipelineLockStateCacheKey("dev"),
                pipelineStateDao.pipelineLockStateCacheKey("dev"));
    }

    @Test
    public void lockedPipeline_shouldCacheLockedPipelineStatus() throws Exception {
        String pipelineName = "mingle";
        when(transactionTemplate.execute(any(org.springframework.transaction.support.TransactionCallback.class))).thenReturn(new PipelineState(pipelineName, new StageIdentifier(pipelineName, 1, "1", 1L, "s1", "1")));

        PipelineState pipelineState = pipelineStateDao.pipelineStateFor(pipelineName);

        assertThat(goCache.get(pipelineStateDao.pipelineLockStateCacheKey(pipelineName)), is(pipelineState));
        PipelineState secondAttempt = pipelineStateDao.pipelineStateFor(pipelineName);

        assertSame(pipelineState, secondAttempt);
        verify(transactionTemplate, times(1)).execute(any(TransactionCallback.class));
    }

    @Test
    public void lockedPipeline_shouldReturnNullIfPipelineIsNotLocked() throws Exception {
        String pipelineName = UUID.randomUUID().toString();
        pipelineStateDao.pipelineStateFor(pipelineName);
        PipelineState actual = pipelineStateDao.pipelineStateFor(pipelineName);

        assertNull("got " + actual, actual);
        assertThat(goCache.get(pipelineStateDao.pipelineLockStateCacheKey(pipelineName)), is(NOT_LOCKED));

        verify(transactionTemplate, times(1)).execute(any(org.springframework.transaction.support.TransactionCallback.class));
    }

    @Test
    public void lockPipeline_ShouldSavePipelineStateAndInvalidateCache() throws Exception {
        final List<TransactionSynchronizationAdapter> transactionSynchronizationAdapters = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TransactionSynchronizationAdapter adapter= (TransactionSynchronizationAdapter) invocation.getArguments()[0];
                transactionSynchronizationAdapters.add(adapter);
                return null;
            }
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));
        setupTransactionTemplate(transactionSynchronizationAdapters);

        final Pipeline pipeline = PipelineMother.pipeline("mingle");
        PipelineState pipelineState = new PipelineState(pipeline.getName(), pipeline.getFirstStage().getIdentifier());
        when(session.load(PipelineState.class, pipeline.getId())).thenReturn(pipelineState);
        goCache.put(pipelineStateDao.pipelineLockStateCacheKey(pipeline.getName()), pipelineState);
        pipelineStateDao.lockPipeline(pipeline);

        assertThat(goCache.get(pipelineStateDao.pipelineLockStateCacheKey(pipeline.getName())), is(nullValue()));

        ArgumentCaptor<PipelineState> pipelineStateArgumentCaptor = ArgumentCaptor.forClass(PipelineState.class);
        verify(session).saveOrUpdate(pipelineStateArgumentCaptor.capture());
        PipelineState savedPipelineState = pipelineStateArgumentCaptor.getValue();
        assertThat(savedPipelineState.isLocked(), is(true));
        assertThat(savedPipelineState.getLockedByPipelineId(), is(pipeline.getId()));
    }

    @Test
    public void lockPipeline_shouldHandleSerializationProperly() throws Exception {
        when(mockTemplate.queryForObject(eq("lockedPipeline"), any())).thenReturn(null);
        assertNull(pipelineStateDao.pipelineStateFor("mingle"));
        goCache.put(pipelineStateDao.pipelineLockStateCacheKey("mingle"), NOT_LOCKED);
        assertNull(pipelineStateDao.pipelineStateFor("mingle"));
    }

    @Test
    public void unlockPipeline_shouldSavePipelineStateAndInvalidateCache() throws Exception {
        final List<TransactionSynchronizationAdapter> transactionSynchronizationAdapters = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TransactionSynchronizationAdapter adapter= (TransactionSynchronizationAdapter) invocation.getArguments()[0];
                transactionSynchronizationAdapters.add(adapter);
                return null;
            }
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));
        setupTransactionTemplate(transactionSynchronizationAdapters);

        final Pipeline pipeline = PipelineMother.pipeline("mingle");
        PipelineState pipelineState = new PipelineState(pipeline.getName(), pipeline.getFirstStage().getIdentifier());
        goCache.put(pipelineStateDao.pipelineLockStateCacheKey(pipeline.getName()), pipelineState);
        when(session.load(PipelineState.class, pipeline.getId())).thenReturn(pipelineState);
        pipelineStateDao.unlockPipeline(pipeline.getName());

        assertThat(goCache.get(pipelineStateDao.pipelineLockStateCacheKey(pipeline.getName())), is(nullValue()));

        ArgumentCaptor<PipelineState> pipelineStateArgumentCaptor = ArgumentCaptor.forClass(PipelineState.class);
        verify(session).saveOrUpdate(pipelineStateArgumentCaptor.capture());
        PipelineState savedPipelineState = pipelineStateArgumentCaptor.getValue();
        assertThat(savedPipelineState.isLocked(), is(false));
        assertThat(savedPipelineState.getLockedBy(), is(nullValue()));
    }

    private void setupTransactionTemplate(List<TransactionSynchronizationAdapter> transactionSynchronizationAdapters) {
        when(transactionTemplate.execute(any(org.springframework.transaction.support.TransactionCallbackWithoutResult.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                org.springframework.transaction.support.TransactionCallbackWithoutResult callback = (org.springframework.transaction.support.TransactionCallbackWithoutResult) invocation.getArguments()[0];
                callback.doInTransaction(new SimpleTransactionStatus());
                for (TransactionSynchronizationAdapter synchronizationAdapter : transactionSynchronizationAdapters) {
                    synchronizationAdapter.afterCommit();
                }
                return null;
            }
        });
    }

}
