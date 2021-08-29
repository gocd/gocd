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
package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.listener.DatabaseEntityChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.UuidGenerator;
import lombok.*;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * @understands persisting and retrieving agent uuid-cookie mapping
 */
@SuppressWarnings({"ALL"})
@Component
public class AgentDao extends HibernateDaoSupport {
    private final GoCache cache;
    private final TransactionTemplate transactionTemplate;
    private final TransactionSynchronizationManager synchronizationManager;
    private final SessionFactory sessionFactory;
    private final UuidGenerator uuidGenerator;
    private final AgentMutexes agentMutexes = new AgentMutexes();

    private Set<DatabaseEntityChangeListener<Agent>> agentEntityChangeListenerSet = new HashSet<>();

    @Autowired
    public AgentDao(SessionFactory sessionFactory, GoCache cache, TransactionTemplate transactionTemplate,
                    TransactionSynchronizationManager transactionSynchronizationManager, UuidGenerator uuidGenerator) {
        this.sessionFactory = sessionFactory;
        this.cache = cache;
        this.transactionTemplate = transactionTemplate;
        synchronizationManager = transactionSynchronizationManager;
        this.uuidGenerator = uuidGenerator;
        setSessionFactory(sessionFactory);
    }

    public void registerDatabaseAgentEntityChangeListener(DatabaseEntityChangeListener<Agent> listener) {
        this.agentEntityChangeListenerSet.add(listener);
    }

    public String cookieFor(final AgentIdentifier agentIdentifier) {
        Agent agent = getAgentByUUIDFromCacheOrDB(agentIdentifier.getUuid());
        return null == agent ? null : agent.getCookie();
    }

    public Agent getAgentByUUIDFromCacheOrDB(String uuid) {
        String key = agentCacheKey(uuid);
        Agent agent = (Agent) cache.get(key);

        if (agent == null) {
            List<String> uuids = singletonList(uuid);
            AgentMutex mutex = agentMutexes.acquire(uuids);
            synchronized (mutex) {
                agent = (Agent) cache.get(key);
                if (agent != null) {
                    return agent;
                }
                agent = fetchAgentFromDBByUUID(uuid);
                cache.put(key, agent);
                agentMutexes.release(uuids, mutex);
            }
        }

        return agent;
    }

    public void associateCookie(final AgentIdentifier agentIdentifier, final String cookie) {
        final String uuid = agentIdentifier.getUuid();
        final String key = agentCacheKey(uuid);

        List<String> uuids = singletonList(agentIdentifier.getUuid());
        AgentMutex mutex = agentMutexes.acquire(uuids);
        synchronized (mutex) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    Agent agent = fetchAgentFromDBByUUID(uuid);
                    if (agent == null) {
                        throw new UnregisteredAgentException(format("Agent [%s] is not registered.", uuid), uuid);
                    }
                    agent.setCookie(cookie);
                    getHibernateTemplate().saveOrUpdate(agent);

                    final Agent updatedAgent = agent;
                    registerAfterCommitCallback(() -> clearCacheAndNotifyAgentEntityChangeListeners(key, updatedAgent));
                }
            });
            agentMutexes.release(uuids, mutex);
        }
    }

    String agentCacheKey(String uuid) {
        return (AgentDao.class.getName() + "_agent_" + uuid).intern();
    }

    public List<Agent> getAllAgents() {
        return ((List<Agent>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            try {
                Query query = sessionFactory.getCurrentSession().createQuery("FROM Agent where deleted = false");
                query.setCacheable(true);
                return query.list();
            } catch (Exception e) {
                bomb("Error while retrieving list of all agents from DB.", e);
            }
            return emptyList();
        }));
    }

    public List<Agent> getAgentsByUUIDs(List<String> uuids) {
        AgentMutex mutex = agentMutexes.acquire(uuids);
        synchronized (mutex) {
            List<Agent> agents = (List<Agent>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
                Query query = sessionFactory.getCurrentSession().createQuery("FROM Agent where uuid in :uuids and deleted = false");
                query.setCacheable(true);
                query.setParameterList("uuids", uuids);
                return query.list();
            });
            agentMutexes.release(uuids, mutex);
            return agents;
        }
    }

    public Agent fetchAgentFromDBByUUID(final String uuid) {
        List<String> uuids = singletonList(uuid);
        AgentMutex mutex = agentMutexes.acquire(uuids);
        synchronized (mutex) {
            Agent agent = (Agent) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
                Query query = sessionFactory.getCurrentSession().createQuery("FROM Agent where uuid = :uuid and deleted = false");
                query.setCacheable(true);
                query.setParameter("uuid", uuid);
                return query.uniqueResult();
            });
            agentMutexes.release(uuids, mutex);
            return agent;
        }
    }

    public Agent fetchAgentFromDBByUUIDIncludingDeleted(final String uuid) {
        List<String> uuids = singletonList(uuid);
        AgentMutex mutex = agentMutexes.acquire(uuids);
        synchronized (mutex) {
            Agent agent = (Agent) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
                Query query = sessionFactory.getCurrentSession().createQuery("FROM Agent where uuid = :uuid");
                query.setCacheable(true);
                query.setParameter("uuid", uuid);
                return query.uniqueResult();
            });
            agentMutexes.release(uuids, mutex);
            return agent;
        }
    }

    public void saveOrUpdate(Agent agent) {
        final String key = agentCacheKey(agent.getUuid());
        updateAgentIdFromDBIfAgentDoesNotHaveAnIdAndAgentExistInDB(agent);
        List<String> uuids = singletonList(agent.getUuid());
        AgentMutex mutex = agentMutexes.acquire(uuids);

        synchronized (mutex) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    registerAfterCommitCallback(() -> clearCacheAndNotifyAgentEntityChangeListeners(key, agent));
                    sessionFactory.getCurrentSession().saveOrUpdate(agent);
                }
            });
            agentMutexes.release(uuids, mutex);
        }
    }

    private void clearCacheAndNotifyAgentEntityChangeListeners(String cachKey, Agent agent) {
        cache.remove(cachKey);
        notifyAgentEntityChangeListeners(agent);
    }

    private void notifyAgentEntityChangeListeners(Agent agent) {
        agentEntityChangeListenerSet.forEach(listener -> listener.entityChanged(agent));
    }

    private void notifyBulkAgentEntityChangeListeners(List<String> uuids) {
        agentEntityChangeListenerSet.forEach(listener -> {
            List<Agent> changedAgents = getAgentsByUUIDs(uuids);
            listener.bulkEntitiesChanged(changedAgents);
        });
    }

    public void updateAgentIdFromDBIfAgentDoesNotHaveAnIdAndAgentExistInDB(Agent agent) {
        Long idFromDB = (Long) getHibernateTemplate().execute(session -> {
            Query query = session.createQuery("select id from Agent where uuid = :uuid");
            query.setString("uuid", agent.getUuid());
            return query.uniqueResult();
        });

        if (idFromDB != null && agent.getId() == -1) {
            agent.setId(idFromDB);
        }
    }

    public void disableAgents(final List<String> uuids) {
        AgentMutex mutex = agentMutexes.acquire(uuids);
        synchronized (mutex) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    String queryString = format("update Agent set disabled = :disabled where uuid in (:uuids)");
                    Query query = sessionFactory.getCurrentSession().createQuery(queryString);
                    query.setParameter("disabled", true);
                    query.setParameterList("uuids", uuids);
                    query.executeUpdate();

                    registerAfterCommitCallback(() -> clearCacheAndNotifyBulkAgentEntityChangeListeners(uuids));
                }
            });
            agentMutexes.release(uuids, mutex);
        }
    }

    public void bulkUpdateAgents(List<Agent> agents) {
        List<String> uuids = agents.stream().map(agent -> agent.getUuid()).collect(toList());
        AgentMutex mutex = agentMutexes.acquire(uuids);
        synchronized (mutex) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    agents.forEach(agent -> sessionFactory.getCurrentSession().saveOrUpdate(Agent.class.getName(), agent));
                    List<String> uuids = agents.stream().map(Agent::getUuid).collect(toList());
                    registerAfterCommitCallback(() -> clearCacheAndNotifyBulkAgentEntityChangeListeners(uuids));
                }
            });
            agentMutexes.release(uuids, mutex);
        }
    }

    public void bulkSoftDelete(List<String> uuids) {
        AgentMutex mutex = agentMutexes.acquire(uuids);
        synchronized (mutex) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    String queryString = format("update Agent set deleted = true where uuid in (:uuids)");
                    Query query = sessionFactory.getCurrentSession().createQuery(queryString);
                    query.setParameterList("uuids", uuids);
                    query.executeUpdate();

                    registerAfterCommitCallback(() -> clearCacheAndNotifyBulkAgentEntityDeleteListeners(uuids));
                }
            });
            agentMutexes.release(uuids, mutex);
        }
    }

    private synchronized void clearCacheAndNotifyBulkAgentEntityChangeListeners(List<String> uuids) {
        uuids.stream().map(this::agentCacheKey).forEach(cache::remove);
        notifyBulkAgentEntityChangeListeners(uuids);
    }

    private synchronized void clearCacheAndNotifyBulkAgentEntityDeleteListeners(List<String> uuids) {
        uuids.stream().map(this::agentCacheKey).forEach(cache::remove);
        notifyBulkAgentEntityDeleteListeners(uuids);
    }

    private void notifyBulkAgentEntityDeleteListeners(List<String> uuids) {
        agentEntityChangeListenerSet.forEach(listener -> listener.bulkEntitiesDeleted(uuids));
    }

    private void registerAfterCommitCallback(Runnable runnable) {
        synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

    @TestOnly
    public void clearListeners() {
        this.agentEntityChangeListenerSet.clear();
    }
}

class AgentMutexes {
    private Map<String, AgentMutex> uuidToMutexMap = new ConcurrentHashMap<>();

    @Override
    public String toString() {
        return "AgentMutexes ( uuidToMutexMap=" + uuidToMutexMap + " )";
    }

    public synchronized void release(List<String> uuids, AgentMutex mutex) {
        mutex.setUsedByCount(mutex.getUsedByCount() - 1);
        if (mutex.getUsedByCount() <= 0) {
            uuids.forEach(uuid -> uuidToMutexMap.remove(uuid));
            uuidToMutexMap.entrySet().removeIf(entry -> entry.getValue().equals(mutex));
        }
    }

    public synchronized AgentMutex acquire(List<String> uuids) {
        return uuids.stream()
                .filter(uuid -> uuidToMutexMap.containsKey(uuid))
                .map(this::getExistingMutexWithIncrementedUsedByCount)
                .findAny()
                .orElseGet(() -> createNewMutex(uuids));
    }

    private AgentMutex createNewMutex(List<String> uuids) {
        String mutexId = "Mutex-" + new UuidGenerator().randomUuid();
        AgentMutex mutex = new AgentMutex(mutexId, 1);
        uuids.forEach(uuid -> uuidToMutexMap.putIfAbsent(uuid, mutex));
        return mutex;
    }

    private AgentMutex getExistingMutexWithIncrementedUsedByCount(String uuid) {
        AgentMutex mutex = uuidToMutexMap.get(uuid);
        mutex.setUsedByCount(mutex.getUsedByCount() + 1);
        return mutex;
    }
}

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
class AgentMutex {
    private String id;
    private long usedByCount;

    @Override
    public synchronized String toString() {
        return "Agent Mutex { id=[" + id + "]" + ", usedByCount=[" + usedByCount + "] }";
    }
}