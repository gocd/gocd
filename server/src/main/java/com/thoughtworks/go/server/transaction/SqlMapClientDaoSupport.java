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
package com.thoughtworks.go.server.transaction;

import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.InitializingBean;

public abstract class SqlMapClientDaoSupport implements InitializingBean {
    protected final GoCache goCache;
    private final SqlSessionFactory sqlSessionFactory;
    private SqlMapClientTemplate sqlSession;
    private SystemEnvironment systemEnvironment;
    private Database database;

    public SqlMapClientDaoSupport(GoCache goCache, SqlSessionFactory sqlSessionFactory, SystemEnvironment systemEnvironment, Database database) {
        this.goCache = goCache;
        this.sqlSessionFactory = sqlSessionFactory;
        this.systemEnvironment = systemEnvironment;
        this.database = database;
    }

    public SqlMapClientTemplate getSqlMapClientTemplate() {
        if (this.sqlSession == null) {
            this.sqlSession = new SqlMapClientTemplate(goCache, sqlSessionFactory);
        }
        return sqlSession;
    }

    @Override
    public void afterPropertiesSet() throws IllegalArgumentException {
        getSqlMapClientTemplate();
    }

    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlSession) {
        this.sqlSession = sqlSession;
    }

}
