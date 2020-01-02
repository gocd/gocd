/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.domain.BackupStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServerBackupRepository extends HibernateDaoSupport {


    @Autowired
    public ServerBackupRepository(SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
    }

    public Optional<ServerBackup> lastSuccessfulBackup() {
        List results = (List) getHibernateTemplate().execute((HibernateCallback) session -> {
            Criteria criteria = session.createCriteria(ServerBackup.class);
            criteria.add(Restrictions.eq("status", BackupStatus.COMPLETED));
            criteria.setMaxResults(1);
            criteria.addOrder(Order.desc("id"));
            return criteria.list();
        });

        return results.isEmpty() ? Optional.empty() : Optional.of((ServerBackup) results.get(0));
    }

    public ServerBackup save(ServerBackup serverBackup) {
        getHibernateTemplate().save(serverBackup);
        return serverBackup;
    }

    public void deleteAll() {
        getHibernateTemplate().execute((HibernateCallback) session -> session.createQuery(String.format("DELETE FROM %s", ServerBackup.class.getName())).executeUpdate());
    }

    public void markInProgressBackupsAsAborted(String message) {
        getHibernateTemplate().execute((HibernateCallback) session -> {
            Query query = session.createQuery(String.format("UPDATE %s set status = :abortedStatus, message = :abortedMessage WHERE status = :inProgressStatus", ServerBackup.class.getName()));
            query.setParameter("abortedStatus", BackupStatus.ABORTED);
            query.setParameter("abortedMessage", message);
            query.setParameter("inProgressStatus", BackupStatus.IN_PROGRESS);
            return query.executeUpdate();
        });
    }

    public Optional<ServerBackup> getBackup(String id) {
        if (StringUtils.isEmpty(id)) {
            return Optional.empty();
        }
        return getBackup(Long.parseLong(id));
    }

    public Optional<ServerBackup> getBackup(long id) {
        return Optional.ofNullable(getHibernateTemplate().get(ServerBackup.class, id));
    }

    public void update(ServerBackup serverBackup) {
        getHibernateTemplate().update(serverBackup);
    }
}
