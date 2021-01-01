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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.service.AccessTokenFilter;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Clock;
import org.hibernate.Criteria;
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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Override
    public void saveOrUpdate(final AccessToken accessToken) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession().saveOrUpdate(AccessToken.class.getName(), accessToken);
            }
        });
    }

    @Override
    public List<AccessToken> findAllTokensForUser(String username, AccessTokenFilter filter) {
        return (List<AccessToken>) transactionTemplate.execute((TransactionCallback) transactionStatus ->
        {
            Criteria criteria = sessionFactory
                    .getCurrentSession()
                    .createCriteria(AccessToken.class)
                    .add(Restrictions.eq("username", username))
                    .add(Restrictions.eq("deletedBecauseUserDeleted", false));

            filter.applyTo(criteria);

            return criteria
                    .setCacheable(true)
                    .list();
        });
    }

    @Override
    public List<AccessToken> findAllTokens(AccessTokenFilter filter) {
        return transactionTemplate.execute(status -> {
            Criteria criteria = sessionFactory.getCurrentSession()
                    .createCriteria(AccessToken.class);

            filter.applyTo(criteria);

            return criteria
                    .setCacheable(true)
                    .list();
        });
    }

    @Override
    public AccessToken findAccessTokenBySaltId(String saltId) {
        return (AccessToken) transactionTemplate.execute((TransactionCallback) transactionStatus ->
                sessionFactory.getCurrentSession()
                        .createCriteria(AccessToken.class)
                        .add(Restrictions.eq("saltId", saltId))
                        .add(Restrictions.eq("deletedBecauseUserDeleted", false))
                        .setCacheable(true).uniqueResult());
    }

    @Override
    public void revokeTokensBecauseOfUserDelete(Collection<String> usernames, String byWhom) {
        transactionTemplate.execute(status -> {
            Session currentSession = sessionFactory.getCurrentSession();
            usernames
                    .stream()
                    .flatMap(username -> findAllTokensForUser(username, AccessTokenFilter.all).stream())
                    .forEach(accessToken -> {
                        accessToken.revokeBecauseOfUserDelete(byWhom, clock.currentTimestamp());
                        currentSession.saveOrUpdate(accessToken);
                    });
            return Boolean.TRUE;
        });
    }

    @Override
    public AccessToken loadForAdminUser(final long id) {
        return (AccessToken) transactionTemplate.execute((TransactionCallback) transactionStatus -> sessionFactory.getCurrentSession().get(AccessToken.class, id));
    }

    @Override
    public AccessToken loadNotDeletedTokenForUser(long id, String username) {
        return (AccessToken) transactionTemplate.execute(transactionCallback ->
                sessionFactory
                        .getCurrentSession()
                        .createCriteria(AccessToken.class)
                        .add(Restrictions.eq("username", username))
                        .add(Restrictions.eq("id", id))
                        .add(Restrictions.eq("deletedBecauseUserDeleted", false))
                        .setCacheable(true)
                        .uniqueResult());

    }

    @Override
    public void updateLastUsedTime(Map<Long, Timestamp> accessTokenIdToLastUsedTimestamp) {
        transactionTemplate.execute(transactionCallback -> {
            final Session currentSession = sessionFactory.getCurrentSession();

            accessTokenIdToLastUsedTimestamp.keySet().forEach(tokenId -> {
                final Query query = currentSession.createQuery("UPDATE AccessToken SET lastUsed = :lastUsed WHERE id = :id");
                query.setLong("id", tokenId);
                query.setTimestamp("lastUsed", accessTokenIdToLastUsedTimestamp.get(tokenId));
                query.executeUpdate();
            });

            return Boolean.TRUE;
        });
    }

}
