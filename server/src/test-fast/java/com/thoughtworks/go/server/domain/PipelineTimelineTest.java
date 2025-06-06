/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.helper.PipelineTimelineEntryMother;
import com.thoughtworks.go.listener.TimelineUpdateListener;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class PipelineTimelineTest {
    private final ZonedDateTime now = ZonedDateTime.now();
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


    @BeforeEach
    public void setUp() {
        pipelineRepository = mock(PipelineRepository.class);
        materials = List.of("first", "second", "third", "fourth");
        first = PipelineTimelineEntryMother.modification(1, materials, List.of(now, now.plusMinutes(1), now.plusMinutes(2), now.plusMinutes(3)), 1, "111", "pipeline");
        second = PipelineTimelineEntryMother.modification(2, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(1), now.plusMinutes(2)), 2, "222", "pipeline");
        third = PipelineTimelineEntryMother.modification(3, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(1), now.plusMinutes(3)), 3, "333", "pipeline");
        fourth = PipelineTimelineEntryMother.modification(4, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(2)), 4, "444", "pipeline");
        pipelineName = "pipeline";
        transactionTemplate = mock(TransactionTemplate.class);
        transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
    }

    @Test
    public void shouldReturnTheNextAndPreviousOfAGivenPipeline() {
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

        assertEquals(actualBefore, before, "Expected " + before + " to be before " + actual + ". Got " + actualBefore);
        assertEquals(actualAfter, after, "Expected " + after + " to be after " + actual + ". Got " + actualAfter);
    }

    @Test
    public void shouldPopulateTheBeforeAndAfterNodesForAGivenPMMDuringAddition() {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        mods.add(fourth);
        mods.add(third);
        mods.add(second);

        assertThat(third.insertedBefore()).isEqualTo(fourth);
        assertThat(third.insertedAfter()).isEqualTo(first);

        assertThat(second.insertedBefore()).isEqualTo(third);
        assertThat(second.insertedAfter()).isEqualTo(first);
    }

    @Test
    public void shouldBeAbleToFindThePreviousPipelineForAGivenPipeline() {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        mods.add(fourth);
        assertThat(mods.naturalOrderBefore(fourth)).isEqualTo(first);

        //bisect
        mods.add(third);
        assertThat(mods.naturalOrderBefore(fourth)).isEqualTo(third);
        assertThat(mods.naturalOrderBefore(third)).isEqualTo(first);

        //bisect
        mods.add(second);
        assertThat(mods.naturalOrderBefore(fourth)).isEqualTo(third);
        assertThat(mods.naturalOrderBefore(third)).isEqualTo(second);
        assertThat(mods.naturalOrderBefore(second)).isEqualTo(first);
    }

    @Test
    public void shouldPopulateTheBeforeAndAfterNodesForAGivenPipelineDuringAddition() {
        PipelineTimelineEntry anotherPipeline1 = PipelineTimelineEntryMother.modification("another", 4, materials, List.of(now, now.plusMinutes(1), now.plusMinutes(2), now.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry anotherPipeline2 = PipelineTimelineEntryMother.modification("another", 5, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(1), now.plusMinutes(3)), 2, "123");
        PipelineTimelineEntry anotherPipeline3 = PipelineTimelineEntryMother.modification("another", 6, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(2)), 3, "123");

        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);

        mods.add(first);
        mods.add(fourth);
        mods.add(anotherPipeline1);
        mods.add(third);
        mods.add(anotherPipeline3);
        mods.add(second);
        mods.add(anotherPipeline2);

        assertThat(third.insertedBefore()).isEqualTo(fourth);
        assertThat(third.insertedAfter()).isEqualTo(first);

        assertThat(second.insertedBefore()).isEqualTo(third);
        assertThat(second.insertedAfter()).isEqualTo(first);

        assertThat(anotherPipeline2.insertedBefore()).isEqualTo(anotherPipeline3);
        assertThat(anotherPipeline2.insertedAfter()).isEqualTo(anotherPipeline1);

        assertThat(mods.runAfter(anotherPipeline2.getId(), new CaseInsensitiveString("another"))).isEqualTo(anotherPipeline3);
        assertThat(mods.runBefore(anotherPipeline2.getId(), new CaseInsensitiveString("another"))).isEqualTo(anotherPipeline1);

        assertThat(mods.runAfter(first.getId(), new CaseInsensitiveString(first.getPipelineName()))).isNull();
        assertThat(mods.runAfter(second.getId(), new CaseInsensitiveString(second.getPipelineName()))).isEqualTo(third);
    }

    @Test
    public void updateShouldNotifyListenersOnAddition() {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);
        final List<PipelineTimelineEntry> entries = new ArrayList<>();
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager, (newlyAddedEntry, timeline1) -> {
            assertThat(timeline1.contains(newlyAddedEntry)).isTrue();
            assertThat(timeline1.containsAll(entries)).isTrue();
            entries.add(newlyAddedEntry);
        });
        stubPipelineRepository(timeline, true, first, second);

        timeline.update();

        assertThat(entries.size()).isEqualTo(1);
        assertThat(entries.contains(first)).isTrue();
    }

    @Test
    public void updateShouldIgnoreExceptionThrownByListenersDuringNotifications() {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);
        TimelineUpdateListener anotherListener = mock(TimelineUpdateListener.class);
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager, (newlyAddedEntry, timeline1) -> {
            throw new RuntimeException();
        }, anotherListener);
        stubPipelineRepository(timeline, true, first, second);
        try {
            timeline.update();
        } catch (Exception e) {
            fail("should not have failed because of exception thrown by listener");
        }
        verify(anotherListener).added(eq(first), any());
    }

    @Test
    public void updateOnInitShouldBeDoneOutsideTransaction() {
        PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        PipelineTimelineEntry[] entries = {first, second};
        stubPipelineRepository(timeline, true, entries);

        timeline.updateTimelineOnInit();

        verify(pipelineRepository).updatePipelineTimeline(timeline, List.of(entries));
        verifyNoMoreInteractions(transactionSynchronizationManager);
        verifyNoMoreInteractions(transactionTemplate);
        assertThat(timeline.maximumId()).isEqualTo(2L);
    }

    @Test
    public void updateShouldLoadNewInstancesFromTheDatabase() {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);

        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        PipelineTimelineEntry[] entries = {first, second};
        stubPipelineRepository(timeline, true, entries);

        timeline.update();

        verify(pipelineRepository).updatePipelineTimeline(timeline, List.of(entries));
        assertThat(timeline.maximumId()).isEqualTo(2L);
    }

    @Test
    public void updateShouldRemoveTheTimelinesReturnedOnRollback() {
        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_ROLLED_BACK, true);
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        PipelineTimelineEntry[] entries = {first, second};
        stubPipelineRepository(timeline, true, first, second);

        timeline.update();

        verify(pipelineRepository).updatePipelineTimeline(timeline, List.of(entries));
        assertThat(timeline.maximumId()).isEqualTo(-1L);
    }

    @Test
    public void shouldRemove_NewlyAddedTimelineEntries_fromAllCollections_UponRollback() {
        Collection<PipelineTimelineEntry> allEntries;

        stubTransactionSynchronization();
        setupTransactionTemplateStub(TransactionSynchronization.STATUS_COMMITTED, true);
        final PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);

        stubPipelineRepository(timeline, true, first, second);
        timeline.update();
        allEntries = timeline.getEntriesFor("pipeline");

        setupTransactionTemplateStub(TransactionSynchronization.STATUS_ROLLED_BACK, false);

        stubPipelineRepository(timeline, false, third, fourth);
        timeline.update();
        allEntries = timeline.getEntriesFor("pipeline");

        assertThat(timeline.maximumId()).isEqualTo(2L);
        assertThat(timeline.getEntriesFor("pipeline").size()).isEqualTo(2);
        assertThat(allEntries).contains(first, second);
        assertThat(timeline.instanceCount(new CaseInsensitiveString("pipeline"))).isEqualTo(2);
        assertThat(timeline.instanceFor(new CaseInsensitiveString("pipeline"), 0)).isEqualTo(first);
        assertThat(timeline.instanceFor(new CaseInsensitiveString("pipeline"), 1)).isEqualTo(second);
    }

    @SuppressWarnings("unchecked")
    private void stubPipelineRepository(final PipelineTimeline timeline, boolean restub, final PipelineTimelineEntry... entries) {
        repositoryEntries = entries;
        if (restub) {
            doAnswer((Answer<Object>) invocationOnMock -> {
                for (PipelineTimelineEntry entry : repositoryEntries) {
                    timeline.add(entry);
                }
                ((List<PipelineTimelineEntry>) invocationOnMock.getArguments()[1]).addAll(List.of(repositoryEntries));
                return List.of(repositoryEntries);
            }).when(pipelineRepository).updatePipelineTimeline(eq(timeline), anyList());
        }
    }

    private void stubTransactionSynchronization() {
        doAnswer(invocationOnMock -> {
            transactionSynchronization = (TransactionSynchronization) invocationOnMock.getArguments()[0];
            return null;
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));
    }

    private void setupTransactionTemplateStub(final int status, final boolean restub) {
        this.txnStatus = status;
        if (restub) {
            when(transactionTemplate.execute(any())).thenAnswer(invocationOnMock -> {
                TransactionCallback<?> callback = (TransactionCallback<?>) invocationOnMock.getArguments()[0];
                callback.doInTransaction(null);
                if (txnStatus == TransactionSynchronization.STATUS_COMMITTED) {
                    transactionSynchronization.afterCommit();
                }
                transactionSynchronization.afterCompletion(txnStatus);
                return null;
            });
        }
    }

    @Test
    public void shouldReturnNullForPipelineBeforeAndAfterIfPipelineDoesNotExist() {
        PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        timeline.add(first);
        assertThat(timeline.runBefore(2, new CaseInsensitiveString("not-present"))).isNull();
        assertThat(timeline.runAfter(2, new CaseInsensitiveString("not-present"))).isNull();
    }

    @Test
    public void shouldCreateANaturalOrderingHalfWayBetweenEachPipeline() {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(first);
        assertThat(first.naturalOrder()).isEqualTo(1.0);

        mods.add(fourth);
        assertThat(fourth.naturalOrder()).isEqualTo(2.0);

        double thirdOrder = (2.0 + 1.0) / 2.0;
        mods.add(third);
        assertThat(third.naturalOrder()).isEqualTo(thirdOrder);

        mods.add(second);
        assertThat(second.naturalOrder()).isEqualTo((thirdOrder + 1.0) / 2.0);
    }


    @Test
    public void shouldCreateANaturalOrderingHalfWayBetweenEachPipelineWhenInsertedInReverseOrder() {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(fourth);
        assertThat(fourth.naturalOrder()).isEqualTo(1.0);

        mods.add(first);
        assertThat(first.naturalOrder()).isEqualTo(0.5);

        double thirdOrder = (1.0 + 0.5) / 2.0;
        mods.add(third);
        assertThat(third.naturalOrder()).isEqualTo(thirdOrder);

        mods.add(second);
        assertThat(second.naturalOrder()).isEqualTo((thirdOrder + 0.5) / 2.0);
    }

    @Test
    public void shouldNotAllowResetingOfNaturalOrder() {
        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.add(fourth);
        mods.add(first);
        try {
            mods.add(fourth);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Calculated natural ordering 1.5 is not the same as the existing naturalOrder 1.0, for pipeline pipeline, with id 4");
        }
    }

}
