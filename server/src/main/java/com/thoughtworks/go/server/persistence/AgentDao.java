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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.Agent;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.util.TriState;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @understands persisting and retrieving agent uuid-cookie mapping
 */
@Service
public class AgentDao extends HibernateDaoSupport {
    private final GoCache cache;
    private final TransactionTemplate transactionTemplate;
    private final TransactionSynchronizationManager synchronizationManager;
    private final SessionFactory sessionFactory;
    private AgentChangeListener agentChangeListener;
    private final UuidGenerator uuidGenerator;

    @Autowired
    public AgentDao(SessionFactory sessionFactory, GoCache cache, TransactionTemplate transactionTemplate, TransactionSynchronizationManager transactionSynchronizationManager, UuidGenerator uuidGenerator) {
        this.sessionFactory = sessionFactory;
        this.cache = cache;
        this.transactionTemplate = transactionTemplate;
        synchronizationManager = transactionSynchronizationManager;
        this.uuidGenerator = uuidGenerator;
        setSessionFactory(sessionFactory);
    }

    public void registerListener(AgentChangeListener agentChangeListener) {
        this.agentChangeListener = agentChangeListener;
    }

    public String cookieFor(final AgentIdentifier agentIdentifier) {
        Agent agent = agentByUuid(agentIdentifier.getUuid());

        return null == agent ? null : agent.getCookie();
    }

    public Agent agentByUuid(String uuid) {
        String key = agentCacheKey(uuid);
        Agent agent = (Agent) cache.get(key);

        if (agent == null) {
            synchronized (key) {
                agent = (Agent) cache.get(key);
                if (agent != null) {
                    return agent;
                }
                agent = fetchAgentByUuid(uuid);
                cache.put(key, agent);
            }
        }

        return agent;
    }

    public AgentConfig agentConfigByUuid(String uuid) {
        Agent agent = agentByUuid(uuid);
        return agentConfigFromAgent(agent);
    }

    public void associateCookie(final AgentIdentifier agentIdentifier, final String cookie) {
        final String uuid = agentIdentifier.getUuid();
        final String key = agentCacheKey(uuid);
        synchronized (key) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    Agent agent = fetchAgentByUuid(uuid);
                    if (agent == null) {
                        agent = new Agent(uuid, cookie, agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                    } else {
                        agent.update(cookie, agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                    }
                    getHibernateTemplate().saveOrUpdate(agent);
                    synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            cache.remove(key);
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
        return getHibernateTemplate().execute(session -> (List<Agent>) session.createQuery("FROM Agent where deleted = false").setCacheable(true).list());
    }

    public List<AgentConfig> getAllAgentConfigs() {
        return getHibernateTemplate().execute(session -> {
            List<Agent> agents = session.createQuery("FROM Agent where deleted = false").setCacheable(true).list();
            return agents.stream().map(agent -> agentConfigFromAgent(agent)).collect(Collectors.toList());
        });
    }

    public List<Agent> getAllAgents(List<String> uuids) {
        return getHibernateTemplate().execute(session -> {
            Query query = session.createQuery("from Agent where uuid in :uuids and deleted = false");
            query.setParameterList("uuids", uuids);
            return (List<Agent>) query.setCacheable(true).list();
        });
    }


    private Agent fetchAgentByUuid(final String uuid) {
        Agent agent = (Agent) getHibernateTemplate().execute(session -> {
            Query query = session.createQuery("from Agent where uuid = :uuid and deleted = false");
            query.setString("uuid", uuid);
            return query.uniqueResult();
        });
        return agent;
    }

    public void saveOrUpdate(AgentConfig agentConfig) {
        final String key = agentCacheKey(agentConfig.getUuid());
        Agent agent = Agent.fromConfig(agentConfig);

        updateAgentObject(agent);
        synchronized (key) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {

                    synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            cache.remove(key);
                        }
                    });
                    sessionFactory.getCurrentSession().saveOrUpdate(agent);
                }
            });
            this.agentChangeListener.onEntityChange(agent);
        }
    }

    private void updateAgentObject(Agent agent) {
        Agent agentInDB = fetchAgentByUuid(agent.getUuid());

        if (agentInDB != null && agent.getId() == -1) {
            agent.setId(agentInDB.getId());
        }
    }

    public void saveOrUpdate(Agent agent) {
        final String key = agentCacheKey(agent.getUuid());
        updateAgentObject(agent);
        synchronized (key) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {

                    synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            cache.remove(key);
                        }
                    });

                    sessionFactory.getCurrentSession().saveOrUpdate(Agent.class.getName(), agent);
                }
            });
            this.agentChangeListener.onEntityChange(agent);
        }
    }

    public void changeDisabled(final List<String> uuids, final boolean disabled) {
        synchronized (uuids) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    String queryString = String.format("update %s set disabled = :disabled where uuid in (:uuids)", Agent.class.getName());
                    Query query = sessionFactory.getCurrentSession().createQuery(queryString);
                    query.setParameter("disabled", disabled);
                    query.setParameterList("uuids", uuids);
                    query.executeUpdate();

                    clearCache(synchronizationManager, uuids);
                }
            });
        }
    }

    public void bulkUpdateAttributes(final List<String> uuids, final List<String> resourcesToAdd, final List<String> resourcesToRemove, final List<String> environmentsToAdd, final List<String> environmentsToRemove, final TriState enable, AgentInstances agentInstances) {
        List<Agent> agents = getAllAgents(uuids);
        // Add all pending agents to the list of agents
        if (enable.isTrue()) {
            List<AgentConfig> pendingAgentConfigs = agentInstances.findPendingAgents(uuids);
            for (AgentConfig agentConfig : pendingAgentConfigs) {
                Agent agent = Agent.fromConfig(agentConfig);
                updateAgentObject(agent);
                if (agent.getCookie() == null) {
                    String cookie = uuidGenerator.randomUuid();
                    agent.setCookie(cookie);
                }
                agents.add(agent);
            }
        }

        synchronized (uuids) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {

                    for (Agent agent : agents) {
                        agent.addResources(resourcesToAdd);
                        agent.removeResources(resourcesToRemove);
                        agent.addEnvironments(environmentsToAdd);
                        agent.removeEnvironments(environmentsToRemove);
                        if (enable.isTrue()) {
                            agent.setDisabled(false);
                        } else if (enable.isFalse()) {
                            agent.setDisabled(true);
                        }
                        sessionFactory.getCurrentSession().saveOrUpdate(Agent.class.getName(), agent);
                    }

                    clearCache(synchronizationManager, uuids);
                }
            });
        }

        notifyListener();
    }

    public void bulkSoftDelete(List<String> uuids) {
        synchronized (uuids) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    String queryString = String.format("update %s set deleted = :deleted where uuid in (:uuids)", Agent.class.getName());
                    Query query = sessionFactory.getCurrentSession().createQuery(queryString);
                    query.setParameter("deleted", true);
                    query.setParameterList("uuids", uuids);
                    query.executeUpdate();

                    clearCache(synchronizationManager, uuids);
                }
            });
        }
    }

    private AgentConfig agentConfigFromAgent(Agent agent) {
        AgentConfig agentConfig = new AgentConfig(agent.getUuid(), agent.getHostname(), agent.getIpaddress(), agent.getResources());
        agentConfig.setElasticAgentId(agent.getElasticAgentId());
        agentConfig.setElasticPluginId(agent.getElasticPluginId());
        agentConfig.setDisabled(agent.isDisabled());
        agentConfig.setEnvironments(agent.getEnvironments());
        agentConfig.setCookie(agent.getCookie());
        agentConfig.setId(agent.getId());
        return agentConfig;
    }

    private synchronized void notifyListener() {
        this.agentChangeListener.onBulkEntityChange();
    }

    private void clearCache(TransactionSynchronizationManager synchronizationManager, List<String> uuids) {
        synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                for (String uuid : uuids) {
                    cache.remove(agentCacheKey(uuid));
                }
                notifyListener();
            }
        });
    }
}
