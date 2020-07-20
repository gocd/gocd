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

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.IBatisUtil.arguments;

@Component
public class MaterialSqlMapDao extends SqlMapClientDaoSupport implements MaterialDao {

    @Autowired
    public MaterialSqlMapDao(GoCache goCache, SqlSessionFactory sqlSessionFactory, SystemEnvironment systemEnvironment, Database database) {
        super(goCache, sqlSessionFactory, systemEnvironment, database);
    }

    @Override
    public Modifications getModificationWithMaterial() {
        List<Modification> modifications = getSqlMapClientTemplate().queryForList("materialWithModifications");
        return new Modifications(modifications);
    }
}
