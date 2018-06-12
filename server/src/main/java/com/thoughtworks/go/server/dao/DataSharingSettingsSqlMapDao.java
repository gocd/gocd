/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

@Component
public class DataSharingSettingsSqlMapDao extends HibernateDaoSupport {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;

    @Autowired
    public DataSharingSettingsSqlMapDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        setSessionFactory(sessionFactory);
    }

    public void saveOrUpdate(DataSharingSettings dataSharingSettings) throws DuplicateDataSharingSettingsException {
        DataSharingSettings existing = load();

        if (dataSharingSettings.hasId() && dataSharingSettings.getId() != existing.getId()) {
            throw new DuplicateDataSharingSettingsException();
        }

        if (existing != null && !dataSharingSettings.hasId()) {
            dataSharingSettings.setId(existing.getId());
        }

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                sessionFactory.getCurrentSession().saveOrUpdate(dataSharingSettings);
            }
        });
    }

    public DataSharingSettings load() {
        return transactionTemplate.execute(status -> (DataSharingSettings) sessionFactory.getCurrentSession().getNamedQuery("load.datasharing.settings").uniqueResult());
    }

    public class DuplicateDataSharingSettingsException extends Exception {
    }
}
