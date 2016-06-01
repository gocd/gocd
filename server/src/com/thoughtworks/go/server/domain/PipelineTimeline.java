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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.listener.TimelineUpdateListener;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * @understands a sorted collection of PipelineMaterialModification
 */
public class PipelineTimeline {
    private static final Logger LOGGER = Logger.getLogger(PipelineTimeline.class);

    private final Map<CaseInsensitiveString, TreeSet<PipelineTimelineEntry>> naturalOrderPmm;
    private final Map<CaseInsensitiveString, ArrayList<PipelineTimelineEntry>> scheduleOrderPmm;
    private volatile long maximumId;
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
        naturalOrderPmm = new HashMap<>();
        scheduleOrderPmm = new HashMap<>();
        maximumId = -1;
    }

    /**
     * @deprecated Used only in tests
     */
    @Deprecated
    public Collection<PipelineTimelineEntry> getEntriesFor(String pipelineName) {
        naturalOrderLock.readLock().lock();
        try {
            TreeSet<PipelineTimelineEntry> tree = naturalOrderPmm.get(new CaseInsensitiveString(pipelineName));
            if (tree == null) {
                tree = new TreeSet<>();
            }
            return Collections.unmodifiableCollection(cloner.deepClone(tree));
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    public long maximumId() {
        return maximumId;
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
            final long maximumIdBeforeUpdate = maximumId;
            transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus transactionStatus) {
                    final List<PipelineTimelineEntry>[] tempEntries = new List[1];
                    transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override public void afterCompletion(int status) {
                            if (STATUS_ROLLED_BACK == status) {
                                rollbackTempEntries();
                            } else if (STATUS_COMMITTED == status) {
                                notifyListeners(tempEntries[0]);
                            }
                        }

                        private void rollbackTempEntries() {
                            for (PipelineTimelineEntry entry : tempEntries[0]) {
                                rollbackNewEntryFor(entry);
                            }
                            maximumId = maximumIdBeforeUpdate;
                        }

                        private void rollbackNewEntryFor(PipelineTimelineEntry entry) {
                            CaseInsensitiveString pipelineName = new CaseInsensitiveString(entry.getPipelineName());
                            initializedNaturalOrderCollection(pipelineName).remove(entry);
                            initializedScheduleOrderCollection(pipelineName).remove(entry);
                        }


                    });
                    tempEntries[0] = pipelineRepository.updatePipelineTimeline(PipelineTimeline.this);
                    return null;
                }
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
                    listener.added(entry.getValue(), naturalOrderPmm.get(entry.getKey()));
                } catch (Exception e) {
                    LOGGER.warn("Ignoring exception when notifying listener: " + listener, e);
                }
            }
        }
    }

    /**
     * This is called on system init and is called by Spring. Hence, this is not done in a transaction. At any other time, the method update should be used
     */
    public void updateTimelineOnInit() {
        acquireAllWriteLocks();
        try {
            pipelineRepository.updatePipelineTimeline(this);
        } finally {
            releaseAllWriteLocks();
        }
    }

    /**
     * @param id           for the pipeline
     * @param pipelineName
     * @return PMM which was before the pipeline with this id at the time of insertion of the PTE with the id or null if there was nothing before this pipeline during insertion
     */
    public PipelineTimelineEntry runBefore(long id, final CaseInsensitiveString pipelineName) {
        naturalOrderLock.readLock().lock();
        try {
            TreeSet<PipelineTimelineEntry> treeForPipeline = naturalOrderPmm.get(pipelineName);
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
     * @param id           for the pipeline
     * @param pipelineName
     * @return PMM which was after the pipeline with this id at the time of insertion of the PTE with the id or null if there was nothing after this pipeline during insertion
     */
    public PipelineTimelineEntry runAfter(long id, final CaseInsensitiveString pipelineName) {
        naturalOrderLock.readLock().lock();
        try {
            TreeSet<PipelineTimelineEntry> treeForPipeline = naturalOrderPmm.get(pipelineName);
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
        maximumId = Math.max(id, maximumId);
    }

    private TreeSet<PipelineTimelineEntry> initializedNaturalOrderCollection(final CaseInsensitiveString pipelineName) {
        if (!naturalOrderPmm.containsKey(pipelineName)) {
            naturalOrderPmm.put(pipelineName, new TreeSet<PipelineTimelineEntry>());
        }
        return naturalOrderPmm.get(pipelineName);
    }

    private ArrayList<PipelineTimelineEntry> initializedScheduleOrderCollection(final CaseInsensitiveString pipelineName) {
        if (!scheduleOrderPmm.containsKey(pipelineName)) {
            scheduleOrderPmm.put(pipelineName, new ArrayList<PipelineTimelineEntry>());
        }
        return scheduleOrderPmm.get(pipelineName);
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

    public long pipelineBefore(long id) {
        naturalOrderLock.readLock().lock();
        try {
            for (Map.Entry<CaseInsensitiveString, TreeSet<PipelineTimelineEntry>> nameToEntry : naturalOrderPmm.entrySet()) {
                for (PipelineTimelineEntry entry : nameToEntry.getValue()) {
                    if (entry.getId() == id) {
                        PipelineTimelineEntry timelineEntry = naturalOrderBefore(entry);
                        if (timelineEntry == null) {
                            return -1;
                        }
                        return timelineEntry.getId();
                    }
                }
            }
            return -1;
        } finally {
            naturalOrderLock.readLock().unlock();
        }
    }

    public long pipelineAfter(long id) {
        naturalOrderLock.readLock().lock();
        try {
            for (Map.Entry<CaseInsensitiveString, TreeSet<PipelineTimelineEntry>> nameToEntry : naturalOrderPmm.entrySet()) {
                for (PipelineTimelineEntry entry : nameToEntry.getValue()) {
                    if (entry.getId() == id) {
                        PipelineTimelineEntry timelineEntry = naturalOrderAfter(entry);
                        if (timelineEntry == null) {
                            return -1;
                        }
                        return timelineEntry.getId();
                    }
                }
            }
            return -1;
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
            naturalOrderPmm.clear();
            scheduleOrderPmm.clear();
        } finally {
            releaseAllWriteLocks();
        }
    }

    public int instanceCount(CaseInsensitiveString pipelineName) {
        scheduleOrderLock.readLock().lock();
        try {
            ArrayList<PipelineTimelineEntry> instances = scheduleOrderPmm.get(pipelineName);
            return instances == null ? 0 : instances.size();
        } finally {
            scheduleOrderLock.readLock().unlock();
        }
    }

    public PipelineTimelineEntry instanceFor(CaseInsensitiveString pipelineName, int index) {
        scheduleOrderLock.readLock().lock();
        try {
            ArrayList<PipelineTimelineEntry> instances = scheduleOrderPmm.get(pipelineName);
            return instances == null ? null : instances.get(index);
        } finally {
            scheduleOrderLock.readLock().unlock();
        }
    }


    public PipelineTimelineEntry getEntryFor(CaseInsensitiveString pipelineName, Integer pipelineCounter) {
        scheduleOrderLock.readLock().lock();
        try {
            ArrayList<PipelineTimelineEntry> instances = scheduleOrderPmm.get(pipelineName);
            for (int i=instances.size()-1; i >= 0; i--) {
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

    public static class Users {
        private TreeMap<MaterialUserConfig, MaterialUserConfig> byFolder = new TreeMap<>();
        private Map<CaseInsensitiveString, LinkedList<PipelineTimelineEntry>> byFlyweight = new HashMap<>();
    }

    public static class MaterialUserConfig implements Comparable<MaterialUserConfig> {
        private final CaseInsensitiveString pipelineName;
        private final String folder;
        private LinkedList<PipelineTimelineEntry> entries;

        public MaterialUserConfig(CaseInsensitiveString pipelineName, String folder) {
            this.pipelineName = pipelineName;
            this.folder = folder;
        }

        public synchronized void add(PipelineTimelineEntry entry) {
            if (entries == null) {
                entries = new LinkedList<>();
            }
            entries.addFirst(entry);
        }

        public List<PipelineTimelineEntry> instances() {
            return entries;
        }

        @Override public int compareTo(MaterialUserConfig o) {
            int diff = pipelineName.compareTo(o.pipelineName);
            if (diff != 0) {
                return diff;
            }
            if (folder == null && o.folder == null) {
                return 0;
            } else if (folder != null & o.folder == null) {
                return 1;
            } else if (o.folder != null && folder == null) {
                return -1;
            } else {
                return folder.compareTo(o.folder);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MaterialUserConfig that = (MaterialUserConfig) o;

            if (folder != null ? !folder.equals(that.folder) : that.folder != null) {
                return false;
            }
            if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = pipelineName != null ? pipelineName.hashCode() : 0;
            result = 31 * result + (folder != null ? folder.hashCode() : 0);
            return result;
        }

        @Override public String toString() {
            return "MaterialUserConfig{" +
                    "pipelineName=" + pipelineName +
                    ", folder='" + folder + '\'' +
                    '}';
        }
    }
}
