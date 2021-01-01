/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class PluginSqlMapDaoTest {

    private PluginSqlMapDao pluginSqlMapDao;

    @BeforeEach
    void setUp() {
        pluginSqlMapDao = new PluginSqlMapDao(mock(SessionFactory.class), null, null);
    }

    @Test
    void shouldGenerateCacheKeyForPluginId() {
        Assertions.assertThat(pluginSqlMapDao.cacheKeyForPluginSettings("cd.go.docker"))
                .isEqualTo("com.thoughtworks.go.server.dao.PluginSqlMapDao.$plugin_settings.$cd.go.docker");
    }
}
