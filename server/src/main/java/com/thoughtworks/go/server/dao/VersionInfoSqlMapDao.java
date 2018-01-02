/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

@Component
public class VersionInfoSqlMapDao extends HibernateDaoSupport implements VersionInfoDao{

    private final SessionFactory sessionFactory;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public VersionInfoSqlMapDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        setSessionFactory(sessionFactory);
    }

    @Override
    public void saveOrUpdate(final VersionInfo versionInfo) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession().saveOrUpdate(versionInfo);
            }
        });
    }

    @Override
    public VersionInfo findByComponentName(final String name) {
        return (VersionInfo) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                return sessionFactory.getCurrentSession()
                        .createCriteria(VersionInfo.class)
                        .add(Restrictions.eq("componentName", name))
                        .setCacheable(true).uniqueResult();
            }
        });
    }

    // used only in tests
    void deleteAll() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession().createQuery("DELETE FROM VersionInfo").executeUpdate();
            }
        });
    }
}
