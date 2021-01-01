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

import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.*;

@Component
public class UserSqlMapDao extends HibernateDaoSupport implements UserDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private GoCache goCache;
    private final AccessTokenDao accessTokenDao;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    protected static final String ENABLED_USER_COUNT_CACHE_KEY = "ENABLED_USER_COUNT_CACHE_KEY".intern();

    @Autowired
    public UserSqlMapDao(SessionFactory sessionFactory,
                         TransactionTemplate transactionTemplate,
                         GoCache goCache,
                         AccessTokenDao accessTokenDao, TransactionSynchronizationManager transactionSynchronizationManager) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
        this.accessTokenDao = accessTokenDao;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        setSessionFactory(sessionFactory);
    }

    @Override
    public void saveOrUpdate(final User user) {
        assertUserNotAnonymous(user);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        clearEnabledUserCountFromCache();
                    }
                });
                sessionFactory.getCurrentSession().saveOrUpdate(copyLoginToDisplayNameIfNotPresent(user));
            }
        });
    }

    @Override
    public User findUser(final String userName) {
        return (User) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            User user = (User) sessionFactory.getCurrentSession()
                    .createCriteria(User.class)
                    .add(Restrictions.eq("name", userName))
                    .setCacheable(true).uniqueResult();
            return user == null ? new NullUser() : user;
        });
    }

    @Override
    public Users findNotificationSubscribingUsers() {
        return (Users) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Criteria criteria = sessionFactory.getCurrentSession().createCriteria(User.class);
            criteria.setCacheable(true);
            criteria.add(Restrictions.isNotEmpty("notificationFilters"));
            criteria.add(Restrictions.eq("enabled", true));
            return new Users(criteria.list());
        });
    }

    @Override
    public Users allUsers() {
        return new Users((List<User>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Query query = sessionFactory.getCurrentSession().createQuery("FROM User");
            query.setCacheable(true);
            return query.list();
        }));
    }

    @Override
    public long enabledUserCount() {
        Long value = (Long) goCache.get(ENABLED_USER_COUNT_CACHE_KEY);
        if (value != null) {
            return value;
        }

        synchronized (ENABLED_USER_COUNT_CACHE_KEY) {
            value = (Long) goCache.get(ENABLED_USER_COUNT_CACHE_KEY);
            if (value == null) {
                value = hibernateTemplate().execute(session -> (Long) session.createCriteria(User.class).add(Restrictions.eq("enabled", true)).setProjection(Projections.rowCount()).setCacheable(true).uniqueResult());

                goCache.put(ENABLED_USER_COUNT_CACHE_KEY, value);
            }

            return value;
        }
    }

    @Override
    public void disableUsers(List<String> usernames) {
        changeEnabledStatus(usernames, false);
    }

    @Override
    public void enableUsers(List<String> usernames) {
        changeEnabledStatus(usernames, true);
    }

    @Override
    public List<User> enabledUsers() {
        List<User> enabledUsers = new ArrayList<>();
        for (User user : allUsers()) {
            if (user.isEnabled()) {
                enabledUsers.add(user);
            }
        }
        return enabledUsers;
    }

    @Override
    public Set<String> findUsernamesForIds(final Set<Long> userIds) {
        List<User> users = allUsers();
        Set<String> userNames = new HashSet<>();
        for (User user : users) {
            if (userIds.contains(user.getId())) {
                userNames.add(user.getName());
            }
        }
        return userNames;
    }

    @Override
    public User load(final long id) {
        return (User) transactionTemplate.execute((TransactionCallback) transactionStatus -> sessionFactory.getCurrentSession().get(User.class, id));
    }

    @Override
    public boolean deleteUser(final String username, String byWhom) {
        return (Boolean) transactionTemplate.execute((TransactionCallback) status -> {
            User user = findUser(username);
            if (user instanceof NullUser) {
                throw new RecordNotFoundException(EntityType.User, username);
            }
            if (user.isEnabled()) {
                throw new UserEnabledException();
            }
            sessionFactory.getCurrentSession().delete(user);
            accessTokenDao.revokeTokensBecauseOfUserDelete(Collections.singletonList(username), byWhom);
            return Boolean.TRUE;
        });
    }

    @Override
    public boolean deleteUsers(List<String> userNames, String byWhom) {
        return (Boolean) transactionTemplate.execute((TransactionCallback) status -> {
            String queryString = "delete from User where name in (:userNames)";
            Query query = sessionFactory.getCurrentSession().createQuery(queryString);
            query.setParameterList("userNames", userNames);
            query.executeUpdate();
            accessTokenDao.revokeTokensBecauseOfUserDelete(userNames, byWhom);
            return Boolean.TRUE;
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

    private void assertUserNotAnonymous(User user) {
        if (user.isAnonymous()) {
            throw new IllegalArgumentException(String.format("User name '%s' is not permitted.", user.getName()));
        }
    }

    private User copyLoginToDisplayNameIfNotPresent(User user) {
        if (StringUtils.isBlank(user.getDisplayName())) {
            user.setDisplayName(user.getName());
        }
        return user;
    }

    private void changeEnabledStatus(final List<String> usernames, final boolean enabled) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        clearEnabledUserCountFromCache();
                    }
                });
                String queryString = String.format("update %s set enabled = :enabled where name in (:userNames)", User.class.getName());
                Query query = sessionFactory.getCurrentSession().createQuery(queryString);
                query.setParameter("enabled", enabled);
                query.setParameterList("userNames", usernames);
                query.executeUpdate();
            }
        });
    }
}
