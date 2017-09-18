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

import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class ServerBackupRepository {

    private final HibernateTemplate hibernateTemplate;

    @Autowired
    public ServerBackupRepository(SessionFactory sessionFactory, TransactionTemplate transactionTemplate) {
        this.hibernateTemplate = new HibernateTemplate(sessionFactory, transactionTemplate);
    }

    public ServerBackup lastBackup() {
        return getHibernateTemplate().execute(new HibernateCallback<ServerBackup>() {
            @Override
            public ServerBackup doInHibernate(Session session) throws HibernateException, SQLException {
                return (ServerBackup) session
                        .createQuery("from ServerBackup order by id desc")
                        .setMaxResults(1)
                        .uniqueResult();
            }
        });
    }

    public void save(ServerBackup serverBackup) {
        getHibernateTemplate().save(serverBackup);
    }

    public void deleteAll() {
        getHibernateTemplate().execute(new HibernateCallback<Void>() {
            public Void doInHibernate(Session session) throws HibernateException, SQLException {
                session.createQuery(String.format("DELETE FROM %s", ServerBackup.class.getName())).executeUpdate();
                return null;
            }
        });
    }

    private HibernateTemplate getHibernateTemplate() {
        return hibernateTemplate;
    }
}
