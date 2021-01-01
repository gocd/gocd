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

import com.thoughtworks.go.domain.EnvironmentVariable;
import com.thoughtworks.go.domain.EnvironmentVariableType;
import com.thoughtworks.go.domain.EnvironmentVariables;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.Collection;
import java.util.List;

@Component
public class EnvironmentVariableSqlMapDao implements EnvironmentVariableDao {
    private SessionFactory sessionFactory;
    private TransactionTemplate transactionTemplate;

    @Autowired
    public EnvironmentVariableSqlMapDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void save(final Long entityId, final EnvironmentVariableType type, final EnvironmentVariables variables) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (EnvironmentVariable variable : variables) {
                    EnvironmentVariable environmentVariableConfig = new EnvironmentVariable(variable.getName(), variable.getValue(), variable.isSecure());
                    environmentVariableConfig.setEntityId(entityId);
                    environmentVariableConfig.setEntityType(type.toString());
                    sessionFactory.getCurrentSession().save(environmentVariableConfig);
                }
            }
        });
    }

    @Override
    public EnvironmentVariables load(final Long entityId, final EnvironmentVariableType type) {
        List<EnvironmentVariable> result = (List<EnvironmentVariable>) transactionTemplate.execute((TransactionCallback) transactionStatus -> {
            Criteria criteria = sessionFactory.getCurrentSession().createCriteria(EnvironmentVariable.class).add(Restrictions.eq("entityId", entityId)).add(
                    Restrictions.eq("entityType", type.toString())).addOrder(Order.asc("id"));
            criteria.setCacheable(true);
            return criteria.list();
        });
        return new EnvironmentVariables(result);
    }

    @Override
    public void deleteAll(final Collection<EnvironmentVariable> variables) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (EnvironmentVariable variable : variables) {
                    sessionFactory.getCurrentSession().delete(variable);
                }
            }
        });
    }
}
