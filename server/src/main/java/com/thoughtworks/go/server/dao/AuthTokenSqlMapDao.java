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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.AuthToken;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.Date;

@Component
public class AuthTokenSqlMapDao extends HibernateDaoSupport implements AuthTokenDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private GoCache goCache;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    protected static final String ENABLED_USER_COUNT_CACHE_KEY = "ENABLED_USER_COUNT_CACHE_KEY".intern();

    @Autowired
    public AuthTokenSqlMapDao(SessionFactory sessionFactory,
                              TransactionTemplate transactionTemplate,
                              GoCache goCache,
                              TransactionSynchronizationManager transactionSynchronizationManager) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        setSessionFactory(sessionFactory);
    }

    public void saveOrUpdate(final AuthToken authToken) {
        validateAuthToken(authToken);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        clearEnabledUserCountFromCache();
                    }
                });
                sessionFactory.getCurrentSession().saveOrUpdate(setCreatedOrUpdatedTime(authToken));
            }
        });
    }

    public AuthToken findAuthToken(final String tokenName) {
        return (AuthToken) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            AuthToken token = (AuthToken) sessionFactory.getCurrentSession()
                    .createCriteria(AuthToken.class)
                    .add(Restrictions.eq("name", tokenName))
                    .setCacheable(true).uniqueResult();
            return token;
        });
    }

    public AuthToken load(final long id) {
        return (AuthToken) transactionTemplate.execute((TransactionCallback) transactionStatus -> sessionFactory.getCurrentSession().get(AuthToken.class, id));
    }

    // Used only by tests
    public void deleteAll() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        clearEnabledUserCountFromCache();
                    }
                });
                sessionFactory.getCurrentSession().createQuery("DELETE FROM AuthToken").executeUpdate();
            }
        });
    }

    private void clearEnabledUserCountFromCache() {
        synchronized (ENABLED_USER_COUNT_CACHE_KEY) {
            goCache.remove(ENABLED_USER_COUNT_CACHE_KEY);
        }
    }

    protected HibernateTemplate hibernateTemplate() {
        return getHibernateTemplate();
    }

    private void validateAuthToken(AuthToken authToken) {
//        if (authToken.isAnonymous()) {
//            throw new IllegalArgumentException(String.format("User name '%s' is not permitted.", authToken.getName()));
//        }
    }


    private AuthToken setCreatedOrUpdatedTime(AuthToken token) {
        if (token.getCreatedAt() != null) {
            token.setCreatedAt(new Date());
        }

        return token;
    }

}
