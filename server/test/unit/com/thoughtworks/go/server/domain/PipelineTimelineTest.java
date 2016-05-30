/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.listener.TimelineUpdateListener;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.helper.PipelineMaterialModificationMother;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PipelineTimelineTest {
    private DateTime now;
    private List<String> materials;
    private PipelineTimelineEntry first;
    private PipelineTimelineEntry second;
    private PipelineTimelineEntry third;
    private PipelineTimelineEntry fourth;
    private PipelineRepository pipelineRepository;
    private String pipelineName;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private TransactionSynchronization transactionSynchronization;
    private PipelineTimelineEntry[] repositoryEntries;
    private int txnStatus;


    @Before public void setUp() throws Exception {
        now = new DateTime();
        pipelineRepository = mock(PipelineRepository.class);
        materials = Arrays.asList("first", "second", "third", "fourth");
        first = PipelineMaterialModificationMother.modification(1, materials, Arrays.asList(now, now.plusMinutes(1), now.plusMinutes(2), now.plusMinutes(3)), 1, "111", "pipeline");
        second = PipelineMaterialModificationMother.modification(2, materials, Arrays.asList(now, now.plusMinutes(2), now.plusMinutes(1), now.plusMinutes(2)), 2, "222", "pipeline");
        third = PipelineMaterialModificationMother.modification(3, materials, Arrays.asList(now, now.plusMinutes(2), now.plusMinutes(1), now.plusMinutes(3)), 3, "333", "pipeline");
        fourth = PipelineMaterialModificationMother.modification(4, materials, Arrays.asList(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(2)), 4, "444", "pipeline");
        pipelineName = "pipeline";
        transactionTemplate = mock(TransactionTemplate.class);
        transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
    }

    @Test
    public void shouldReturnTheNextAndPreviousOfAGivenPipeline() throws Exception {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        mods.add(third);
        mods.add(second);
        mods.add(fourth);

        assertBeforeAfter(mods, first, null, null);
        assertBeforeAfter(mods, third, first, null);
        assertBeforeAfter(mods, second, first, third);
        assertBeforeAfter(mods, fourth, third, null);
    }

    private void assertBeforeAfter(PipelineTimeline mods, PipelineTimelineEntry actual, PipelineTimelineEntry before, PipelineTimelineEntry after) {
        PipelineTimelineEntry actualBefore = mods.runBefore(actual.getId(), new CaseInsensitiveString(pipelineName));
        PipelineTimelineEntry actualAfter = mods.runAfter(actual.getId(), new CaseInsensitiveString(pipelineName));

        assertEquals("Expected " + before + " to be before " + actual + ". Got " + actualBefore, actualBefore, before);
        assertEquals("Expected " + after + " to be after " + actual + ". Got " + actualAfter, actualAfter, after);
    }

    @Test public void shouldPopulateTheBeforeAndAfterNodesForAGivenPMMDuringAddition() throws Exception {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        mods.add(fourth);
        mods.add(third);
        mods.add(second);

        assertThat(third.insertedBefore(), is(fourth));
        assertThat(third.insertedAfter(), is(first));

        assertThat(second.insertedBefore(), is(third));
        assertThat(second.insertedAfter(), is(first));
    }

    @Test public void shouldReturnThePipelineBeforeAGivenPipelineId() throws Exception {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        mods.add(fourth);
        mods.add(third);
        mods.add(second);

        assertThat(mods.pipelineBefore(first.getId()), is(-1L));
        assertThat(mods.pipelineBefore(second.getId()), is(first.getId()));
        assertThat(mods.pipelineBefore(third.getId()), is(second.getId()));
        assertThat(mods.pipelineBefore(fourth.getId()), is(third.getId()));
    }

    @Test public void shouldReturnThePipelineAfterAGivenPipelineId() throws Exception {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        mods.add(fourth);
        mods.add(third);
        mods.add(second);

        assertThat(mods.pipelineAfter(first.getId()), is(second.getId()));
        assertThat(mods.pipelineAfter(second.getId()), is(third.getId()));
        assertThat(mods.pipelineAfter(third.getId()), is(fourth.getId()));
        assertThat(mods.pipelineAfter(fourth.getId()), is(-1L));
    }

    @Test public void shouldBeAbleToFindThePreviousPipelineForAGivenPipeline() throws Exception {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        mods.add(fourth);
        assertThat(mods.naturalOrderBefore(fourth), is(first));

        //bisect
        mods.add(third);
        assertThat(mods.naturalOrderBefore(fourth), is(third));
        assertThat(mods.naturalOrderBefore(third), is(first));

        //bisect
        mods.add(second);
        assertThat(mods.naturalOrderBefore(fourth), is(third));
        assertThat(mods.naturalOrderBefore(third), is(second));
        assertThat(mods.naturalOrderBefore(second), is(first));
    }

    @Test public void shouldPopuplateTheBeforeAndAfterNodesForAGivenPipelineDuringAddition() throws Exception {
        PipelineTimelineEntry anotherPipeline1 = PipelineMaterialModificationMother.modification("another", 4, materials, Arrays.asList(now, now.plusMinutes(1), now.plusMinutes(2), now.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry anotherPipeline2 = PipelineMaterialModificationMother.modification("another", 5, materials, Arrays.asList(now, now.plusMinutes(2), now.plusMinutes(1), now.plusMinutes(3)), 2, "123");
        PipelineTimelineEntry anotherPipeline3 = PipelineMaterialModificationMother.modification("another", 6, materials, Arrays.asList(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(2)), 3, "123");

        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);

        mods.add(first);
        mods.add(fourth);
        mods.add(anotherPipeline1);
        mods.add(third);
        mods.add(anotherPipeline3);
        mods.add(second);
        mods.add(anotherPipeline2);

        assertThat(third.insertedBefore(), is(fourth));
        assertThat(third.insertedAfter(), is(first));

        assertThat(second.insertedBefore(), is(third));
        assertThat(second.insertedAfter(), is(first));

        assertThat(anotherPipeline2.insertedBefore(), is(anotherPipeline3));
        assertThat(anotherPipeline2.insertedAfter(), is(anotherPipeline1));

        assertThat(mods.runAfter(anotherPipeline2.getId(), new CaseInsensitiveString("another")), is(anotherPipeline3));
        assertThat(mods.runBefore(anotherPipeline2.getId(), new CaseInsensitiveString("another")), is(anotherPipeline1));

        assertThat(mods.runAfter(first.getId(), new CaseInsensitiveString(first.getPipelineName())), is(nullValue()));
        assertThat(mods.runAfter(second.getId(), new CaseInsensitiveString(second.getPipelineName())), is(third));
    }

    @Test public void updateShouldNotifyListenersOnAddition() throws Exception {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);
        final List<PipelineTimelineEntry>[] entries = new List[1];
        entries[0] = new ArrayList<PipelineTimelineEntry>();
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager, new TimelineUpdateListener() {
            public void added(PipelineTimelineEntry newlyAddedEntry, TreeSet<PipelineTimelineEntry> timeline) {
                assertThat(timeline.contains(newlyAddedEntry), is(true));
                assertThat(timeline.containsAll(entries[0]), is(true));
                entries[0].add(newlyAddedEntry);
            }
        });
        stubPipelineRepository(timeline, true, new PipelineTimelineEntry[]{first, second});
        timeline.update();
        assertThat(entries[0].size(), is(1));
        assertThat(entries[0].contains(first), is(true));
    }

    @Test public void updateShouldIgnoreExceptionThrownByListenersDuringNotifications() throws Exception {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);
        TimelineUpdateListener anotherListener = mock(TimelineUpdateListener.class);
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager, new TimelineUpdateListener() {
            public void added(PipelineTimelineEntry newlyAddedEntry, TreeSet<PipelineTimelineEntry> timeline) {
                throw new RuntimeException();
            }
        }, anotherListener);
        stubPipelineRepository(timeline, true, new PipelineTimelineEntry[]{first, second});
        try {
            timeline.update();
        } catch (Exception e) {
            fail("should not have failed because of exception thrown by listener");
        }
        verify(anotherListener).added(eq(first), any(TreeSet.class));
    }

    @Test public void updateOnInitShouldBeDoneOutsideTransaction() throws Exception {
        PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        stubPipelineRepository(timeline, true, new PipelineTimelineEntry[]{first, second});
        timeline.updateTimelineOnInit();
        verify(pipelineRepository).updatePipelineTimeline(timeline);
        verifyNoMoreInteractions(transactionSynchronizationManager);
        verifyNoMoreInteractions(transactionTemplate);
        assertThat(timeline.maximumId(), is(2L));
        assertThat(timeline.pipelineAfter(1L), is(2L));
    }

    @Test public void updateShouldLoadNewInstancesFromTheDatabase() throws Exception {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);

        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        stubPipelineRepository(timeline, true, new PipelineTimelineEntry[]{first, second});
        timeline.update();
        verify(pipelineRepository).updatePipelineTimeline(timeline);
        assertThat(timeline.maximumId(), is(2L));
        assertThat(timeline.pipelineAfter(1L), is(2L));
    }

    @Test public void updateShouldRemoveTheTimelinesReturnedOnRollback() throws Exception {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_ROLLED_BACK, true);
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        stubPipelineRepository(timeline, true, new PipelineTimelineEntry[]{first, second});
        timeline.update();
        verify(pipelineRepository).updatePipelineTimeline(timeline);
        assertThat(timeline.maximumId(), is(-1L));
    }

    @Test
    public void shouldRemove_NewlyAddedTimelineEntries_fromAllCollections_UponRollback() throws Exception {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        stubPipelineRepository(timeline, true, first, second);
        timeline.update();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_ROLLED_BACK, false);
        stubPipelineRepository(timeline, false, third, fourth);
        timeline.update();
        assertThat(timeline.maximumId(), is(2L));
        assertThat(timeline.getEntriesFor("pipeline").size(), is(2));
        assertThat(timeline.getEntriesFor("pipeline"), hasItems(first, second));
        assertThat(timeline.instanceCount(new CaseInsensitiveString("pipeline")), is(2));
        assertThat(timeline.instanceFor(new CaseInsensitiveString("pipeline"), 0), is(first));
        assertThat(timeline.instanceFor(new CaseInsensitiveString("pipeline"), 1), is(second));
    }

    private void stubPipelineRepository(final PipelineTimeline timeline, boolean restub, final PipelineTimelineEntry... entries) {
        repositoryEntries = entries;
        if (restub) {
            when(pipelineRepository.updatePipelineTimeline(timeline)).thenAnswer(new Answer<Object>() {
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    for (PipelineTimelineEntry entry : repositoryEntries) {
                        timeline.add(entry);
                    }
                    return Arrays.asList(repositoryEntries);
                }
            });
        }
    }

    private void stubTransactionSynchronization() {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                transactionSynchronization = (TransactionSynchronization) invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));
    }

    private void setupTransactionTemplateStub(final int status, final boolean restub) throws Exception {
        this.txnStatus = status;
        if (restub) {
            when(transactionTemplate.execute(Mockito.any(TransactionCallback.class))).thenAnswer(new Answer<Object>() {
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    TransactionCallback callback = (TransactionCallback) invocationOnMock.getArguments()[0];
                    callback.doInTransaction(null);
                    if (txnStatus == TransactionSynchronization.STATUS_COMMITTED) {
                        transactionSynchronization.afterCommit();
                    }
                    transactionSynchronization.afterCompletion(txnStatus);
                    return null;
                }
            });
        }
    }

    @Test public void shouldReturnNullForPipelineBeforeAndAfterIfPipelineDoesNotExist() throws Exception {
        PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        timeline.add(first);
        assertThat(timeline.runBefore(2, new CaseInsensitiveString("not-present")), is(nullValue()));
        assertThat(timeline.runAfter(2, new CaseInsensitiveString("not-present")), is(nullValue()));
    }

    @Test
    public void shouldCreateANaturalOrderingHalfWayBetweenEachPipeline() throws Exception {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        assertThat(first.naturalOrder(), is(1.0));

        mods.add(fourth);
        assertThat(fourth.naturalOrder(), is(2.0));

        double thirdOrder = (2.0 + 1.0) / 2.0;
        mods.add(third);
        assertThat(third.naturalOrder(), is(thirdOrder));

        mods.add(second);
        assertThat(second.naturalOrder(), is((thirdOrder + 1.0) / 2.0));
    }


    @Test
    public void shouldCreateANaturalOrderingHalfWayBetweenEachPipelineWhenInsertedInReverseOrder() throws Exception {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(fourth);
        assertThat(fourth.naturalOrder(), is(1.0));

        mods.add(first);
        assertThat(first.naturalOrder(), is(0.5));

        double thirdOrder = (1.0 + 0.5) / 2.0;
        mods.add(third);
        assertThat(third.naturalOrder(), is(thirdOrder));

        mods.add(second);
        assertThat(second.naturalOrder(), is((thirdOrder + 0.5) / 2.0));
    }

    @Test
    public void shouldNotAllowResetingOfNaturalOrder() {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(fourth);
        mods.add(first);
        try {
            mods.add(fourth);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Calculated natural ordering 1.5 is not the same as the existing naturalOrder 1.0, for pipeline pipeline, with id 4"));
        }
     }

}
