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

import java.util.List;

@Component
public class AuthTokenSqlMapDao extends HibernateDaoSupport implements AuthTokenDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;

    @Autowired
    public AuthTokenSqlMapDao(SessionFactory sessionFactory,
                              TransactionTemplate transactionTemplate) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        setSessionFactory(sessionFactory);
    }

    public void saveOrUpdate(final AuthToken authToken) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession().saveOrUpdate(authToken);
            }
        });
    }

    public AuthToken findAuthToken(final String tokenName, String username) {
        return (AuthToken) transactionTemplate.execute((TransactionCallback) transactionStatus ->
                sessionFactory.getCurrentSession()
                        .createCriteria(AuthToken.class)
                        .add(Restrictions.eq("name", tokenName))
                        .add(Restrictions.eq("username", username))
                        .setCacheable(true).uniqueResult());
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

    @Override
    public AuthToken findTokenBySaltId(String saltId) {
        return (AuthToken) transactionTemplate.execute((TransactionCallback) transactionStatus ->
                sessionFactory.getCurrentSession()
                        .createCriteria(AuthToken.class)
                        .add(Restrictions.eq("saltId", saltId))
                        .setCacheable(true).uniqueResult());
    }

    public AuthToken load(final long id) {
        return (AuthToken) transactionTemplate.execute((TransactionCallback) transactionStatus -> sessionFactory.getCurrentSession().get(AuthToken.class, id));
    }

    // Used only by tests
    public void deleteAll() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession().createQuery("DELETE FROM AuthToken").executeUpdate();
            }
        });
    }
}
