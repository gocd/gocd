/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.server.cache.GoCache;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;

import java.util.List;

public class SqlMapClientTemplate {
    private final GoCache goCache;
    private final SqlSessionTemplate delegate;

    public SqlMapClientTemplate(GoCache goCache, SqlSessionFactory sqlSessionFactory) {
        this.goCache = goCache;
        this.delegate = new SqlSessionTemplate(sqlSessionFactory);
    }

    public <T> T queryForObject(String statementName, Object parameter) {
        return delegate.selectOne(statementName, parameter);
    }

    public <T> List<T> queryForList(String statementName, Object parameter) {
        return delegate.selectList(statementName, parameter);
    }

    public <T> List<T> queryForList(String statementName) {
        return delegate.selectList(statementName);
    }

    public void insert(String statementName, Object parameter) {
        goCache.stopServingForTransaction();
        delegate.insert(statementName, parameter);
    }

    public int update(String statementName, Object parameter) {
        goCache.stopServingForTransaction();
        return delegate.update(statementName, parameter);
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
        delegate.delete(statementName, parameter);
    }
}
