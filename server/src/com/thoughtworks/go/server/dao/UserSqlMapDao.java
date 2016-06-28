/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.exceptions.UserNotFoundException;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.StringUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

@Component
public class UserSqlMapDao  extends HibernateDaoSupport implements UserDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;
    private GoCache goCache;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    protected static final String ENABLED_USER_COUNT_CACHE_KEY = "ENABLED_USER_COUNT_CACHE_KEY".intern();

    @Autowired
    public UserSqlMapDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate, GoCache goCache, TransactionSynchronizationManager transactionSynchronizationManager) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        setSessionFactory(sessionFactory);
    }

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

    public User findUser(final String userName) {
        return (User) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                User user = (User) sessionFactory.getCurrentSession()
                        .createCriteria(User.class)
                        .add(Restrictions.eq("name", userName))
                        .setCacheable(true).uniqueResult();
                return user == null ? new NullUser() : user;
            }
        });
    }

    public Users findNotificationSubscribingUsers() {
        return (Users) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                Criteria criteria = sessionFactory.getCurrentSession().createCriteria(User.class);
                criteria.setCacheable(true);
                criteria.add(Restrictions.isNotEmpty("notificationFilters"));
                return new Users(criteria.list());
            }
        });
    }

    public Users allUsers() {
        return new Users((List<User>) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                Query query = sessionFactory.getCurrentSession().createQuery("FROM User");
                query.setCacheable(true);
                return query.list();
            }
        }));
    }

    public Integer enabledUserCount() {
        Integer value = (Integer) goCache.get(ENABLED_USER_COUNT_CACHE_KEY);
        if (value != null) {
            return value;
        }

        synchronized (ENABLED_USER_COUNT_CACHE_KEY) {
            value = (Integer) goCache.get(ENABLED_USER_COUNT_CACHE_KEY);
            if (value == null) {
                value = (Integer) hibernateTemplate().execute(new HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate(Session session) throws HibernateException, SQLException {
                        return session.createCriteria(User.class).add(Restrictions.eq("enabled", true)).setProjection(Projections.rowCount()).setCacheable(true).uniqueResult();
                    }
                });

                goCache.put(ENABLED_USER_COUNT_CACHE_KEY, value);
            }

            return value;
        }
    }

    // Used only by Twist tests
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
                sessionFactory.getCurrentSession().createQuery("DELETE FROM User").executeUpdate();
            }
        });
    }

    public void disableUsers(List<String> usernames) {
        changeEnabledStatus(usernames, false);
    }

    public void enableUsers(List<String> usernames) {
        changeEnabledStatus(usernames, true);
    }

    public List<User> enabledUsers() {
        List<User> enabledUsers = new ArrayList<>();
        for (User user : allUsers()) {
            if (user.isEnabled()) {
                enabledUsers.add(user);
            }
        }
        return enabledUsers;
    }

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

    public User load(final long id) {
        return (User) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                return sessionFactory.getCurrentSession().load(User.class, id);
            }
        });
    }

    @Override
    public boolean deleteUser(final String username) {
        return (Boolean) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                User user = findUser(username);
                if (user instanceof NullUser) {
                    throw new UserNotFoundException();
                }
                if (user.isEnabled()) {
                    throw new UserEnabledException();
                }
                sessionFactory.getCurrentSession().delete(user);
                return Boolean.TRUE;
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

    private void assertUserNotAnonymous(User user) {
        if (user.isAnonymous()) {
            throw new IllegalArgumentException(String.format("User name '%s' is not permitted.", user.getName()));
        }
    }

    private User copyLoginToDisplayNameIfNotPresent(User user) {
        if (StringUtil.isBlank(user.getDisplayName())) {
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
