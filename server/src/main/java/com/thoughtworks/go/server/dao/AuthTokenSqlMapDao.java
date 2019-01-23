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
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.List;

@Component
public class AuthTokenSqlMapDao extends HibernateDaoSupport implements AuthTokenDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private GoCache goCache;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    protected static final String AUTH_TOKEN_CACHE_KEY = "AUTH_TOKEN_CACHE_KEY".intern();

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
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        removeFromCache(authToken);
                    }
                });
                sessionFactory.getCurrentSession().saveOrUpdate(authToken);
            }
        });
    }

    public AuthToken findAuthToken(final String tokenName, String username) {
        AuthToken token = (AuthToken) goCache.get(AUTH_TOKEN_CACHE_KEY, tokenName);
        if (token == null) {
            synchronized (tokenName) {
                token = (AuthToken) goCache.get(AUTH_TOKEN_CACHE_KEY, tokenName);
                if (token != null) {
                    return token;
                }

                token = (AuthToken) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
                    AuthToken savedToken = (AuthToken) sessionFactory.getCurrentSession()
                            .createCriteria(AuthToken.class)
                            .add(Restrictions.eq("name", tokenName))
                            .add(Restrictions.eq("username", username))
                            .setCacheable(true).uniqueResult();
                    return savedToken;
                });

                if (token != null) {
                    populateInCache(token);
                }
            }
        }

        return token;
    }

    @Override
    public List<AuthToken> findAllTokensForUser(String username) {
        return (List<AuthToken>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Query query = sessionFactory.getCurrentSession().createQuery("FROM AuthToken where username = :username");
            query.setString("username", username);
            query.setCacheable(true);
            return query.list();
        });
    }

    public AuthToken load(final long id) {
        return (AuthToken) transactionTemplate.execute((TransactionCallback) transactionStatus -> sessionFactory.getCurrentSession().get(AuthToken.class, id));
    }

    private void populateInCache(AuthToken token) {
        String cacheKey = String.format("%s_%s", token.getUsername(), token.getName());

        synchronized (cacheKey) {
            goCache.put(AUTH_TOKEN_CACHE_KEY, cacheKey, token);
        }
        synchronized (token.getValue()) {
            goCache.put(AUTH_TOKEN_CACHE_KEY, token.getValue(), token);
        }
    }

    private void removeFromCache(AuthToken token) {
        String cacheKey = String.format("%s_%s", token.getUsername(), token.getName());

        synchronized (cacheKey) {
            goCache.remove(AUTH_TOKEN_CACHE_KEY, cacheKey);
        }
        synchronized (token.getValue()) {
            goCache.remove(AUTH_TOKEN_CACHE_KEY, token.getValue());
        }
    }

    // Used only by tests
    public void deleteAll() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        clearAllAuthTokensFromCache();
                    }
                });
                sessionFactory.getCurrentSession().createQuery("DELETE FROM AuthToken").executeUpdate();
            }
        });
    }

    //required only for tests
    private void clearAllAuthTokensFromCache() {
        synchronized (AUTH_TOKEN_CACHE_KEY) {
            goCache.remove(AUTH_TOKEN_CACHE_KEY);
        }
    }
}
