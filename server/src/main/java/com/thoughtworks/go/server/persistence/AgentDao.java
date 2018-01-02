/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.Agent;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.sql.SQLException;

/**
 * @understands persisting and retrieving agent uuid-cookie mapping
 */
@Service
public class AgentDao extends HibernateDaoSupport {
    private final GoCache cache;
    private final TransactionTemplate transactionTemplate;
    private final TransactionSynchronizationManager synchronizationManager;

    @Autowired
    public AgentDao(SessionFactory sessionFactory, GoCache cache, TransactionTemplate transactionTemplate, TransactionSynchronizationManager transactionSynchronizationManager) {
        this.cache = cache;
        this.transactionTemplate = transactionTemplate;
        synchronizationManager = transactionSynchronizationManager;
        setSessionFactory(sessionFactory);
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
                if (agent != null){
                    return agent;
                }
                agent = fetchAgentByUuid(uuid);
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
                @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                    Agent agent = fetchAgentByUuid(uuid);
                    if (agent == null) {
                        agent = new Agent(uuid, cookie, agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                    } else {
                        agent.update(cookie, agentIdentifier.getHostName(), agentIdentifier.getIpAddress());
                    }
                    getHibernateTemplate().saveOrUpdate(agent);
                    synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override public void afterCommit() {
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

    private Agent fetchAgentByUuid(final String uuid) {
        return (Agent) getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Query query = session.createQuery("from Agent where uuid = :uuid");
                query.setString("uuid", uuid);
                return query.uniqueResult();
            }
        });
    }
}
