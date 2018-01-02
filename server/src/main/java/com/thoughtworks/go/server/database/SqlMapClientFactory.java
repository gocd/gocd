/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.database;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class SqlMapClientFactory extends org.springframework.orm.ibatis.SqlMapClientFactoryBean {

    private DatabaseStrategy databaseStrategy;

    @Autowired
    public SqlMapClientFactory(DatabaseStrategy databaseStrategy) {
        this.databaseStrategy = databaseStrategy;
    }

    @Override
    public void setConfigLocation(Resource configLocation) {
        if (configLocation != null) {
            this.setConfigLocations(new Resource[]{configLocation});
        } else {
            super.setConfigLocation(configLocation);
        }
    }

    @Override
    public void setConfigLocations(Resource[] configLocations) {
        super.setConfigLocations(addToConfigLocations(configLocations, databaseStrategy.getIbatisConfigXmlLocation()));
    }

    private Resource[] addToConfigLocations(Resource[] configLocations, String ibatisConfigXmlLocation) {
        if (StringUtils.isBlank(ibatisConfigXmlLocation)) {
            return configLocations;
        }
        return (Resource[]) ArrayUtils.add(configLocations, new ClassPathResource(ibatisConfigXmlLocation));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }
}
