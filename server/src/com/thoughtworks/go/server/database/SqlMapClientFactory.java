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

package com.thoughtworks.go.server.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static com.thoughtworks.go.util.ArrayUtil.addToArray;
import static com.thoughtworks.go.util.StringUtil.isBlank;

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
        if (isBlank(ibatisConfigXmlLocation)) {
            return configLocations;
        }
        return addToArray(configLocations, new ClassPathResource(ibatisConfigXmlLocation));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }
}
