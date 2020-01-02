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

import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SqlMapClientTemplate {
    private GoCache goCache;
    private final org.mybatis.spring.SqlSessionTemplate delegate;
    private final SystemEnvironment systemEnvironment;
    private final Database database;

    private final ConcurrentHashMap<String, String> translatedStatementNames = new ConcurrentHashMap<>();

    public SqlMapClientTemplate(GoCache goCache, SystemEnvironment systemEnvironment, Database database, SqlSessionFactory sqlSessionFactory) {
        this.goCache = goCache;
        this.delegate = new org.mybatis.spring.SqlSessionTemplate(sqlSessionFactory);
        this.systemEnvironment = systemEnvironment;
        this.database = database;
    }

    private String translateStatementName(String statementName) {
        return translatedStatementNames.computeIfAbsent(statementName, statementName1 -> {
            if (systemEnvironment.isDefaultDbProvider()) {
                return statementName1;
            }
            String forExternalDb = String.format("%s-%s", statementName1, database.getType());
            MappedStatement statement;
            try {
                statement = delegate.getConfiguration().getMappedStatement(forExternalDb);
            } catch (Exception e) {
                statement = null;
            }
            return statement != null ? forExternalDb : statementName1;
        });
    }

    public Object queryForObject(String statementName, Object parameter) {
        return delegate.selectOne(translateStatementName(statementName), parameter);
    }

    public List queryForList(String statementName, Object parameter) {
        return delegate.selectList(translateStatementName(statementName), parameter);
    }

    public List queryForList(String statementName) {
        return delegate.selectList(translateStatementName(statementName));
    }

    public void insert(String statementName, Object parameter) {
        goCache.stopServingForTransaction();
        delegate.insert(translateStatementName(statementName), parameter);
    }

    public int update(String statementName, Object parameter) {
        goCache.stopServingForTransaction();
        return delegate.update(translateStatementName(statementName), parameter);
    }

    public void update(String statementName, Object parameter, int requiredRowsAffected) {
        goCache.stopServingForTransaction();
        int actualRowsAffected = delegate.update(statementName, parameter);
        if (actualRowsAffected != requiredRowsAffected) {
            throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(
                    statementName, requiredRowsAffected, actualRowsAffected);
        }
    }

    public void delete(String statementName, Object parameter) {
        goCache.stopServingForTransaction();
        delegate.delete(translateStatementName(statementName), parameter);
    }
}
