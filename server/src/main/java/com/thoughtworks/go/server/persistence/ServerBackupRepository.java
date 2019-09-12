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

import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.domain.BackupStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServerBackupRepository extends HibernateDaoSupport {

    private final SessionFactory sessionFactory;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public ServerBackupRepository(SessionFactory sessionFactory, TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
        setSessionFactory(sessionFactory);
        this.sessionFactory = sessionFactory;
    }

    public Optional<ServerBackup> lastSuccessfulBackup() {
        List results = transactionTemplate.execute(status -> {
            Criteria criteria = sessionFactory.getCurrentSession().
                    createCriteria(ServerBackup.class)
                    .add(Restrictions.eq("status", BackupStatus.COMPLETED))
                    .setMaxResults(1)
                    .addOrder(Order.desc("id"));
            return criteria.list();
        });

        return results.isEmpty() ? Optional.empty() : Optional.of((ServerBackup) results.get(0));
    }

    public ServerBackup saveOrUpdate(ServerBackup serverBackup) {
        transactionTemplate.execute(status -> {
            sessionFactory.getCurrentSession().saveOrUpdate(serverBackup);
        });
        return serverBackup;
    }

    public void deleteAll() {
        transactionTemplate.execute(status -> {
            sessionFactory.getCurrentSession()
                    .createQuery("delete from ServerBackup")
                    .executeUpdate();

        });
    }

    public void markInProgressBackupsAsAborted(String message) {
        transactionTemplate.execute(status -> {
            sessionFactory.getCurrentSession()
                    .createQuery("UPDATE ServerBackup set status = :abortedStatus, message = :abortedMessage WHERE status = :inProgressStatus")
                    .setParameter("abortedStatus", BackupStatus.ABORTED)
                    .setParameter("abortedMessage", message)
                    .setParameter("inProgressStatus", BackupStatus.IN_PROGRESS)
                    .executeUpdate();
        });
    }

    public ServerBackup getBackup(long id) {
        return Optional
                .ofNullable(transactionTemplate.execute(status -> (ServerBackup) sessionFactory.getCurrentSession().get(ServerBackup.class, id)))
                .orElseThrow(() -> new RecordNotFoundException(EntityType.Backup, id));
    }

}
