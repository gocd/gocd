/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.listener.TimelineUpdateListener;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @understands a sorted collection of PipelineMaterialModification
 */
@Component
public class PipelineTimeline {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineTimeline.class);

    private final Map<CaseInsensitiveString, PipelineTimelineEntrySet> pipelineToEntries;
    private final PipelineRepository pipelineRepository;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private TimelineUpdateListener[] listeners;
    private final ReadWriteLock naturalOrderLock = new ReentrantReadWriteLock();
    private final ReadWriteLock scheduleOrderLock = new ReentrantReadWriteLock();
    private final Cloner cloner = new Cloner();

    @Autowired
    public PipelineTimeline(PipelineRepository pipelineRepository, TransactionTemplate transactionTemplate, TransactionSynchronizationManager transactionSynchronizationManager,
                            TimelineUpdateListener... listeners) {
        this.pipelineRepository = pipelineRepository;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.listeners = listeners;
        pipelineToEntries = new HashMap<>();
    }

    /**
     * @deprecated Used only in tests
     */
    @Deprecated
    public Collection<PipelineTimelineEntry> getEntriesFor(String pipelineName) {
        naturalOrderLock.readLock().lock();
        try {
            TreeSet<PipelineTimelineEntry> tree = pipelineToEntries.get(new CaseInsensitiveString(pipelineName)).getNaturalOrderSet();
            if (tree == null) {
                tree = new TreeSet<>();
            }
            return Collections.unmodifiableCollection(cloner.deepClone(tree));
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    public long getMaximumIdFor(String pipelineName) {
        PipelineTimelineEntrySet pipelineTimelineEntrySet = pipelineToEntries.get(new CaseInsensitiveString(pipelineName));

        return pipelineTimelineEntrySet == null ? -1 : pipelineTimelineEntrySet.getMaximumId();
    }

    public void add(PipelineTimelineEntry pipelineTimelineEntry) {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineTimelineEntry.getPipelineName());
        PipelineTimelineEntrySet pipelineTimelineEntrySet = initializePipelineEntriesFor(pipelineName);
        pipelineTimelineEntrySet.getNaturalOrderSet().add(pipelineTimelineEntry);
        pipelineTimelineEntrySet.getScheduledOrderSet().add(pipelineTimelineEntry);
        pipelineTimelineEntry.setInsertedBefore(naturalOrderAfter(pipelineTimelineEntry));
        pipelineTimelineEntry.setInsertedAfter(naturalOrderBefore(pipelineTimelineEntry));
        pipelineTimelineEntry.updateNaturalOrder();
        pipelineTimelineEntrySet.updateMaximumId(pipelineTimelineEntry.getId());
    }

    public void update(final String pipelineName) {
        acquireAllWriteLocks();
        try {
            PipelineTimelineEntrySet pipelineTimelineEntrySet = initializePipelineEntriesFor(new CaseInsensitiveString(pipelineName));
            final long maximumIdBeforeUpdate = pipelineTimelineEntrySet.getMaximumId();
            transactionTemplate.execute((TransactionCallback) transactionStatus -> {
                final List<PipelineTimelineEntry> newlyAddedEntries = new ArrayList<>();
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (STATUS_ROLLED_BACK == status) {
                            rollbackTempEntries();
                        } else if (STATUS_COMMITTED == status) {
                            notifyListeners(newlyAddedEntries);
                        }
                    }

                    private void rollbackTempEntries() {
                        for (PipelineTimelineEntry entry : newlyAddedEntries) {
                            rollbackNewEntryFor(entry);
                        }
                        pipelineTimelineEntrySet.setMaximumId(maximumIdBeforeUpdate);
                    }

                    private void rollbackNewEntryFor(PipelineTimelineEntry entry) {
                        CaseInsensitiveString pipelineName = new CaseInsensitiveString(entry.getPipelineName());
                        PipelineTimelineEntrySet pipelineTimelineEntrySet = initializePipelineEntriesFor(pipelineName);
                        pipelineTimelineEntrySet.getNaturalOrderSet().remove(entry);
                        pipelineTimelineEntrySet.getScheduledOrderSet().remove(entry);
                    }


                });
                pipelineRepository.updatePipelineTimeline(PipelineTimeline.this, newlyAddedEntries, pipelineName);
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
    // --------------------------------------------------------

    private void notifyListeners(List<PipelineTimelineEntry> newEntries) {
        Map<CaseInsensitiveString, PipelineTimelineEntry> pipelineToOldestEntry = new HashMap<>();
        for (PipelineTimelineEntry challenger : newEntries) {
            CaseInsensitiveString pipelineName = new CaseInsensitiveString(challenger.getPipelineName());
            PipelineTimelineEntry champion = pipelineToOldestEntry.get(pipelineName);
            if (champion == null || challenger.compareTo(champion) < 0) {
                pipelineToOldestEntry.put(pipelineName, challenger);
            }
        }

        for (TimelineUpdateListener listener : listeners) {
            for (Map.Entry<CaseInsensitiveString, PipelineTimelineEntry> entry : pipelineToOldestEntry.entrySet()) {
                try {
                    listener.added(entry.getValue(), pipelineToEntries.get(entry.getKey()).getNaturalOrderSet());
                } catch (Exception e) {
                    LOGGER.warn("Ignoring exception when notifying listener: {}", listener, e);
                }
            }
        }
    }

    /**
     * @param id           for the pipeline
     * @param pipelineName
     * @return PMM which was before the pipeline with this id at the time of insertion of the PTE with the id or null if there was nothing before this pipeline during insertion
     */
    public PipelineTimelineEntry runBefore(long id, final CaseInsensitiveString pipelineName) {
        populatePipelineTimelineEntriesFor(pipelineName);
        naturalOrderLock.readLock().lock();
        try {
            TreeSet<PipelineTimelineEntry> treeForPipeline = pipelineToEntries.get(pipelineName).getNaturalOrderSet();
            if (treeForPipeline == null || treeForPipeline.isEmpty()) {
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

    private void populatePipelineTimelineEntriesFor(CaseInsensitiveString pipelineName) {
        PipelineTimelineEntrySet pipelineTimelineEntrySet;
        naturalOrderLock.readLock().lock();
        try {
            pipelineTimelineEntrySet = pipelineToEntries.get(pipelineName);
        } finally {
            naturalOrderLock.readLock().unlock();
        }
        if (pipelineTimelineEntrySet == null) {
            update(pipelineName.toString());
        }
    }

    /**
     * @param id           for the pipeline
     * @param pipelineName
     * @return PMM which was after the pipeline with this id at the time of insertion of the PTE with the id or null if there was nothing after this pipeline during insertion
     */
    public PipelineTimelineEntry runAfter(long id, final CaseInsensitiveString pipelineName) {
        populatePipelineTimelineEntriesFor(pipelineName);
        naturalOrderLock.readLock().lock();
        try {
            TreeSet<PipelineTimelineEntry> treeForPipeline = pipelineToEntries.get(pipelineName).getNaturalOrderSet();
            if (treeForPipeline == null || treeForPipeline.isEmpty()) {
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

    private PipelineTimelineEntrySet initializePipelineEntriesFor(final CaseInsensitiveString pipelineName) {
        if (!pipelineToEntries.containsKey(pipelineName)) {
            pipelineToEntries.put(pipelineName, new PipelineTimelineEntrySet());
        }
        return pipelineToEntries.get(pipelineName);
    }

    private PipelineTimelineEntry naturalOrderAfter(PipelineTimelineEntry pipelineTimelineEntry) {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineTimelineEntry.getPipelineName());
        populatePipelineTimelineEntriesFor(pipelineName);
        naturalOrderLock.readLock().lock();
        try {
            return pipelineToEntries.get(pipelineName).getNaturalOrderSet().higher(pipelineTimelineEntry);
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    PipelineTimelineEntry naturalOrderBefore(PipelineTimelineEntry pipelineTimelineEntry) {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineTimelineEntry.getPipelineName());
        populatePipelineTimelineEntriesFor(pipelineName);
        naturalOrderLock.readLock().lock();
        try {
            return pipelineToEntries.get(pipelineName).getNaturalOrderSet().lower(pipelineTimelineEntry);
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    /**
     * @deprecated No reason why you should use this apart from test tear down
     */
    @Deprecated
    public void clearWhichIsEvilAndShouldNotBeUsedInRealWorld() {
        acquireAllWriteLocks();
        try {
            pipelineToEntries.clear();
        } finally {
            releaseAllWriteLocks();
        }
    }

    public int instanceCount(CaseInsensitiveString pipelineName) {
        populatePipelineTimelineEntriesFor(pipelineName);
        scheduleOrderLock.readLock().lock();
        try {
            ArrayList<PipelineTimelineEntry> instances = pipelineToEntries.get(pipelineName).getScheduledOrderSet();
            return instances == null ? 0 : instances.size();
        } finally {
            scheduleOrderLock.readLock().unlock();
        }
    }

    public PipelineTimelineEntry instanceFor(CaseInsensitiveString pipelineName, int index) {
        populatePipelineTimelineEntriesFor(pipelineName);
        scheduleOrderLock.readLock().lock();
        try {
            ArrayList<PipelineTimelineEntry> instances = pipelineToEntries.get(pipelineName).getScheduledOrderSet();
            return instances == null ? null : instances.get(index);
        } finally {
            scheduleOrderLock.readLock().unlock();
        }
    }


    public PipelineTimelineEntry getEntryFor(CaseInsensitiveString pipelineName, Integer pipelineCounter) {
        populatePipelineTimelineEntriesFor(pipelineName);
        scheduleOrderLock.readLock().lock();
        try {
            ArrayList<PipelineTimelineEntry> instances = pipelineToEntries.get(pipelineName).getScheduledOrderSet();
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
