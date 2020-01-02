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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;

@Component
public class DbMetadataSqlMapDao extends HibernateDaoSupport implements DbMetadataDao {


    @Autowired
    public DbMetadataSqlMapDao(SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
    }

    @Override
    public int getSchemaVersion() throws DataAccessException {

        return (Integer) getHibernateTemplate().execute((HibernateCallback) session -> session.createSQLQuery("select max(change_number) from changelog").uniqueResult());
    }
}
