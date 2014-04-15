/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.dao;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class DbMetadataSqlMapDao extends SqlMapClientDaoSupport implements DbMetadataDao {

    @Autowired
    public DbMetadataSqlMapDao(GoCache goCache, SqlMapClient sqlMapClient, SystemEnvironment systemEnvironment, Database database) {
        super(goCache, sqlMapClient, systemEnvironment, database);
    }

    public int getSchemaVersion() throws DataAccessException {
        return (Integer) getSqlMapClientTemplate().queryForObject("getSchemaVersion");
    }
}
