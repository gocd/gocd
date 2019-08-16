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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.listener.DatabaseEntityChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.util.TriState;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
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

    private Set<DatabaseEntityChangeListener<Agent>> agentEntityChangeListenerSet = new HashSet<>();

    @Autowired
    public AgentDao(SessionFactory sessionFactory, GoCache cache, TransactionTemplate transactionTemplate, TransactionSynchronizationManager transactionSynchronizationManager, UuidGenerator uuidGenerator) {
        this.sessionFactory = sessionFactory;
        this.cache = cache;
        this.transactionTemplate = transactionTemplate;
        synchronizationManager = transactionSynchronizationManager;
        this.uuidGenerator = uuidGenerator;
        setSessionFactory(sessionFactory);
    }

    public void registerListener(DatabaseEntityChangeListener<Agent> listener) {
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
            synchronized (key) {
                agent = (Agent) cache.get(key);
                if (agent != null) {
                    return agent;
                }
                agent = fetchAgentFromDBByUUID(uuid);
                cache.put(key, agent);
            }
        }

        return agent;
    }

    public void associateCookie(final AgentIdentifier agentIdentifier, final String cookie) {
        final String uuid = agentIdentifier.getUuid();
        final String key = agentCacheKey(uuid);

        synchronized (key) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    Agent agent = fetchAgentFromDBByUUID(uuid);
                    if (agent == null) {
                        agent = new Agent(uuid, agentIdentifier.getHostName(), agentIdentifier.getIpAddress(), cookie);
                    } else {
                        agent.setFieldValues(cookie, agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                    }
                    getHibernateTemplate().saveOrUpdate(agent);

                    final Agent updatedAgent = agent;
                    synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            cache.remove(key);
                            notifyAgentEntityChangeListeners(updatedAgent);
                        }
                    });
                }
            });
        }
    }

    String agentCacheKey(String uuid) {
        return (AgentDao.class.getName() + "_agent_" + uuid).intern();
    }

    public List<Agent> getAllAgents() {
        return ((List<Agent>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Query query = sessionFactory.getCurrentSession().createQuery("FROM Agent where deleted = false");
            query.setCacheable(true);
            try {
                return query.list();
            } catch (Exception e) {
                return emptyList();
            }
        }));
    }

    public List<Agent> getAgentsByUUIDs(List<String> uuids) {
        return ((List<Agent>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Query query = sessionFactory.getCurrentSession().createQuery("FROM Agent where uuid in :uuids and deleted = false");
            query.setCacheable(true);
            query.setParameterList("uuids", uuids);
            return query.list();
        }));
    }

    public Agent fetchAgentFromDBByUUID(final String uuid) {
        return (Agent) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Query query = sessionFactory.getCurrentSession().createQuery("FROM Agent where uuid = :uuid and deleted = false");
            query.setCacheable(true);
            query.setParameter("uuid", uuid);
            return query.uniqueResult();
        });
    }

    public void saveOrUpdate(Agent agent) {
        final String key = agentCacheKey(agent.getUuid());
        updateAgentIdFromDBIfAgentDoesNotHaveAnIdAndAgentExistInDB(agent);
        synchronized (key) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            cache.remove(key);
                            notifyAgentEntityChangeListeners(agent);
                        }
                    });
                    sessionFactory.getCurrentSession().saveOrUpdate(agent);
                }
            });
        }
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

    public void enableOrDisableAgents(final List<String> uuids, final boolean disabled) {
        synchronized (uuids) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    String queryString = format("update Agent set disabled = :disabled where uuid in (:uuids)");
                    Query query = sessionFactory.getCurrentSession().createQuery(queryString);
                    query.setParameter("disabled", disabled);
                    query.setParameterList("uuids", uuids);
                    query.executeUpdate();

                    registerCommitCallbackToClearCacheAndNotifyBulkChangeListeners(synchronizationManager, uuids);
                }
            });
        }
    }

    public void bulkUpdateAttributes(List<Agent> agents, TriState state) {
        synchronized (agents) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    agents.forEach(agent -> sessionFactory.getCurrentSession().saveOrUpdate(Agent.class.getName(), agent));
                    List<String> uuids = agents.stream()
                            .map(Agent::getUuid)
                            .collect(toList());
                    registerCommitCallbackToClearCacheAndNotifyBulkChangeListeners(synchronizationManager, uuids);
                }
            });
        }
    }

    public void bulkSoftDelete(List<String> uuids) {
        synchronized (uuids) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    String queryString = format("update Agent set deleted = true where uuid in (:uuids)");
                    Query query = sessionFactory.getCurrentSession().createQuery(queryString);
                    query.setParameterList("uuids", uuids);
                    query.executeUpdate();

                    registerCommitCallbackToClearCacheAndNotifyBulkDeleteListeners(synchronizationManager, uuids);
                }
            });
        }
    }

    private synchronized void clearCacheAndNotifyListeners(List<String> uuids) {
        for (String uuid : uuids) {
            cache.remove(agentCacheKey(uuid));
        }
        notifyBulkAgentEntityChangeListeners(uuids);
    }


    private synchronized void clearCacheAndNotifyDeleteListeners(List<String> uuids) {
        for (String uuid : uuids) {
            cache.remove(agentCacheKey(uuid));
        }
        notifyBulkAgentEntityDeleteListeners(uuids);
    }

    private void notifyBulkAgentEntityDeleteListeners(List<String> uuids) {
        agentEntityChangeListenerSet.forEach(listener -> listener.bulkEntitiesDeleted(uuids));
    }

    private void registerCommitCallbackToClearCacheAndNotifyBulkChangeListeners(TransactionSynchronizationManager synchronizationManager, List<String> uuids) {
        synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                clearCacheAndNotifyListeners(uuids);
            }
        });
    }

    private void registerCommitCallbackToClearCacheAndNotifyBulkDeleteListeners(TransactionSynchronizationManager synchronizationManager, List<String> uuids) {
        synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                clearCacheAndNotifyDeleteListeners(uuids);
            }
        });
    }

    /**
     * @deprecated Used only in tests
     */
    public void clearListeners() {
        this.agentEntityChangeListenerSet.clear();
    }
}