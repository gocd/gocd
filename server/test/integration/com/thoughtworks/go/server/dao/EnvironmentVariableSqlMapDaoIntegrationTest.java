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

import java.util.Map;

import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.server.dao.EnvironmentVariableDao.EnvironmentVariableType.Job;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class EnvironmentVariableSqlMapDaoIntegrationTest {

    @Autowired private EnvironmentVariableSqlMapDao dao;
    @Autowired private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }


    @Test
    public void shouldSavePlainTextEnvironmentVariable() {
        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        String plainText = "plainText";
        String key = "key";
        variables.add(new EnvironmentVariableConfig(new GoCipher(), key, plainText, false));
        dao.save(1L, Job, variables);

        EnvironmentVariablesConfig actual = dao.load(1L, Job);

        assertThat(actual.get(0).getName(), is(key));
        assertThat(actual.get(0).getValue(), is(plainText));
        assertThat(actual.get(0).isSecure(), is(false));
    }

    @Test
    public void shouldSaveSecureEnvironmentVariable() throws InvalidCipherTextException {
        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        String plainText = "plainText";
        String key = "key";
        GoCipher goCipher = new GoCipher();
        variables.add(new EnvironmentVariableConfig(goCipher, key, plainText, true));
        dao.save(1L, Job, variables);

        EnvironmentVariablesConfig actual = dao.load(1L, Job);

        assertThat(actual.get(0).getName(), is(key));
        assertThat(actual.get(0).getValue(), is(plainText));
        assertThat(actual.get(0).isSecure(), is(true));
    }

    private Map<String, Object> args(long id, EnvironmentVariableDao.EnvironmentVariableType type) {
        return arguments("entityId", id).and("entityType", type).asMap();
    }

}
