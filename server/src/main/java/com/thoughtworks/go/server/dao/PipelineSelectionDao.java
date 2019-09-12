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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;

@Component
public class PipelineSelectionDao extends HibernateDaoSupport {

    private final SessionFactory sessionFactory;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public PipelineSelectionDao(SessionFactory sessionFactory, TransactionTemplate transactionTemplate) {
        this.sessionFactory = sessionFactory;
        this.transactionTemplate = transactionTemplate;
        setSessionFactory(sessionFactory);
    }

    public long saveOrUpdate(PipelineSelections pipelineSelections) {
        return this.transactionTemplate.execute(status -> {
            sessionFactory.getCurrentSession().saveOrUpdate(pipelineSelections);
            return pipelineSelections.getId();
        });
    }

    public PipelineSelections findPipelineSelectionsById(long id) {
        return this.transactionTemplate.execute(status ->
                (PipelineSelections) sessionFactory.getCurrentSession().get(PipelineSelections.class, id)
        );
    }

    public PipelineSelections findPipelineSelectionsById(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return findPipelineSelectionsById(Long.parseLong(id));
    }

    public PipelineSelections findPipelineSelectionsByUserId(Long userId) {
        if (userId == null) {
            return null;
        }

        return transactionTemplate.execute(status ->
                (PipelineSelections) sessionFactory.getCurrentSession()
                        .createQuery("from PipelineSelections where userId = :userId")
                        .setParameter("userId", userId)
                        .setCacheable(true)
                        .uniqueResult());

    }
}
