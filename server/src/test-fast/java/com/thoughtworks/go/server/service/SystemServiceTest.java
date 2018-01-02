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

package com.thoughtworks.go.server.service;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.server.dao.DbMetadataDao;
import com.thoughtworks.go.util.SystemEnvironment;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class SystemServiceTest {
    private SystemService systemService;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() {
        systemEnvironment = Mockito.mock(SystemEnvironment.class);
        systemService = new SystemService(null, systemEnvironment);
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
        Mockery mockery = new Mockery();
        final DbMetadataDao dao = mockery.mock(DbMetadataDao.class);
        mockery.checking(new Expectations() {
            {
                one(dao).getSchemaVersion();
                will(returnValue(20));
            }
        });

        new SystemEnvironment().setProperty("java.version", "1.5");
        new SystemEnvironment().setProperty("os.name", "Linux");
        new SystemEnvironment().setProperty("os.version", "2.6");

        Map<String, Object> model = new HashMap<>();

        new SystemService(dao, null).populateServerDetailsModel(model);

        assertThat(model.get("jvm_version"), is("1.5"));
        assertThat(model.get("os_info"), is("Linux 2.6"));
        assertThat(model.get("schema_version"), is(20));
        mockery.assertIsSatisfied();
    }

}
