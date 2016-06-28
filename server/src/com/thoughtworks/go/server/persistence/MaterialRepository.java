/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.database.QueryExtensions;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialInstance;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.ui.ModificationForPipeline;
import com.thoughtworks.go.server.ui.PipelineId;
import com.thoughtworks.go.server.util.CollectionUtil;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.io.File;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.isNull;

/**
 * @understands how to store and retrieve Materials from the database
 */
public class MaterialRepository extends HibernateDaoSupport {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MaterialRepository.class.getName());

    private final GoCache goCache;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    private final MaterialConfigConverter materialConfigConverter;
    private final QueryExtensions queryExtensions;
    private int latestModificationsCacheLimit;
    private MaterialExpansionService materialExpansionService;

    public MaterialRepository(SessionFactory sessionFactory, GoCache goCache, int latestModificationsCacheLimit, TransactionSynchronizationManager transactionSynchronizationManager,
                              MaterialConfigConverter materialConfigConverter, MaterialExpansionService materialExpansionService, Database databaseStrategy) {
        this.goCache = goCache;
        this.latestModificationsCacheLimit = latestModificationsCacheLimit;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.materialConfigConverter = materialConfigConverter;
        this.materialExpansionService = materialExpansionService;
        this.queryExtensions = databaseStrategy.getQueryExtensions();
        setSessionFactory(sessionFactory);
    }

    @SuppressWarnings({"unchecked"})
    public List<Modification> getModificationsForPipelineRange(final String pipelineName, final Integer fromCounter, final Integer toCounter) {
        return (List<Modification>) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                final List<Long> fromInclusiveModificationList = fromInclusiveModificationsForPipelineRange(session, pipelineName, fromCounter, toCounter);

                final Set<Long> fromModifications = new TreeSet<>(fromInclusiveModificationsForPipelineRange(session, pipelineName, fromCounter, fromCounter));

                final Set<Long> fromExclusiveModificationList = new HashSet<>();

                for (Long modification : fromInclusiveModificationList) {
                    if (fromModifications.contains(modification)) {
                        fromModifications.remove(modification);
                    } else {
                        fromExclusiveModificationList.add(modification);
                    }
                }

                SQLQuery query = session.createSQLQuery("SELECT * FROM modifications WHERE id IN (:ids) ORDER BY materialId ASC, id DESC");
                query.addEntity(Modification.class);
                query.setParameterList("ids", fromExclusiveModificationList.isEmpty() ? fromInclusiveModificationList : fromExclusiveModificationList);
                return query.list();
            }
        });
    }

    private List<Long> fromInclusiveModificationsForPipelineRange(Session session, String pipelineName, Integer fromCounter, Integer toCounter) {
        String pipelineIdsSql = queryExtensions.queryFromInclusiveModificationsForPipelineRange(pipelineName, fromCounter, toCounter);
        SQLQuery pipelineIdsQuery = session.createSQLQuery(pipelineIdsSql);
        final List ids = pipelineIdsQuery.list();
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        String minMaxQuery = " SELECT mods1.materialId as materialId, min(mods1.id) as min, max(mods1.id) as max"
                + " FROM modifications mods1 "
                + "     INNER JOIN pipelineMaterialRevisions pmr ON (mods1.id >= pmr.actualFromRevisionId AND mods1.id <= pmr.toRevisionId) AND mods1.materialId = pmr.materialId "
                + " WHERE pmr.pipelineId IN (:ids) "
                + " GROUP BY mods1.materialId";

        SQLQuery query = session.createSQLQuery("SELECT mods.id "
                + " FROM modifications mods"
                + "     INNER JOIN (" + minMaxQuery + ") as edges on edges.materialId = mods.materialId and mods.id >= min and mods.id <= max"
                + " ORDER BY mods.materialId ASC, mods.id DESC");
        query.addScalar("id", new LongType());
        query.setParameterList("ids", ids);

        return query.list();
    }

    public Map<Long, List<ModificationForPipeline>> findModificationsForPipelineIds(final List<Long> pipelineIds) {
        final int MODIFICATION = 0;
        final int RELEVANT_PIPELINE_ID = 1;
        final int RELEVANT_PIPELINE_NAME = 2;
        final int MATERIAL_TYPE = 3;
        final int MATERIAL_FINGERPRINT = 4;
        //noinspection unchecked
        return (Map<Long, List<ModificationForPipeline>>) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                if (pipelineIds.isEmpty()) {
                    return new HashMap<Long, List<ModificationForPipeline>>();
                }
                Map<PipelineId, Set<Long>> relevantToLookedUpMap = relevantToLookedUpDependencyMap(session, pipelineIds);

                SQLQuery query = session.createSQLQuery("SELECT mods.*, pmr.pipelineId as pmrPipelineId, p.name as pmrPipelineName, m.type as materialType, m.fingerprint as fingerprint"
                        + " FROM modifications mods "
                        + "     INNER JOIN pipelineMaterialRevisions pmr ON (mods.id >= pmr.fromRevisionId AND mods.id <= pmr.toRevisionId) AND mods.materialId = pmr.materialId "
                        + "     INNER JOIN pipelines p ON pmr.pipelineId = p.id"
                        + "     INNER JOIN materials m ON mods.materialId = m.id"
                        + " WHERE pmr.pipelineId IN (:ids)");

                @SuppressWarnings({"unchecked"})
                List<Object[]> allModifications = query.
                        addEntity("mods", Modification.class).
                        addScalar("pmrPipelineId", new LongType()).
                        addScalar("pmrPipelineName", new StringType()).
                        addScalar("materialType", new StringType()).
                        addScalar("fingerprint", new StringType()).
                        setParameterList("ids", CollectionUtil.map(relevantToLookedUpMap.keySet(), PipelineId.MAP_ID)).
                        list();

                Map<Long, List<ModificationForPipeline>> modificationsForPipeline = new HashMap<>();
                CollectionUtil.CollectionValueMap<Long, ModificationForPipeline> modsForPipeline = CollectionUtil.collectionValMap(modificationsForPipeline,
                        new CollectionUtil.ArrayList<ModificationForPipeline>());
                for (Object[] modAndPmr : allModifications) {
                    Modification mod = (Modification) modAndPmr[MODIFICATION];
                    Long relevantPipelineId = (Long) modAndPmr[RELEVANT_PIPELINE_ID];
                    String relevantPipelineName = (String) modAndPmr[RELEVANT_PIPELINE_NAME];
                    String materialType = (String) modAndPmr[MATERIAL_TYPE];
                    String materialFingerprint = (String) modAndPmr[MATERIAL_FINGERPRINT];
                    PipelineId relevantPipeline = new PipelineId(relevantPipelineName, relevantPipelineId);
                    Set<Long> longs = relevantToLookedUpMap.get(relevantPipeline);
                    for (Long lookedUpPipeline : longs) {
                        modsForPipeline.put(lookedUpPipeline, new ModificationForPipeline(relevantPipeline, mod, materialType, materialFingerprint));
                    }
                }
                return modificationsForPipeline;
            }
        });
    }

    private Map<PipelineId, Set<Long>> relevantToLookedUpDependencyMap(Session session, List<Long> pipelineIds) {
        final int LOOKED_UP_PIPELINE_ID = 2;
        final int RELEVANT_PIPELINE_ID = 0;
        final int RELEVANT_PIPELINE_NAME = 1;

        String pipelineIdsSql = queryExtensions.queryRelevantToLookedUpDependencyMap(pipelineIds);
        SQLQuery pipelineIdsQuery = session.createSQLQuery(pipelineIdsSql);
        pipelineIdsQuery.addScalar("id", new LongType());
        pipelineIdsQuery.addScalar("name", new StringType());
        pipelineIdsQuery.addScalar("lookedUpId", new LongType());
        final List<Object[]> ids = pipelineIdsQuery.list();

        Map<Long, List<PipelineId>> lookedUpToParentMap = new HashMap<>();
        CollectionUtil.CollectionValueMap<Long, PipelineId> lookedUpToRelevantMap = CollectionUtil.collectionValMap(lookedUpToParentMap, new CollectionUtil.ArrayList<PipelineId>());
        for (Object[] relevantAndLookedUpId : ids) {
            lookedUpToRelevantMap.put((Long) relevantAndLookedUpId[LOOKED_UP_PIPELINE_ID],
                    new PipelineId((String) relevantAndLookedUpId[RELEVANT_PIPELINE_NAME], (Long) relevantAndLookedUpId[RELEVANT_PIPELINE_ID]));
        }
        return CollectionUtil.reverse(lookedUpToParentMap);
    }

    @SuppressWarnings("unchecked")
    public MaterialRevisions findMaterialRevisionsForPipeline(long pipelineId) {
        List<PipelineMaterialRevision> revisions = findPipelineMaterialRevisions(pipelineId);
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (PipelineMaterialRevision revision : revisions) {
            List<Modification> modifications = findModificationsFor(revision);
            materialRevisions.addRevision(new MaterialRevision(revision.getMaterial(), revision.getChanged(), modifications.toArray(new Modification[modifications.size()])));
        }
        return materialRevisions;
    }

    public void cacheMaterialRevisionsForPipelines(Set<Long> pipelineIds) {
        List<Long> ids = new ArrayList<>(pipelineIds);

        final int batchSize = 500;
        loadPMRsIntoCache(ids, batchSize);
    }

    private void loadPMRsIntoCache(List<Long> ids, int batchSize) {
        int total = ids.size(), remaining = total;
        while (!ids.isEmpty()) {
            LOGGER.info(String.format("Loading PMRs,Remaining %s Pipelines (Total: %s)...", remaining, total));
            final List<Long> idsBatch = batchIds(ids, batchSize);
            loadPMRByPipelineIds(idsBatch);
            remaining -= batchSize;
        }
    }

    private <T> List<T> batchIds(List<T> items, int batchSize) {
        List<T> ids = new ArrayList<>();
        for (int i = 0; i < batchSize; ++i) {
            if (items.isEmpty()) {
                break;
            }
            ids.add(items.remove(0));
        }
        return ids;
    }

    public List findPipelineMaterialRevisions(long pipelineId) {
        String cacheKey = pipelinePmrsKey(pipelineId);
        synchronized (cacheKey) {
            List results = (List) goCache.get(cacheKey);
            if (results != null) {
                return results;
            }
            results = findPMRByPipelineId(pipelineId);
            goCache.put(cacheKey, results);
            return results;
        }
    }

    private List findPMRByPipelineId(long pipelineId) {
        return getHibernateTemplate().find("FROM PipelineMaterialRevision WHERE pipelineId = ? ORDER BY id", pipelineId);
    }

    private void loadPMRByPipelineIds(List<Long> pipelineIds) {
        List<PipelineMaterialRevision> pmrs = getHibernateTemplate().findByCriteria(buildPMRDetachedQuery(pipelineIds));
        sortPersistentObjectsById(pmrs, true);
        final Set<PipelineMaterialRevision> uniquePmrs = new HashSet<>();
        for (PipelineMaterialRevision pmr : pmrs) {
            String cacheKey = pipelinePmrsKey(pmr.getPipelineId());
            List<PipelineMaterialRevision> pmrsForId = (List<PipelineMaterialRevision>) goCache.get(cacheKey);
            if (pmrsForId == null) {
                pmrsForId = new ArrayList<>();
                goCache.put(cacheKey, pmrsForId);
            }
            pmrsForId.add(pmr);
            putMaterialInstanceIntoCache(pmr.getToModification().getMaterialInstance());
            uniquePmrs.add(pmr);
        }
        loadModificationsIntoCache(uniquePmrs);
    }

    private void sortPersistentObjectsById(List<? extends PersistentObject> persistentObjects, boolean asc) {
        Comparator<PersistentObject> ascendingSort = new Comparator<PersistentObject>() {
            @Override
            public int compare(PersistentObject po1, PersistentObject po2) {
                return (int) (po1.getId() - po2.getId());
            }
        };
        Comparator<PersistentObject> descendingSort = new Comparator<PersistentObject>() {
            @Override
            public int compare(PersistentObject po1, PersistentObject po2) {
                return (int) (po2.getId() - po1.getId());
            }
        };
        Collections.sort(persistentObjects, asc ? ascendingSort : descendingSort);
    }

    public void putMaterialInstanceIntoCache(MaterialInstance materialInstance) {
        String cacheKey = materialKey(materialInstance.getFingerprint());
        goCache.put(cacheKey, materialInstance);
    }

    private DetachedCriteria buildPMRDetachedQuery(List<Long> pipelineIds) {
        DetachedCriteria criteria = DetachedCriteria.forClass(PipelineMaterialRevision.class);
        criteria.add(Restrictions.in("pipelineId", pipelineIds));
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return criteria;
    }

    private void loadModificationsIntoCache(Set<PipelineMaterialRevision> pmrs) {
        List<PipelineMaterialRevision> pmrList = new ArrayList<>(pmrs);
        int batchSize = 100, total = pmrList.size(), remaining = total;
        while (!pmrList.isEmpty()) {
            LOGGER.info(String.format("Loading modifications, Remaining %s PMRs(Total: %s)...", remaining, total));
            final List<PipelineMaterialRevision> pmrBatch = batchIds(pmrList, batchSize);
            loadModificationsForPMR(pmrBatch);
            remaining -= batchSize;
        }
    }

    private void loadModificationsForPMR(List<PipelineMaterialRevision> pmrs) {
        List<Criterion> criterions = new ArrayList<>();
        for (PipelineMaterialRevision pmr : pmrs) {
            if (goCache.get(pmrModificationsKey(pmr)) != null) {
                continue;
            }
            final Criterion modificationClause = Restrictions.between("id", pmr.getFromModification().getId(), pmr.getToModification().getId());
            final SimpleExpression idClause = Restrictions.eq("materialInstance", pmr.getMaterialInstance());
            criterions.add(Restrictions.and(idClause, modificationClause));
        }
        List<Modification> modifications = getHibernateTemplate().findByCriteria(buildModificationDetachedQuery(criterions));
        sortPersistentObjectsById(modifications, false);
        for (Modification modification : modifications) {
            List<String> cacheKeys = pmrModificationsKey(modification, pmrs);
            for (String cacheKey : cacheKeys) {
                List<Modification> modificationList = (List<Modification>) goCache.get(cacheKey);
                if (modificationList == null) {
                    modificationList = new ArrayList<>();
                    goCache.put(cacheKey, modificationList);
                }
                modificationList.add(modification);
            }
        }
    }

    private DetachedCriteria buildModificationDetachedQuery(List<Criterion> criteria) {
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Modification.class);
        Disjunction disjunction = Restrictions.disjunction();
        detachedCriteria.add(disjunction);
        for (Criterion criterion : criteria) {
            disjunction.add(criterion);
        }
        detachedCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return detachedCriteria;
    }

    private String pipelinePmrsKey(long pipelineId) {
        return (MaterialRepository.class.getName() + "_pipelinePMRs_" + pipelineId).intern();
    }

    @SuppressWarnings("unchecked")
    List<Modification> findMaterialRevisionsForMaterial(long id) {
        return getHibernateTemplate().find("FROM Modification WHERE materialId = ?", new Object[]{id});
    }

    @SuppressWarnings("unchecked")
    List<Modification> findModificationsFor(PipelineMaterialRevision pmr) {
        String cacheKey = pmrModificationsKey(pmr);
        List<Modification> modifications = (List<Modification>) goCache.get(cacheKey);
        if (modifications == null) {
            synchronized (cacheKey) {
                modifications = (List<Modification>) goCache.get(cacheKey);
                if (modifications == null) {
                    modifications = getHibernateTemplate().find(
                            "FROM Modification WHERE materialId = ? AND id BETWEEN ? AND ? ORDER BY id DESC",
                            new Object[]{findMaterialInstance(pmr.getMaterial()).getId(), pmr.getFromModification().getId(), pmr.getToModification().getId()});
                    goCache.put(cacheKey, modifications);
                }
            }
        }
        return modifications;
    }

    private String pmrModificationsKey(PipelineMaterialRevision pmr) {
        // we intern() it because we might synchronize on the returned String
        return (MaterialRepository.class.getName() + "_pmrModifications_" + pmr.getId()).intern();
    }

    private List<String> pmrModificationsKey(Modification modification, List<PipelineMaterialRevision> pmrs) {
        final long id = modification.getId();
        final MaterialInstance materialInstance = modification.getMaterialInstance();
        Collection<PipelineMaterialRevision> matchedPmrs = (Collection<PipelineMaterialRevision>) CollectionUtils.select(pmrs, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                PipelineMaterialRevision pmr = (PipelineMaterialRevision) o;
                long from = pmr.getFromModification().getId();
                long to = pmr.getToModification().getId();
                MaterialInstance pmi = findMaterialInstance(pmr.getMaterial());
                return from <= id && id <= to && materialInstance.equals(pmi);
            }
        });
        List<String> keys = new ArrayList<>(matchedPmrs.size());
        for (PipelineMaterialRevision matchedPmr : matchedPmrs) {
            keys.add(pmrModificationsKey(matchedPmr));
        }
        return keys;
    }

    String latestMaterialModificationsKey(MaterialInstance materialInstance) {
        // we intern() it because we might synchronize on the returned String
        return (MaterialRepository.class.getName() + "_latestMaterialModifications_" + materialInstance.getId()).intern();
    }

    String materialModificationCountKey(MaterialInstance materialInstance) {
        // we intern() it because we might synchronize on the returned String
        return (MaterialRepository.class.getName() + "_materialModificationCount_" + materialInstance.getId()).intern();
    }

    String materialModificationsWithPaginationKey(MaterialInstance materialInstance) {
        // we intern() it because we might synchronize on the returned String
        return (MaterialRepository.class.getName() + "_materialModificationsWithPagination_" + materialInstance.getId()).intern();
    }

    String materialModificationsWithPaginationSubKey(Pagination pagination) {
        return String.format("%s-%s", pagination.getOffset(), pagination.getPageSize());
    }

    public void saveOrUpdate(MaterialInstance materialInstance) {
        String cacheKey = materialKey(materialInstance.getFingerprint());
        synchronized (cacheKey) {
            getHibernateTemplate().saveOrUpdate(materialInstance);
            goCache.remove(cacheKey);
            goCache.put(cacheKey, materialInstance);
        }
    }

    public MaterialInstance find(long id) {
        return getHibernateTemplate().load(MaterialInstance.class, id);
    }

    public MaterialInstance saveMaterialRevision(MaterialRevision materialRevision) {
        LOGGER.info("Saving revision " + materialRevision);
        MaterialInstance materialInstance = findOrCreateFrom(materialRevision.getMaterial());
        saveModifications(materialInstance, materialRevision.getModifications());
        return materialInstance;
    }

    // Used in tests
    public void saveModification(MaterialInstance materialInstance, Modification modification) {
        modification.setMaterialInstance(materialInstance);
        try {
            getHibernateTemplate().saveOrUpdate(modification);
            removeLatestCachedModification(materialInstance, modification);
            removeCachedModificationCountFor(materialInstance);
            removeCachedModificationsFor(materialInstance);
        } catch (Exception e) {
            String message = "Cannot save modification " + modification;
            LOGGER.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public MaterialInstance findOrCreateFrom(Material material) {
        String cacheKey = materialKey(material);
        synchronized (cacheKey) {
            MaterialInstance materialInstance = findMaterialInstance(material);
            if (materialInstance == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Material instance for material '%s' not found in the database, creating a new instance now.", material));
                }
                materialInstance = material.createMaterialInstance();
                saveOrUpdate(materialInstance);
            }
            return materialInstance;
        }
    }

    final String materialKey(Material material) {
        // we intern() it because we synchronize on the returned String
        return materialKey(material.getFingerprint());
    }

    private String materialKey(String fingerprint) {
        return (MaterialRepository.class.getName() + "_materialInstance_" + fingerprint).intern();
    }

    public MaterialInstance findMaterialInstance(Material material) {
        String cacheKey = materialKey(material);
        MaterialInstance materialInstance = (MaterialInstance) goCache.get(cacheKey);
        if (materialInstance == null) {
            synchronized (cacheKey) {
                materialInstance = (MaterialInstance) goCache.get(cacheKey);
                if (materialInstance == null) {
                    DetachedCriteria hibernateCriteria = DetachedCriteria.forClass(material.getInstanceType());
                    for (Map.Entry<String, Object> property : material.getSqlCriteria().entrySet()) {
                        if (!property.getKey().equals(AbstractMaterial.SQL_CRITERIA_TYPE)) {//type is polymorphic mapping discriminator
                            if (property.getValue() == null) {
                                hibernateCriteria.add(isNull(property.getKey()));
                            } else {
                                hibernateCriteria.add(eq(property.getKey(), property.getValue()));
                            }
                        }
                    }
                    materialInstance = (MaterialInstance) firstResult(hibernateCriteria);

                    if (materialInstance != null) {
                        goCache.put(cacheKey, materialInstance);
                    }
                }
            }
        }
        return materialInstance;//TODO: clone me, caller may mutate
    }

    private String buildMaterialInstanceQuery(List<Long> materialIds) {
        StringBuilder queryBuilder = new StringBuilder("FROM MaterialInstance WHERE id IN (");
        for (Long materialId : materialIds) {
            queryBuilder.append(materialId + ",");
        }
        queryBuilder.append(")");
        return queryBuilder.toString().replace(",)", ")"); //hack to remove the last comma
    }


    public MaterialInstance findMaterialInstance(MaterialConfig materialConfig) {
        String cacheKey = materialKey(materialConfig.getFingerprint());
        MaterialInstance materialInstance = (MaterialInstance) goCache.get(cacheKey);
        if (materialInstance == null) {
            synchronized (cacheKey) {
                materialInstance = (MaterialInstance) goCache.get(cacheKey);
                if (materialInstance == null) {
                    DetachedCriteria hibernateCriteria = DetachedCriteria.forClass(materialConfigConverter.getInstanceType(materialConfig));
                    for (Map.Entry<String, Object> property : materialConfig.getSqlCriteria().entrySet()) {
                        if (!property.getKey().equals(AbstractMaterial.SQL_CRITERIA_TYPE)) { //type is polymorphic mapping discriminator
                            if (property.getValue() == null) {
                                hibernateCriteria.add(isNull(property.getKey()));
                            } else {
                                hibernateCriteria.add(eq(property.getKey(), property.getValue()));
                            }
                        }
                    }
                    materialInstance = (MaterialInstance) firstResult(hibernateCriteria);

                    if (materialInstance != null) {
                        goCache.put(cacheKey, materialInstance);
                    }
                }
            }
        }
        return materialInstance;//TODO: clone me, caller may mutate
    }

    private Object firstResult(DetachedCriteria criteria) {
        List results = getHibernateTemplate().findByCriteria(criteria);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    private Object uniqueResult(DetachedCriteria criteria) {
        List results = getHibernateTemplate().findByCriteria(criteria);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw bomb("expected unique results, got " + results.size() + ": " + results);
        }
        return results.get(0);
    }

    public void savePipelineMaterialRevision(Pipeline pipeline, long pipelineId, MaterialRevision materialRevision) {
        Modification from = materialRevision.getOldestModification();
        Modification to = materialRevision.getLatestModification();
        Long actualFromModificationId = getLastBuiltModificationId(pipeline, to.getMaterialInstance(), from);
        if (!from.hasId() || !to.hasId()) {
            throw bomb("You cannot save a PipelineMaterialRevision unless the modifications have already been saved.");
        }
        PipelineMaterialRevision revision = new PipelineMaterialRevision(pipelineId, materialRevision, actualFromModificationId);
        save(revision, pipeline.getName());
    }

    private Long getLastBuiltModificationId(final Pipeline pipeline, final MaterialInstance materialInstance, Modification from) {
        if (materialInstance instanceof DependencyMaterialInstance) {
            Long id = findLastBuiltModificationId(pipeline, materialInstance);
            if (id == null) {
                return from.getId();
            } else {
                return modificationAfter(id, materialInstance);
            }
        }
        return from.getId();
    }

    private long modificationAfter(final long id, final MaterialInstance materialInstance) {
        BigInteger result = (BigInteger) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                String sql = "SELECT id "
                        + " FROM modifications "
                        + " WHERE materialId = ? "
                        + "        AND id > ?"
                        + " ORDER BY id"
                        + " LIMIT 1";
                SQLQuery query = session.createSQLQuery(sql);
                query.setLong(0, materialInstance.getId());
                query.setLong(1, id);
                return query.uniqueResult();
            }
        });
        return result == null ? id : result.longValue();
    }

    private Long findLastBuiltModificationId(final Pipeline pipeline, final MaterialInstance materialInstance) {
        BigInteger result = (BigInteger) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                String sql = "SELECT fromRevisionId "
                        + " FROM pipelineMaterialRevisions pmr "
                        + "     INNER JOIN pipelines p on p.id = pmr.pipelineId "
                        + " WHERE materialId = ? "
                        + "     AND p.name = ? "
                        + "     AND pipelineId < ? "
                        + " ORDER BY pmr.id DESC"
                        + " LIMIT 1";
                SQLQuery query = session.createSQLQuery(sql);
                query.setLong(0, materialInstance.getId());
                query.setString(1, pipeline.getName());
                query.setLong(2, pipeline.getId());
                return query.uniqueResult();
            }
        });
        return result == null ? null : result.longValue();
    }

    private boolean hasSameMaterialName(Material material, PipelineMaterialRevision pmr) {
        if (material.getName() == null && pmr.getMaterialName() == null) {
            return true;
        }
        if (material.getName() == null && pmr.getMaterialName() != null) {
            return false;
        }
        return material.getName().equals(new CaseInsensitiveString(pmr.getMaterialName()));
    }

    private boolean hasSameFolder(Material material, PipelineMaterialRevision pmr) {
        if (material.getFolder() == null && pmr.getFolder() == null) {
            return true;
        }
        if (material.getFolder() == null && pmr.getFolder() != null) {
            return false;
        }
        return material.getFolder().equals(pmr.getFolder());
    }

    private void save(final PipelineMaterialRevision pipelineMaterialRevision, final String pipelineName) {
        getHibernateTemplate().save(pipelineMaterialRevision);
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                String key = cacheKeyForLatestPmrForPipelineKey(pipelineMaterialRevision.getMaterialId(), pipelineName.toLowerCase());
                synchronized (key) {
                    goCache.remove(key);
                }
            }
        });
    }

    public void createPipelineMaterialRevisions(Pipeline pipeline, Long pipelineId, MaterialRevisions materialRevisions) {
        for (MaterialRevision materialRevision : materialRevisions) {
            savePipelineMaterialRevision(pipeline, pipelineId, materialRevision);
        }
    }

    /**
     * @deprecated Not used in production
     */
    public void save(MaterialRevisions materialRevisions) {
        for (MaterialRevision materialRevision : materialRevisions) {
            saveMaterialRevision(materialRevision);
        }
    }

    public List<Modification> findModificationsSinceAndUptil(Material material, MaterialRevision materialRevision, PipelineTimelineEntry.Revision scmRevision) {
        List<Modification> modificationsSince = findModificationsSince(material, materialRevision);
        if (scmRevision == null) {
            return modificationsSince;
        }
        ArrayList<Modification> modificationsUptil = new ArrayList<>();
        for (Modification modification : modificationsSince) {
            if (modification.getId() <= scmRevision.id) {
                modificationsUptil.add(modification);
            }
        }
        return modificationsUptil;
    }

    @SuppressWarnings("unchecked")
    public List<Modification> findModificationsSince(Material material, MaterialRevision revision) {
        MaterialInstance materialInstance = findOrCreateFrom(material);
        String cacheKey = latestMaterialModificationsKey(materialInstance);
        synchronized (cacheKey) {
            long sinceModificationId = revision.getLatestModification().getId();
            Modifications modifications = cachedModifications(materialInstance);
            if (!modificationExists(sinceModificationId, modifications)) {
                LOGGER.debug("CACHE-MISS for findModificationsSince - " + materialInstance + ": " + revision.getLatestModification());
                modifications = _findModificationsSince(materialInstance, sinceModificationId);
                if (shouldCache(modifications)) {
                    goCache.put(cacheKey, modifications);
                } else {
                    goCache.remove(cacheKey);
                }
            }
            return modifications.since(sinceModificationId);
        }
    }

    private boolean modificationExists(long sinceModificationId, Modifications modifications) {
        return modifications != null && modifications.hasModfication(sinceModificationId);
    }

    private boolean shouldCache(Modifications modifications) {
        return modifications.size() <= latestModificationsCacheLimit;
    }

    private Modifications _findModificationsSince(MaterialInstance materialInstance, long sinceModificationId) {
        return new Modifications(
                (List<Modification>) getHibernateTemplate().find("FROM Modification WHERE materialId = ? AND id >= ? ORDER BY id DESC", new Object[]{materialInstance.getId(), sinceModificationId}));
    }

    private void removeLatestCachedModification(final MaterialInstance materialInstance, Modification latest) {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                String cacheKey = latestMaterialModificationsKey(materialInstance);
                synchronized (cacheKey) {
                    goCache.remove(cacheKey);
                }
            }
        });
    }

    private void removeCachedModificationCountFor(final MaterialInstance materialInstance) {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                String key = materialModificationCountKey(materialInstance);
                synchronized (key) {
                    goCache.remove(key);
                }
            }
        });
    }

    private void removeCachedModificationsFor(final MaterialInstance materialInstance) {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                String key = materialModificationsWithPaginationKey(materialInstance);
                synchronized (key) {
                    goCache.remove(key);
                }
            }
        });
    }

    Modifications cachedModifications(MaterialInstance materialInstance) {
        return (Modifications) goCache.get(latestMaterialModificationsKey(materialInstance));
    }

    public MaterialRevisions findLatestModification(Material material) {
        MaterialInstance materialInstance = findMaterialInstance(material);
        if (materialInstance == null) {
            return new MaterialRevisions();
        }
        Materials materials = new Materials();
        materialExpansionService.expandForHistory(material, materials);
        MaterialRevisions allModifications = new MaterialRevisions();
        for (Material expanded : materials) {
            final MaterialInstance expandedInstance = findOrCreateFrom(expanded);
            Modification modification = findLatestModification(expandedInstance);
            if (modification != null) {
                allModifications.addRevision(expanded, modification);
            }
        }
        return allModifications;
    }

    Modification findLatestModification(final MaterialInstance expandedInstance) {
        Modifications modifications = cachedModifications(expandedInstance);
        if (modifications != null && !modifications.isEmpty()) {
            return modifications.get(0);
        }
        String cacheKey = latestMaterialModificationsKey(expandedInstance);
        synchronized (cacheKey) {
            Modification modification = (Modification) getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query query = session.createQuery("FROM Modification WHERE materialId = ? ORDER BY id DESC");
                    query.setMaxResults(1);
                    query.setLong(0, expandedInstance.getId());
                    return query.uniqueResult();
                }
            });
            goCache.put(cacheKey, new Modifications(modification));
            return modification;
        }
    }

    public void saveModifications(MaterialInstance materialInstance, List<Modification> newChanges) {
        if (newChanges.isEmpty()){
            return;
        }
        ArrayList<Modification> list = new ArrayList<>(newChanges);
        Collections.reverse(list);
        for (Modification modification : list) {
            modification.setMaterialInstance(materialInstance);
        }

        try {
            checkAndRemoveDuplicates(materialInstance, newChanges, list);
            getHibernateTemplate().saveOrUpdateAll(list);
        } catch (Exception e) {
            String message = "Cannot save modification: ";
            LOGGER.error(message, e);
            throw new RuntimeException(message + e.getMessage(), e);
        }
        for (Modification modification : list) {
            removeLatestCachedModification(materialInstance, modification);
        }
        removeCachedModificationCountFor(materialInstance);
        removeCachedModificationsFor(materialInstance);
    }

    private void checkAndRemoveDuplicates(MaterialInstance materialInstance, List<Modification> newChanges, ArrayList<Modification> list) {
        if (!new SystemEnvironment().get(SystemEnvironment.CHECK_AND_REMOVE_DUPLICATE_MODIFICATIONS))
            return;
        DetachedCriteria criteria = DetachedCriteria.forClass(Modification.class);
        criteria.setProjection(Projections.projectionList().add(Projections.property("revision")));
        criteria.add(Restrictions.eq("materialInstance.id", materialInstance.getId()));
        ArrayList<String> revisions = new ArrayList<>();
        for (Modification modification : newChanges) {
            revisions.add(modification.getRevision());
        }
        criteria.add(Restrictions.in("revision", revisions));
        List<String> matchingRevisionsFromDb = getHibernateTemplate().findByCriteria(criteria);
        if (!matchingRevisionsFromDb.isEmpty()) {
            for (final String revision : matchingRevisionsFromDb) {
                Modification modification = ListUtil.find(list, new ListUtil.Condition() {
                    @Override
                    public <T> boolean isMet(T item) {
                        return ((Modification) item).getRevision().equals(revision);
                    }
                });
                list.remove(modification);
            }
        }
        if (!newChanges.isEmpty() && list.isEmpty()) {
            throw new RuntimeException("All modifications already exist in db: " + revisions);
        }
        if (!matchingRevisionsFromDb.isEmpty()) {
            LOGGER.info("Saving revisions for material [{}] after removing the following duplicates {}",
                    materialInstance.toOldMaterial(null, null, null).getLongDescription(), matchingRevisionsFromDb);
        }

    }

    public Modification findModificationWithRevision(final Material material, final String revision) {
        return (Modification) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                try {
                    final long materialId = findOrCreateFrom(material).getId();
                    return MaterialRepository.this.findModificationWithRevision(session, materialId, revision);
                } catch (Exception e) {
                    LOGGER.error("Error while retrieving modification with material [" + material + "] containing revision [" + revision + "]", e);
                    throw e instanceof HibernateException ? (HibernateException) e : new RuntimeException(e);
                }
            }
        });
    }

    Modification findModificationWithRevision(Session session, long materialId, String revision) {
        Modification modification;
        String key = cacheKeyForModificationWithRevision(materialId, revision);
        modification = (Modification) goCache.get(key);
        if (modification == null) {
            synchronized (key) {
                modification = (Modification) goCache.get(key);
                if (modification == null) {
                    Query query = session.createQuery("FROM Modification WHERE materialId = ? and revision = ? ORDER BY id DESC");
                    query.setLong(0, materialId);
                    query.setString(1, revision);
                    modification = (Modification) query.uniqueResult();
                    goCache.put(key, modification);
                }
            }
        }
        return modification;
    }

    public MaterialRevisions findLatestRevisions(MaterialConfigs materialConfigs) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (MaterialConfig materialConfig : materialConfigs) {
            MaterialInstance materialInstance = findMaterialInstance(materialConfig);
            if (materialInstance != null) {
                Modification modification = findLatestModification(materialInstance);
                Material material = materialConfigConverter.toMaterial(materialConfig);
                materialRevisions.addRevision(modification == null ? new MaterialRevision(material) : new MaterialRevision(material, modification));
            }
        }
        return materialRevisions;
    }

    public boolean hasPipelineEverRunWith(final String pipelineName, final MaterialRevisions revisions) {
        return (Boolean) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                int numberOfMaterials = revisions.getRevisions().size();
                int match = 0;
                for (MaterialRevision revision : revisions) {
                    long materialId = findOrCreateFrom(revision.getMaterial()).getId();
                    long modificationId = revision.getLatestModification().getId();
                    String key = cacheKeyForHasPipelineEverRunWithModification(pipelineName, materialId, modificationId);
                    if (goCache.get(key) != null) {
                        match++;
                        continue;
                    }
                    String sql = "SELECT materials.id"
                            + " FROM pipelineMaterialRevisions"
                            + " INNER JOIN pipelines ON pipelineMaterialRevisions.pipelineId = pipelines.id"
                            + " INNER JOIN modifications on modifications.id  = pipelineMaterialRevisions.torevisionId"
                            + " INNER JOIN materials on modifications.materialId = materials.id"
                            + " WHERE materials.id = ? AND pipelineMaterialRevisions.toRevisionId >= ? AND pipelineMaterialRevisions.fromRevisionId <= ? AND pipelines.name = ?"
                            + " GROUP BY materials.id;";
                    SQLQuery query = session.createSQLQuery(sql);
                    query.setLong(0, materialId);
                    query.setLong(1, modificationId);
                    query.setLong(2, modificationId);
                    query.setString(3, pipelineName);
                    if (!query.list().isEmpty()) {
                        match++;
                        goCache.put(key, Boolean.TRUE);
                    }
                }
                return match == numberOfMaterials;
            }
        });
    }

    private String cacheKeyForHasPipelineEverRunWithModification(Object pipelineName, long materialId, long modificationId) {
        return String.format("%s_hasPipelineEverRunWithModification_%s_%s_%s", getClass().getName(), pipelineName, materialId, modificationId).intern();
    }

    @SuppressWarnings("unchecked")
    public List<MatchedRevision> findRevisionsMatching(final MaterialConfig materialConfig, final String searchString) {
        return (List<MatchedRevision>) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                String sql = "SELECT m.*"
                        + " FROM modifications AS m"
                        + " INNER JOIN materials mat ON mat.id = m.materialId"
                        + " WHERE mat.fingerprint = :finger_print"
                        + " AND (m.revision || ' ' || COALESCE(m.username, '') || ' ' || COALESCE(m.comment, '') LIKE :search_string OR m.pipelineLabel LIKE :search_string)"
                        + " ORDER BY m.id DESC"
                        + " LIMIT 5";
                SQLQuery query = session.createSQLQuery(sql);
                query.addEntity("m", Modification.class);
                Material material = materialConfigConverter.toMaterial(materialConfig);
                query.setString("finger_print", material.getFingerprint());
                query.setString("search_string", "%" + searchString + "%");
                final List<MatchedRevision> list = new ArrayList<>();
                for (Modification mod : (List<Modification>) query.list()) {
                    list.add(material.createMatchedRevision(mod, searchString));
                }
                return list;
            }
        });
    }

    public List<Modification> modificationFor(final StageIdentifier stageIdentifier) {
        if (stageIdentifier == null) {
            return null;
        }
        String key = cacheKeyForModificationsForStageLocator(stageIdentifier);
        List<Modification> modifications = (List<Modification>) goCache.get(key);
        if (modifications == null) {
            synchronized (key) {
                modifications = (List<Modification>) goCache.get(key);
                if (modifications == null) {
                    modifications = getHibernateTemplate().executeFind(new HibernateCallback() {
                        public Object doInHibernate(Session session) throws HibernateException, SQLException {
                            Query q = session.createQuery("FROM Modification WHERE revision = :revision ORDER BY id DESC");
                            q.setParameter("revision", stageIdentifier.getStageLocator());
                            return q.list();
                        }
                    });
                    if (!modifications.isEmpty()) {
                        goCache.put(key, modifications);
                    }
                }
            }
        }
        return modifications;
    }

    public Long getTotalModificationsFor(final MaterialInstance materialInstance) {
        String key = materialModificationCountKey(materialInstance);
        Long totalCount = (Long) goCache.get(key);
        if (totalCount == null || totalCount == 0) {
            synchronized (key) {
                totalCount = (Long) goCache.get(key);
                if (totalCount == null || totalCount == 0) {
                    totalCount = (Long) getHibernateTemplate().execute(new HibernateCallback() {
                        public Object doInHibernate(Session session) throws HibernateException, SQLException {
                            Query q = session.createQuery("select count(*) FROM Modification WHERE materialId = ?");
                            q.setLong(0, materialInstance.getId());
                            return q.uniqueResult();
                        }
                    });
                    goCache.put(key, totalCount);
                }
            }
        }
        return totalCount;
    }

    public Modifications getModificationsFor(final MaterialInstance materialInstance, final Pagination pagination) {
        String key = materialModificationsWithPaginationKey(materialInstance);
        String subKey = materialModificationsWithPaginationSubKey(pagination);
        Modifications modifications = (Modifications) goCache.get(key, subKey);
        if (modifications == null) {
            synchronized (key) {
                modifications = (Modifications) goCache.get(key, subKey);
                if (modifications == null) {
                    List<Modification> modificationsList = getHibernateTemplate().executeFind(new HibernateCallback() {
                        public Object doInHibernate(Session session) throws HibernateException, SQLException {
                            Query q = session.createQuery("FROM Modification WHERE materialId = ? ORDER BY id DESC");
                            q.setFirstResult(pagination.getOffset());
                            q.setMaxResults(pagination.getPageSize());
                            q.setLong(0, materialInstance.getId());
                            return q.list();
                        }
                    });
                    if (!modificationsList.isEmpty()) {
                        modifications = new Modifications(modificationsList);
                        goCache.put(key, subKey, modifications);
                    }
                }
            }
        }
        return modifications;
    }

    public Long latestModificationRunByPipeline(final CaseInsensitiveString pipelineName, final Material material) {
        final long materialId = findMaterialInstance(material).getId();
        String key = cacheKeyForLatestPmrForPipelineKey(materialId, pipelineName.toLower());
        Long modificationId = (Long) goCache.get(key);
        if (modificationId == null) {
            synchronized (key) {
                modificationId = (Long) goCache.get(key);
                if (modificationId == null) {
                    modificationId = (Long) getHibernateTemplate().execute(new HibernateCallback() {
                        public Object doInHibernate(Session session) throws HibernateException, SQLException {
                            SQLQuery sqlQuery = session.createSQLQuery("SELECT  MAX(pmr.toRevisionId) toRevisionId "
                                    + "FROM (SELECT torevisionid, pipelineid FROM pipelineMaterialRevisions WHERE materialid = :material_id)  AS pmr\n"
                                    + "INNER JOIN pipelines p ON ( p.name = :pipeline_name AND p.id = pmr.pipelineId)");

                            sqlQuery.setParameter("material_id", materialId);
                            sqlQuery.setParameter("pipeline_name", pipelineName.toString());
                            sqlQuery.addScalar("toRevisionId", new LongType());
                            return sqlQuery.uniqueResult();
                        }
                    });
                    if (modificationId == null) {
                        modificationId = -1L;
                    }
                    goCache.put(key, modificationId);
                }
            }
        }
        return modificationId;
    }

    private String cacheKeyForLatestPmrForPipelineKey(long materialId, final String lowerCasePipelineName) {
        return String.format("%s_latestPmrForPipeline_%s_andMaterial_%s", getClass().getName(), lowerCasePipelineName, materialId).intern();
    }

    String cacheKeyForNthLatestModification(int n, DependencyMaterial dependencyMaterial, PipelineIdentifier pipelineIdentifier) {
        return String.format("%s_nthLatestModificationFor_%s_forMaterial_%s_withIdentifier_%s", getClass().getName(), n, dependencyMaterial.getFingerprint(),
                pipelineIdentifier.pipelineLocator()).intern();
    }

    String cacheKeyForModificationWithRevision(long materialId, String revision) {
        return String.format("%s_findModificationWithRevision_ForMaterialId_%s_andRevision_%s", getClass().getName(), materialId, revision).intern();
    }

    String cacheKeyForModificationsForStageLocator(StageIdentifier stageIdentifier) {
        return String.format("%s_modificationsFor_%s", getClass().getName(), stageIdentifier.getStageLocator()).intern();
    }

    public File folderFor(Material material) {
        MaterialInstance materialInstance = this.findOrCreateFrom(material);
        return new File(new File("pipelines", "flyweight"), materialInstance.getFlyweightName());
    }

}
