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
package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.database.QueryExtensions;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;

/**
 * @understands how to store and retrieve piplines from the database
 */
@Component
public class PipelineRepository extends HibernateDaoSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineRepository.class);
    private final QueryExtensions queryExtensions;
    private GoCache goCache;

    @Autowired
    public PipelineRepository(SessionFactory sessionFactory, GoCache goCache, Database databaseStrategy) {
        this.goCache = goCache;
        this.queryExtensions = databaseStrategy.getQueryExtensions();
        setSessionFactory(sessionFactory);
    }

    public static int updateNaturalOrderForPipeline(Session session, Long pipelineId, double naturalOrder) {
        String sql = "UPDATE pipelines SET naturalOrder = :naturalOrder WHERE id = :pipelineId";
        SQLQuery query = session.createSQLQuery(sql);
        query.setLong("pipelineId", pipelineId);
        query.setDouble("naturalOrder", naturalOrder);
        return query.executeUpdate();
    }

    public void updatePipelineTimeline(final PipelineTimeline pipelineTimeline, final List<PipelineTimelineEntry> tempEntriesForRollback, final String pipelineName) {
        getHibernateTemplate().execute(new HibernateCallback() {
            private static final int PIPELINE_NAME = 0;
            private static final int ID = 1;
            private static final int COUNTER = 2;
            private static final int MODIFIED_TIME = 3;
            private static final int FINGERPRINT = 4;
            private static final int NATURAL_ORDER = 5;
            private static final int REVISION = 6;
            private static final int FOLDER = 7;
            private static final int MOD_ID = 8;
            private static final int PMR_ID = 9;

            @Override
            public Object doInHibernate(Session session) throws HibernateException {
                LOGGER.info("Start updating pipeline timeline");
                List<Object[]> matches = retrieveTimeline(session, pipelineTimeline, pipelineName);
                List<PipelineTimelineEntry> newPipelines = populateFrom(matches);
                addEntriesToPipelineTimeline(newPipelines, pipelineTimeline, tempEntriesForRollback);

                updateNaturalOrdering(session, newPipelines);
                LOGGER.info("Pipeline timeline updated");
                return null;
            }

            private void updateNaturalOrdering(Session session, List<PipelineTimelineEntry> pipelines) {
                for (PipelineTimelineEntry pipeline : pipelines) {
                    if (pipeline.hasBeenUpdated()) {
                        updateNaturalOrderForPipeline(session, pipeline.getId(), pipeline.naturalOrder());
                    }
                }
            }

            private List<Object[]> loadTimeline(SQLQuery query, String pipelineName) {
                long startedAt = System.currentTimeMillis();
                List<Object[]> matches = (List<Object[]>) query.list();
                long duration = System.currentTimeMillis() - startedAt;
                LOGGER.info("updating in memory pipeline-timeline for {} took: {} ms", pipelineName, duration);
                return matches;
            }

            private List<Object[]> retrieveTimeline(Session session, PipelineTimeline pipelineTimeline, String pipelineName) {
                SQLQuery query = session.createSQLQuery(queryExtensions.retrievePipelineTimelineFor(pipelineName));
                query.setLong("pipelineId", pipelineTimeline.getMaximumIdFor(pipelineName));

                List<Object[]> matches = loadTimeline(query, pipelineName);
                sortTimeLineByPidAndPmrId(matches);
                return matches;
            }

            private void sortTimeLineByPidAndPmrId(List<Object[]> matches) {
                matches.sort((m1, m2) -> {
                    long id1 = id(m1);
                    long id2 = id(m2);
                    if (id1 == id2) {
                        return (int) (pmrId(m1) - pmrId(m2));
                    }
                    return (int) (id1 - id2);
                });
            }


            private List<PipelineTimelineEntry> populateFrom(List<Object[]> matches) {
                ArrayList<PipelineTimelineEntry> newPipelines = new ArrayList<>();
                if (matches.isEmpty()) {
                    return newPipelines;
                }

                Map<String, List<PipelineTimelineEntry.Revision>> revisions = new HashMap<>();

                String name = null;
                long curId = -1;
                Integer counter = null;
                double naturalOrder = 0.0;

                PipelineTimelineEntry entry = null;

                for (int i = 0; i < matches.size(); i++) {
                    Object[] row = matches.get(i);
                    long id = id(row);
                    if (curId != id) {
                        name = pipelineName(row);
                        curId = id;
                        counter = counter(row);
                        revisions = new HashMap<>();
                        naturalOrder = naturalOrder(row);
                    }

                    String fingerprint = fingerprint(row);

                    if (!revisions.containsKey(fingerprint)) {
                        revisions.put(fingerprint, new ArrayList<>());
                    }
                    revisions.get(fingerprint).add(rev(row));

                    int nextI = i + 1;
                    if (((nextI < matches.size() && id(matches.get(nextI)) != curId) ||//new pipeline instance starts in next record, so capture this one
                            nextI == matches.size())) {//this is the last record, so capture it
                        entry = new PipelineTimelineEntry(name, curId, counter, revisions, naturalOrder);
                        newPipelines.add(entry);
                    }
                }
                return newPipelines;
            }

            private String folder(Object[] row) {
                return (String) row[FOLDER];
            }

            private PipelineTimelineEntry.Revision rev(Object[] row) {
                return new PipelineTimelineEntry.Revision(modifiedTime(row), stringRevision(row), folder(row), modId(row));
            }

            private long pmrId(Object[] row) {
                return ((BigInteger) row[PMR_ID]).longValue();
            }

            private long modId(Object[] row) {
                return ((BigInteger) row[MOD_ID]).longValue();
            }

            private double naturalOrder(Object[] row) {
                return (Double) row[NATURAL_ORDER];
            }

            private Date modifiedTime(Object[] row) {
                return (Date) row[MODIFIED_TIME];
            }

            private String stringRevision(Object[] row) {
                return (String) row[REVISION];
            }

            private String fingerprint(Object[] row) {
                return String.valueOf(row[FINGERPRINT]);
            }

            private String pipelineName(Object[] row) {
                return (String) row[PIPELINE_NAME];
            }

            private int counter(Object[] row) {
                return row[COUNTER] == null ? -1 : ((BigInteger) row[COUNTER]).intValue();
            }

            private long id(Object[] first) {
                return ((BigInteger) first[ID]).longValue();
            }
        });
    }

    private void addEntriesToPipelineTimeline(List<PipelineTimelineEntry> newEntries, PipelineTimeline pipelineTimeline, List<PipelineTimelineEntry> tempEntriesForRollback) {
        for (PipelineTimelineEntry newEntry : newEntries) {
            tempEntriesForRollback.add(newEntry);
            pipelineTimeline.add(newEntry);
        }
    }

    public long saveSelectedPipelines(PipelineSelections pipelineSelections) {
        removePipelineSelectionFromCacheForUserId(pipelineSelections);
        removePipelineSelectionFromCacheForCookie(pipelineSelections);
        getHibernateTemplate().saveOrUpdate(pipelineSelections);
        return pipelineSelections.getId();
    }

    public PipelineSelections findPipelineSelectionsById(long id) {
        PipelineSelections pipelineSelections;
        String key = pipelineSelectionForCookieKey(id);

        if (goCache.isKeyInCache(key)) {
            return (PipelineSelections) goCache.get(key);
        }

        synchronized (key) {
            if (goCache.isKeyInCache(key)) {
                return (PipelineSelections) goCache.get(key);
            }

            pipelineSelections = getHibernateTemplate().get(PipelineSelections.class, id);

            if (null != pipelineSelections) {
                goCache.put(key, pipelineSelections);
            }

            return pipelineSelections;
        }
    }

    public PipelineSelections findPipelineSelectionsById(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return findPipelineSelectionsById(Long.parseLong(id));
    }

    public PipelineSelections findPipelineSelectionsByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        PipelineSelections pipelineSelections;
        String key = pipelineSelectionForUserIdKey(userId);
        if (goCache.isKeyInCache(key)) {
            return (PipelineSelections) goCache.get(key);
        }
        synchronized (key) {
            if (goCache.isKeyInCache(key)) {
                return (PipelineSelections) goCache.get(key);
            }
            List list = getHibernateTemplate().find("FROM PipelineSelections WHERE userId = ?", new Object[]{userId});
            if (list.isEmpty()) {
                pipelineSelections = null;
            } else {
                pipelineSelections = (PipelineSelections) list.get(0);
            }

            goCache.put(key, pipelineSelections);
            return pipelineSelections;
        }
    }

    private void removePipelineSelectionFromCacheForCookie(PipelineSelections pipelineSelections) {
        String pipelineSelectionCookieKey = pipelineSelectionForCookieKey(pipelineSelections.getId());
        synchronized (pipelineSelectionCookieKey) {
            goCache.remove(pipelineSelectionCookieKey);
        }
    }

    private void removePipelineSelectionFromCacheForUserId(PipelineSelections pipelineSelections) {
        String pipelineSelectionUserIdKey = pipelineSelectionForUserIdKey(pipelineSelections.userId());
        synchronized (pipelineSelectionUserIdKey) {
            goCache.remove(pipelineSelectionUserIdKey);
        }
    }

    String pipelineSelectionForUserIdKey(Long userId) {
        return (PipelineRepository.class.getName() + "_userIdPipelineSelection_" + userId).intern();
    }

    String pipelineSelectionForCookieKey(long id) {
        return (PipelineRepository.class.getName() + "_cookiePipelineSelection_" + id).intern();
    }
}
