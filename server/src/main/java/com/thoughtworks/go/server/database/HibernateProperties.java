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
package com.thoughtworks.go.server.database;

import com.thoughtworks.go.database.Database;
import org.hibernate.cache.EhCacheProvider;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class HibernateProperties extends Properties {
    @Autowired
    public HibernateProperties(Database database) {
        super.put(Environment.DIALECT, database.dialectForHibernate());
        super.put(Environment.CACHE_PROVIDER, EhCacheProvider.class.getName());
        super.put(Environment.USE_QUERY_CACHE, "true");
        super.put(Environment.SHOW_SQL, "false");
    }
}
