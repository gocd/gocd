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

package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.server.domain.ServerBackup;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServerBackupRepository extends HibernateDaoSupport {


    @Autowired
    public ServerBackupRepository(SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
    }

    public ServerBackup lastBackup() {
        List results = (List) getHibernateTemplate().execute((HibernateCallback) session -> {
            Criteria criteria = session.createCriteria(ServerBackup.class);
            criteria.setMaxResults(1);
            criteria.addOrder(Order.desc("id"));
            return criteria.list();
        });

        return results.isEmpty() ? null : (ServerBackup) results.get(0);
    }

    public void save(ServerBackup serverBackup) {
        getHibernateTemplate().save(serverBackup);
    }

    public void deleteAll() {
        getHibernateTemplate().execute((HibernateCallback) session -> session.createQuery(String.format("DELETE FROM %s", ServerBackup.class.getName())).executeUpdate());
    }
}
