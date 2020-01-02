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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.dao.DbMetadataDao;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SystemServiceTest {
    private SystemService systemService;
    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void setUp() {
        systemService = new SystemService(null);
    }

    @Test
    public void shouldReturnFalseWhenPathIsNull() {
        assertFalse(systemService.isAbsolutePath(null));
    }

    @Test
    public void shouldRetrieveJvmVersion() {
        assertNotNull(systemService.getJvmVersion());
    }

    @Test
    public void shouldRetrieveOsInfo() {
        assertNotNull(systemService.getOsInfo());
    }

    @Test
    public void shouldPopulateServerDetailsModel() {
        final DbMetadataDao dao = mock(DbMetadataDao.class);
        when(dao.getSchemaVersion()).thenReturn(20);

        new SystemEnvironment().setProperty("java.version", "1.5");
        new SystemEnvironment().setProperty("os.name", "Linux");
        new SystemEnvironment().setProperty("os.version", "2.6");

        Map<String, Object> model = new HashMap<>();

        new SystemService(dao).populateServerDetailsModel(model);

        assertThat(model.get("jvm_version"), is("1.5"));
        assertThat(model.get("os_info"), is("Linux 2.6"));
        assertThat(model.get("schema_version"), is(20));
    }

}
