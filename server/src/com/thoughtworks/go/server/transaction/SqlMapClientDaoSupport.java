/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.transaction;


import java.util.List;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.SqlMapClientCallback;

public class SqlMapClientDaoSupport extends org.springframework.orm.ibatis.support.SqlMapClientDaoSupport {
    public SqlMapClientDaoSupport(GoCache goCache, SqlMapClient sqlMapClient, SystemEnvironment systemEnvironment, Database database) {
        SqlMapClientTemplate sqlMapClientTemplate = new SqlMapClientTemplate(goCache, systemEnvironment, database);
        sqlMapClientTemplate.setSqlMapClient(sqlMapClient);
        setSqlMapClientTemplate(sqlMapClientTemplate);
    }


    public static class SqlMapClientTemplate extends org.springframework.orm.ibatis.SqlMapClientTemplate {
        private final GoCache goCache;

        private final ThreadLocal<Boolean> internalCall = new ThreadLocal<>();
        private final SystemEnvironment systemEnvironment;
        private Database database;

        public SqlMapClientTemplate(GoCache goCache, SystemEnvironment systemEnvironment, Database database) {
            this.systemEnvironment = systemEnvironment;
            this.database = database;
            this.internalCall.set(false);
            this.goCache = goCache;
        }

        private String translateStatementName(String statementName) {
            if (systemEnvironment.isDefaultDbProvider()) {
                return statementName;
            }
            String forExternalDb = String.format("%s-%s", statementName, database.getType());
            MappedStatement statement;
            try {
                statement = ((SqlMapClientImpl) super.getSqlMapClient()).getMappedStatement(forExternalDb);
            } catch (Exception e) {
                statement = null;
            }
            return statement != null ? forExternalDb : statementName;
        }

        @Override public Object insert(final String statementName, final Object parameterObject) throws DataAccessException {
            return executeInternal(new Operation<Object>() {
                public Object execute() {
                    stopServingForTransaction();
                    return SqlMapClientTemplate.super.insert(translateStatementName(statementName), parameterObject);
                }
            });
        }

        @Override public int update(final String statementName, final Object parameterObject) throws DataAccessException {
            return executeInternal(new Operation<Integer>() {
                public Integer execute() {
                    stopServingForTransaction();
                    return SqlMapClientTemplate.super.update(statementName, parameterObject);
                }
            });
        }

        @Override public int delete(final String statementName, final Object parameterObject) throws DataAccessException {
            return executeInternal(new Operation<Integer>() {
                public Integer execute() {
                    stopServingForTransaction();
                    return SqlMapClientTemplate.super.delete(statementName, parameterObject);
                }
            });
        }

        @Override public Object queryForObject(final String statementName, final Object parameterObject) throws DataAccessException {
            return executeInternal(new Operation<Object>() {
                public Object execute() {
                    return SqlMapClientTemplate.super.queryForObject(translateStatementName(statementName), parameterObject);
                }
            });
        }

        @Override public List queryForList(final String statementName, final Object parameterObject) throws DataAccessException {
            return executeInternal(new Operation<List>() {
                public List execute() {
                    return SqlMapClientTemplate.super.queryForList(translateStatementName(statementName), parameterObject);
                }
            });
        }

        @Override public Object execute(SqlMapClientCallback action) throws DataAccessException {
            if (isInternalCall()) {
                return super.execute(action);
            }
            throw new UnsupportedOperationException("Please call one of the supported methods. Refer " + SqlMapClientDaoSupport.SqlMapClientTemplate.class.getCanonicalName() + " for details. This is to ensure read consistency during transactions.");
        }

        private <T> T executeInternal(Operation<T> operation) {
            try {
                internalCall.set(true);
                return operation.execute();
            } finally {
                internalCall.set(false);
            }
        }

        private void stopServingForTransaction() {
            goCache.stopServingForTransaction();
        }

        private boolean isInternalCall() {
            return internalCall.get();
        }

        private interface Operation<T> {
            T execute();
        }
    }



}


