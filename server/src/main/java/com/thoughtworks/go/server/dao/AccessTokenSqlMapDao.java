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

import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Clock;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.Collection;
import java.util.List;

@Component
public class AccessTokenSqlMapDao extends HibernateDaoSupport implements AccessTokenDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private Clock clock;

    @Autowired
    public AccessTokenSqlMapDao(SessionFactory sessionFactory,
                                TransactionTemplate transactionTemplate, Clock clock) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        setSessionFactory(sessionFactory);
    }

    public void saveOrUpdate(final AccessToken accessToken) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession().saveOrUpdate(AccessToken.class.getName(), accessToken);
            }
        });
    }

    @Override
    public List<AccessToken> findAllTokensForUser(String username) {
        return (List<AccessToken>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Query query = sessionFactory.getCurrentSession().createQuery("FROM AccessToken WHERE deletedBecauseUserDeleted = :deletedBecauseUserDeleted AND username = :username");
            query.setString("username", username);
            query.setBoolean("deletedBecauseUserDeleted", false);
            query.setCacheable(true);
            return query.list();
        });
    }

    @Override
    public List<AccessToken> findAllTokens() {
        return transactionTemplate.execute(status -> sessionFactory.getCurrentSession()
                .createQuery("FROM AccessToken")
                .setCacheable(true)
                .list());
    }

    @Override
    public AccessToken findAccessTokenBySaltId(String saltId) {
        return (AccessToken) transactionTemplate.execute((TransactionCallback) transactionStatus ->
                sessionFactory.getCurrentSession()
                        .createCriteria(AccessToken.class)
                        .add(Restrictions.eq("saltId", saltId))
                        .setCacheable(true).uniqueResult());
    }

    @Override
    public void revokeTokensBecauseOfUserDelete(Collection<String> usernames, String byWhom) {
        transactionTemplate.execute(status -> {
            Session currentSession = sessionFactory.getCurrentSession();
            usernames
                    .stream()
                    .flatMap(username -> findAllTokensForUser(username).stream())
                    .forEach(accessToken -> {
                        accessToken.revokeBecauseOfUserDelete(byWhom, clock.currentTimestamp());
                        currentSession.saveOrUpdate(accessToken);
                    });
            return Boolean.TRUE;
        });
    }

    public AccessToken load(final long id) {
        return (AccessToken) transactionTemplate.execute((TransactionCallback) transactionStatus -> sessionFactory.getCurrentSession().get(AccessToken.class, id));
    }

}
