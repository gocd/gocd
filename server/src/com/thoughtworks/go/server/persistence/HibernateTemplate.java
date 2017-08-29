/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.orm.hibernate4.SessionFactoryUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class HibernateTemplate {
    private final SessionFactory sessionFactory;
    private final TransactionTemplate transactionTemplate;
    private SQLExceptionTranslator jdbcExceptionTranslator;

    public HibernateTemplate(SessionFactory sessionFactory, TransactionTemplate transactionTemplate) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        getJdbcExceptionTranslator();
    }

    public <T> T execute(HibernateCallback<T> action) {
        return transactionTemplate.execute(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(TransactionStatus status) {
                Session currentSession = null;
                boolean newSessionOpened = false;

                try {
                    currentSession = sessionFactory.getCurrentSession();
                } catch (HibernateException e) {
                    currentSession = sessionFactory.openSession();
                    newSessionOpened = true;
                }
                try {
                    return action.doInHibernate(currentSession);
                } catch (HibernateException e) {
                    throw SessionFactoryUtils.convertHibernateAccessException(e);
                } catch (SQLException e) {
                    throw getJdbcExceptionTranslator().translate("Hibernate-related JDBC operation", null, e);
                } finally {
                    if (newSessionOpened && currentSession != null) {
                        currentSession.close();
                    }
                }
            }
        });
    }

    private synchronized SQLExceptionTranslator getJdbcExceptionTranslator() {
        if (this.jdbcExceptionTranslator == null) {
            this.jdbcExceptionTranslator = newJdbcExceptionTranslator();
        }
        return this.jdbcExceptionTranslator;
    }

    private SQLExceptionTranslator newJdbcExceptionTranslator() {
        DataSource ds = getDataSource();
        if (ds != null) {
            return new SQLErrorCodeSQLExceptionTranslator(ds);
        }
        return new SQLStateSQLExceptionTranslator();
    }

    private DataSource getDataSource() {
        DataSource dataSource = null;
        if (sessionFactory instanceof SessionFactoryImpl) {
            ConnectionProvider connectionProvider = ((SessionFactoryImpl) sessionFactory).getConnectionProvider();
            if (connectionProvider instanceof DatasourceConnectionProviderImpl) {
                dataSource = ((DatasourceConnectionProviderImpl) connectionProvider).getDataSource();
            }
        }
        return dataSource;
    }

    public <T> List<T> findByCriteria(DetachedCriteria criteria) {
        return execute(new HibernateCallback<List<T>>() {
            @Override
            public List<T> doInHibernate(Session session) throws HibernateException, SQLException {
                return criteria.getExecutableCriteria(session).list();
            }
        });
    }

    public int executeUpdate(String query, Object... params) {
        return execute(new HibernateCallback<Integer>() {
            @Override
            public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                return buildQuery(session, query, params).executeUpdate();
            }
        });
    }

    public void saveOrUpdate(Object object) {
        execute(new HibernateCallback<Void>() {
            @Override
            public Void doInHibernate(Session session) throws HibernateException, SQLException {
                session.saveOrUpdate(object);
                return null;
            }
        });
    }

    public <T> T save(T object) {
        return execute(new HibernateCallback<T>() {
            @Override
            public T doInHibernate(Session session) throws HibernateException, SQLException {
                return (T) session.save(object);
            }
        });
    }

    public <T> List<T> find(String queryString, Object... params) {
        return execute(new HibernateCallback<List<T>>() {
            @Override
            public List<T> doInHibernate(Session session) throws HibernateException, SQLException {
                return buildQuery(session, queryString, params).list();
            }
        });
    }

    private Query buildQuery(Session session, String queryString, Object... params) {
        Query query = session.createQuery(queryString);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i, params[i]);
        }
        return query;
    }

    public <T> T findFirst(String queryString, Object... params) {
        return execute(new HibernateCallback<T>() {
            @Override
            public T doInHibernate(Session session) throws HibernateException, SQLException {
                return (T) buildQuery(session, queryString, params).uniqueResult();
            }
        });
    }

    public void saveOrUpdateAll(List<?> list) {
        execute(new HibernateCallback<Void>() {
            @Override
            public Void doInHibernate(Session session) throws HibernateException, SQLException {
                for (Object o : list) {
                    session.saveOrUpdate(o);
                }
                return null;
            }
        });
    }

    public <T> T load(Class<T> entityClass, Serializable id) throws DataAccessException {
        return execute(new HibernateCallback<T>() {
            @Override
            public T doInHibernate(Session session) throws HibernateException, SQLException {
                return (T) session.load(entityClass, id);
            }
        });
    }

    public void bulkUpdate(String query, Object... params) {
        executeUpdate(query, params);
    }

    public <T> T get(Class<?> entityClass, Serializable id) {
        return execute(new HibernateCallback<T>() {
            @Override
            public T doInHibernate(Session session) throws HibernateException, SQLException {
                return (T) session.get(entityClass, id);
            }
        });
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void delete(Object o) {
        execute(new HibernateCallback<Void>() {
            @Override
            public Void doInHibernate(Session session) throws HibernateException, SQLException {
                session.delete(o);
                return null;
            }
        });
    }
}
