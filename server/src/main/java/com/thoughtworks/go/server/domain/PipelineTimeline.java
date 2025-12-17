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
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Understands a sorted collection of PipelineMaterialModification
 */
@Component
public class PipelineTimeline {
    private final PipelineRepository pipelineRepository;
    private final TransactionTemplate transactionTemplate;
    private final TransactionSynchronizationManager transactionSynchronizationManager;

    private final Map<CaseInsensitiveString, NavigableSet<PipelineTimelineEntry>> naturalOrderPmm = new HashMap<>();
    private final Map<CaseInsensitiveString, List<PipelineTimelineEntry>> scheduleOrderPmm = new HashMap<>();
    private final ReadWriteLock naturalOrderLock = new ReentrantReadWriteLock();
    private final ReadWriteLock scheduleOrderLock = new ReentrantReadWriteLock();

    private final AtomicLong maximumId = new AtomicLong(-1);

    @Autowired
    public PipelineTimeline(PipelineRepository pipelineRepository, TransactionTemplate transactionTemplate, TransactionSynchronizationManager transactionSynchronizationManager) {
        this.pipelineRepository = pipelineRepository;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
    }

    @TestOnly
    public Collection<PipelineTimelineEntry> getEntriesFor(String pipelineName) {
        naturalOrderLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(naturalOrderPmm.getOrDefault(new CaseInsensitiveString(pipelineName), Collections.emptyNavigableSet()));
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    public long maximumId() {
        return maximumId.get();
    }

    public void add(PipelineTimelineEntry pipelineTimelineEntry) {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineTimelineEntry.getPipelineName());
        initializedNaturalOrderCollection(pipelineName).add(pipelineTimelineEntry);
        initializedScheduleOrderCollection(pipelineName).add(pipelineTimelineEntry);
        pipelineTimelineEntry.setInsertedBefore(naturalOrderAfter(pipelineTimelineEntry));
        pipelineTimelineEntry.setInsertedAfter(naturalOrderBefore(pipelineTimelineEntry));
        pipelineTimelineEntry.updateNaturalOrder();
        updateMaximumId(pipelineTimelineEntry.getId());
    }

    public void update() {
        acquireAllWriteLocks();
        try {
            final long maximumIdBeforeUpdate = maximumId.get();
            transactionTemplate.execute(transactionStatus -> {
                final List<PipelineTimelineEntry> newlyAddedEntries = new ArrayList<>();
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (STATUS_ROLLED_BACK == status) {
                            rollbackTempEntries();
                        }
                    }

                    private void rollbackTempEntries() {
                        for (PipelineTimelineEntry entry : newlyAddedEntries) {
                            rollbackNewEntryFor(entry);
                        }
                        maximumId.set(maximumIdBeforeUpdate);
                    }

                    private void rollbackNewEntryFor(PipelineTimelineEntry entry) {
                        CaseInsensitiveString pipelineName = new CaseInsensitiveString(entry.getPipelineName());
                        initializedNaturalOrderCollection(pipelineName).remove(entry);
                        initializedScheduleOrderCollection(pipelineName).remove(entry);
                    }


                });
                pipelineRepository.updatePipelineTimeline(PipelineTimeline.this, newlyAddedEntries);
                return null;
            });
        } finally {
            releaseAllWriteLocks();
        }
    }


    // --------------------------------------------------------
    // These methods should acquire and release lock in corresponding order.
    private void acquireAllWriteLocks() {
        naturalOrderLock.writeLock().lock();
        scheduleOrderLock.writeLock().lock();
    }

    //Release in the reverse of the acquired order.
    private void releaseAllWriteLocks() {
        scheduleOrderLock.writeLock().unlock();
        naturalOrderLock.writeLock().unlock();
    }

    /**
     * This is called on system init and is called by Spring. Hence, this is not done in a transaction. At any other time, the method update should be used
     */
    public void updateTimelineOnInit() {
        acquireAllWriteLocks();
        try {
            pipelineRepository.updatePipelineTimeline(this, new ArrayList<>());
        } finally {
            releaseAllWriteLocks();
        }
    }

    /**
     * @param id           for the pipeline
     * @param pipelineName name for the pipeline
     * @return PMM which was before the pipeline with this id at the time of insertion of the PTE with the id or null if there was nothing before this pipeline during insertion
     */
    public PipelineTimelineEntry runBefore(long id, final CaseInsensitiveString pipelineName) {
        naturalOrderLock.readLock().lock();
        try {
            Set<PipelineTimelineEntry> treeForPipeline = naturalOrderPmm.get(pipelineName);
            if (treeForPipeline == null) {
                return null;
            }
            for (PipelineTimelineEntry pipelineTimelineEntry : treeForPipeline) {
                if (id == pipelineTimelineEntry.getId()) {
                    return pipelineTimelineEntry.insertedAfter();
                }
            }
            throw new RuntimeException("Cannot find pipeline with id: " + id);
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    /**
     * @param id           id for the pipeline
     * @param pipelineName name for the pipeline
     * @return PMM which was after the pipeline with this id at the time of insertion of the PTE with the id or null if there was nothing after this pipeline during insertion
     */
    public PipelineTimelineEntry runAfter(long id, final CaseInsensitiveString pipelineName) {
        naturalOrderLock.readLock().lock();
        try {
            Set<PipelineTimelineEntry> treeForPipeline = naturalOrderPmm.get(pipelineName);
            if (treeForPipeline == null) {
                return null;
            }
            for (PipelineTimelineEntry pipelineTimelineEntry : treeForPipeline) {
                if (id == pipelineTimelineEntry.getId()) {
                    return pipelineTimelineEntry.insertedBefore();
                }
            }
            throw new RuntimeException("Cannot find pipeline with id: " + id);
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    private void updateMaximumId(long id) {
        maximumId.accumulateAndGet(id, Math::max);
    }

    private NavigableSet<PipelineTimelineEntry> initializedNaturalOrderCollection(final CaseInsensitiveString pipelineName) {
        return naturalOrderPmm.computeIfAbsent(pipelineName, k -> new TreeSet<>());
    }

    private List<PipelineTimelineEntry> initializedScheduleOrderCollection(final CaseInsensitiveString pipelineName) {
        return scheduleOrderPmm.computeIfAbsent(pipelineName, k -> new ArrayList<>());
    }

    private PipelineTimelineEntry naturalOrderAfter(PipelineTimelineEntry pipelineTimelineEntry) {
        naturalOrderLock.readLock().lock();
        try {
            return naturalOrderPmm.get(new CaseInsensitiveString(pipelineTimelineEntry.getPipelineName())).higher(pipelineTimelineEntry);
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    PipelineTimelineEntry naturalOrderBefore(PipelineTimelineEntry pipelineTimelineEntry) {
        naturalOrderLock.readLock().lock();
        try {
            return naturalOrderPmm.get(new CaseInsensitiveString(pipelineTimelineEntry.getPipelineName())).lower(pipelineTimelineEntry);
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    /**
     * No reason why you should use this apart from test tear down
     */
    @TestOnly
    public void clearWhichIsEvilAndShouldNotBeUsedInRealWorld() {
        acquireAllWriteLocks();
        try {
            naturalOrderPmm.clear();
            scheduleOrderPmm.clear();
        } finally {
            releaseAllWriteLocks();
        }
    }

    public int instanceCount(CaseInsensitiveString pipelineName) {
        scheduleOrderLock.readLock().lock();
        try {
            List<PipelineTimelineEntry> instances = scheduleOrderPmm.get(pipelineName);
            return instances == null ? 0 : instances.size();
        } finally {
            scheduleOrderLock.readLock().unlock();
        }
    }

    public PipelineTimelineEntry instanceFor(CaseInsensitiveString pipelineName, int index) {
        scheduleOrderLock.readLock().lock();
        try {
            List<PipelineTimelineEntry> instances = scheduleOrderPmm.get(pipelineName);
            return instances == null ? null : instances.get(index);
        } finally {
            scheduleOrderLock.readLock().unlock();
        }
    }


    public PipelineTimelineEntry getEntryFor(CaseInsensitiveString pipelineName, Integer pipelineCounter) {
        scheduleOrderLock.readLock().lock();
        try {
            List<PipelineTimelineEntry> instances = scheduleOrderPmm.get(pipelineName);
            for (int i = instances.size() - 1; i >= 0; i--) {
                PipelineTimelineEntry instance = instances.get(i);
                if (instance.getCounter() == pipelineCounter) {
                    return instance;
                }
            }
            return null;
        } finally {
            scheduleOrderLock.readLock().unlock();
        }
    }

}
