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
package com.thoughtworks.go.server.persistence;


import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.database.QueryExtensions;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.database.DatabaseStrategy;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.user.Filters;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PipelineRepositoryTest {

    private SessionFactory sessionFactory;
    private GoCache goCache;
    private HibernateTemplate hibernateTemplate;
    private PipelineRepository pipelineRepository;
    private DatabaseStrategy databaseStrategy;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private TransactionTemplate transactionTemplate;
    private Session session;
    private SQLQuery sqlQuery;

    @Before
    public void setup() {
        session = mock(Session.class);
        sqlQuery = mock(SQLQuery.class);
        sessionFactory = mock(SessionFactory.class);
        hibernateTemplate = mock(HibernateTemplate.class);
        goCache = mock(GoCache.class);
        databaseStrategy = mock(DatabaseStrategy.class);
        when(databaseStrategy.getQueryExtensions()).thenReturn(mock(QueryExtensions.class));
        pipelineRepository = new PipelineRepository(sessionFactory, databaseStrategy);
        pipelineRepository.setHibernateTemplate(hibernateTemplate);
        transactionTemplate = mock(TransactionTemplate.class);
        transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
    }


    private PipelineSelections makePipelineSelections() {
        return new PipelineSelections(Filters.defaults(), null, null);
    }

    @Test
    public void shouldUpdateTimelineEntriesAndEntriesForRollback() throws Exception {
        Object[] pipelineRow1 = {"p1", new BigInteger("1"), new BigInteger("1"), new Date(), "fingerprint", 1.0, "r1", null, new BigInteger("1"), new BigInteger("1")};
        Object[] pipelineRow2 = {"p1", new BigInteger("2"), new BigInteger("2"), new Date(), "fingerprint", 2.0, "r2", null, new BigInteger("1"), new BigInteger("1")};

        stubPipelineInstancesInDb(pipelineRow1, pipelineRow2);
        ArrayList<PipelineTimelineEntry> tempEntries = new ArrayList<>();
        PipelineTimeline pipelineTimeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);

        pipelineRepository.updatePipelineTimeline(pipelineTimeline, tempEntries);

        PipelineTimelineEntry timelineEntry1 = pipelineTimeline.getEntryFor(new CaseInsensitiveString("p1"), 1);
        PipelineTimelineEntry timelineEntry2 = pipelineTimeline.getEntryFor(new CaseInsensitiveString("p1"), 2);
        assertThat(pipelineTimeline.instanceCount(new CaseInsensitiveString("p1")), is(2));
        assertNotNull(timelineEntry1);
        assertNotNull(timelineEntry2);
        assertThat(tempEntries.size(), is(2));
        assertThat(tempEntries, containsInAnyOrder(timelineEntry1, timelineEntry2));
    }

    @Test
    public void shouldNotUpdateTimelineEntriesAndEntriesForRollbackUponFailureDuringRetrieval() throws Exception {
        Object[] pipelineRow1 = {"p1", new BigInteger("1"), new BigInteger("1"), new Date(), "fingerprint", 1.0, "r1", null, new BigInteger("1"), new BigInteger("1")};
        Object[] pipelineRow2 = {"p1", "cause-failure-during-retrieval", new BigInteger("2"), new Date(), "fingerprint", 2.0, "r2", null, new BigInteger("1"), new BigInteger("1")};

        stubPipelineInstancesInDb(pipelineRow1, pipelineRow2);
        ArrayList<PipelineTimelineEntry> tempEntries = new ArrayList<>();
        PipelineTimeline pipelineTimeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);

        try {
            pipelineRepository.updatePipelineTimeline(pipelineTimeline, tempEntries);
            fail("Should fail to retrieve pipeline.");
        } catch (ClassCastException e) {
            assertThat(tempEntries.size(), is(0));
            assertThat(pipelineTimeline.instanceCount(new CaseInsensitiveString("p1")), is(0));
        }
    }

    @Test
    public void shouldUpdateTimelineEntriesAndEntriesForRollbackDuringFailureWhileUpdatingTheDb() throws Exception {
        Object[] pipelineRow1 = {"p1", new BigInteger("1"), new BigInteger("1"), new Date(), "fingerprint", 1.0, "r1", null, new BigInteger("1"), new BigInteger("1")};
        Object[] pipelineRow2 = {"p1", new BigInteger("2"), new BigInteger("2"), new Date(), "fingerprint", 2.0, "r2", null, new BigInteger("1"), new BigInteger("1")};

        stubPipelineInstancesInDb(pipelineRow1, pipelineRow2);

        when(sqlQuery.executeUpdate()).thenThrow(new RuntimeException("Failure during update natural order in db"));
        ArrayList<PipelineTimelineEntry> tempEntries = new ArrayList<>();
        PipelineTimeline pipelineTimeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);

        try {
            pipelineRepository.updatePipelineTimeline(pipelineTimeline, tempEntries);
        } catch (RuntimeException e) {
            PipelineTimelineEntry timelineEntry1 = pipelineTimeline.getEntryFor(new CaseInsensitiveString("p1"), 1);
            PipelineTimelineEntry timelineEntry2 = pipelineTimeline.getEntryFor(new CaseInsensitiveString("p1"), 2);
            assertThat(pipelineTimeline.instanceCount(new CaseInsensitiveString("p1")), is(2));
            assertNotNull(timelineEntry1);
            assertNotNull(timelineEntry2);
            assertThat(tempEntries.size(), is(2));
            assertThat(tempEntries, containsInAnyOrder(timelineEntry1, timelineEntry2));
        }
    }

    private void stubPipelineInstancesInDb(Object[]... rows) {
        pipelineRepository.setHibernateTemplate(new HibernateTemplate() {
            @Override
            public <T> T execute(HibernateCallback<T> action) throws DataAccessException {
                try {
                    return action.doInHibernate(session);
                } catch (SQLException e) {
                    throw new RuntimeException();
                }
            }
        });
        when(session.createSQLQuery(nullable(String.class))).thenReturn(sqlQuery);
        when(sqlQuery.list()).thenReturn(Arrays.asList(rows));
    }
}
