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

package com.thoughtworks.go.server.dao;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.util.IBatisUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bombUnless;
import static com.thoughtworks.go.util.IBatisUtil.arguments;

@Component
public class PropertySqlMapDao extends SqlMapClientDaoSupport implements PropertyDao {
    private static final Logger LOGGER = Logger.getLogger(PropertySqlMapDao.class);

    @Autowired
    public PropertySqlMapDao(GoCache goCache, SqlMapClient sqlMapClient, SystemEnvironment systemEnvironment, Database database) {
        super(goCache, sqlMapClient, systemEnvironment, database);
    }

    public boolean save(long instanceId, Property property) {
        ensureExists(instanceId);
        String propertyName = property.getKey();
        try {
            getSqlMapClientTemplate().insert("saveProperty",
                    arguments("instanceId", instanceId)
                            .and("propertyName", propertyName)
                            .and("value", property.getValue()).asMap()
            );
            return true;
        } catch (DataAccessException e) {
            String message = "Error saving property '" + propertyName + "' = '" + property.getValue() + "' on instanceId '" + instanceId + "'";
            LOGGER.error(message, e);
            LOGGER.debug(message, e);
            return false;
        }
    }

    private void ensureExists(Long instanceId) {
        Boolean exists = (Boolean) getSqlMapClientTemplate().queryForObject("buildInstanceExists", instanceId);
        bombUnless(exists, "No instance '" + instanceId + "' found to set property");
    }

    public String value(long instanceId, String propertyName) {
        Map<String, Object> toGet = new HashMap<>();
        toGet.put("instanceId", instanceId);
        toGet.put("propertyName", propertyName);
        return (String) getSqlMapClientTemplate().queryForObject("getProperty", toGet);
    }

    @SuppressWarnings("unchecked")
    public Properties list(long buildId) {
        List<Property> list = getSqlMapClientTemplate()
                .queryForList("getAllPropertiesByBuildInstanceId", buildId);
        return new Properties(list);
    }

    public List<Properties> loadHistory(String pipelineName, String stageName, String jobName, Long maxPipelineId,
                                        Integer limitCount) {
        IBatisUtil.IBatisArgument arguments = arguments("pipeline", pipelineName)
                .and("stage", stageName)
                .and("build", jobName)
                .and("limitCount", limitCount);
        if (maxPipelineId != null) {
            arguments.and("pipelineId", maxPipelineId);
        }
        return groupByPipelineId(flatHistory(arguments));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> flatHistory(IBatisUtil.IBatisArgument arguments) {
        List<Long> pipelineIds = getSqlMapClientTemplate().queryForList("limitedPipelineIds", arguments.asMap());
        arguments.and("limitedPipelineIds", pipelineIds);

        long maxId = Collections.max(pipelineIds);
        long minId = Collections.min(pipelineIds);
        arguments = arguments.and("maxId", maxId).and("minId", minId);

        return (List<Map<String, Object>>) getSqlMapClientTemplate()
                .queryForList("getAllPropertiesHistory", arguments.asMap());
    }


    static List<Properties> groupByPipelineId(List<Map<String, Object>> flatHistory) {
        LinkedHashMap<String, Properties> propHistory = new LinkedHashMap<>();

        for (Map<String, Object> flatMap : flatHistory) {
            addToHistory(propHistory, sanitize(flatMap));
        }
        return new ArrayList<>(propHistory.values());
    }

    private static Map<String, Object> sanitize(Map<String, Object> flatMap) {
        HashMap<String, Object> santizedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            santizedMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return santizedMap;
    }

    private static void addToHistory(LinkedHashMap<String, Properties> propHistory, Map<String, Object> flatMap) {
        String id = String.valueOf(flatMap.get("pipelineid"));
        String key = (String) flatMap.get("key");
        String value = (String) flatMap.get("value");

        if (!propHistory.containsKey(id)) {
            propHistory.put(id, new Properties());
        }
        propHistory.get(id).add(new Property(key, value));
    }

}
