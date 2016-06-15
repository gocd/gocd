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

import java.sql.SQLException;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.AgentCookie;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.TransactionStatus;

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
        String key = agentCacheKey(agentIdentifier);
        AgentCookie cookie = (AgentCookie) cache.get(key);
        if (cookie == null) {
            synchronized (key) {
                cookie = findAgentCookieByUuid(agentIdentifier);
                cache.put(key, cookie);
            }
        }
        if (cookie != null) {
            return cookie.getCookie();
        }
        return null;
    }

    public void associateCookie(final AgentIdentifier agentIdentifier, final String cookie) {
        final String key = agentCacheKey(agentIdentifier);
        synchronized (key) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                    AgentCookie agentCookie = findAgentCookieByUuid(agentIdentifier);
                    if (agentCookie == null) {
                        agentCookie = new AgentCookie(agentIdentifier.getUuid(), cookie);
                    } else {
                        agentCookie.updateCookie(cookie);
                    }
                    getHibernateTemplate().saveOrUpdate(agentCookie);
                    synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                        @Override public void afterCommit() {
                            cache.remove(key);                            
                        }
                    });
                }
            });
        }
    }

    String agentCacheKey(AgentIdentifier identifier) {
        return (AgentDao.class.getName() + "_agent_" + identifier.getUuid()).intern();
    }

    private AgentCookie findAgentCookieByUuid(final AgentIdentifier agentIdentifier) {
        return (AgentCookie) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Query query = session.createQuery("from AgentCookie where uuid = :uuid");
                query.setString("uuid", agentIdentifier.getUuid());
                return query.uniqueResult();
            }
        });
    }
}
