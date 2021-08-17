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

import com.thoughtworks.go.domain.EnvironmentVariable;
import com.thoughtworks.go.domain.EnvironmentVariables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.thoughtworks.go.domain.EnvironmentVariableType.Job;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class EnvironmentVariableSqlMapDaoIntegrationTest {
    @Autowired private EnvironmentVariableSqlMapDao dao;
    @Autowired private DatabaseAccessHelper dbHelper;

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSavePlainTextEnvironmentVariable() {
        EnvironmentVariables variables = new EnvironmentVariables();
        String plainText = "plainText";
        String key = "key";
        variables.add(new EnvironmentVariable(key, plainText, false));
        dao.save(1L, Job, variables);

        EnvironmentVariables actual = dao.load(1L, Job);

        assertThat(actual.get(0).getName(), is(key));
        assertThat(actual.get(0).getValue(), is(plainText));
        assertThat(actual.get(0).isSecure(), is(false));
    }

    @Test
    public void shouldSaveSecureEnvironmentVariable() {
        EnvironmentVariables variables = new EnvironmentVariables();
        String plainText = "plainText";
        String key = "key";
        variables.add(new EnvironmentVariable(key, plainText, true));
        dao.save(1L, Job, variables);

        EnvironmentVariables actual = dao.load(1L, Job);

        assertThat(actual.get(0).getName(), is(key));
        assertThat(actual.get(0).getValue(), is(plainText));
        assertThat(actual.get(0).isSecure(), is(true));
    }

    @Test
    public void shouldDeleteEnvironmentVariable() throws Exception {
        EnvironmentVariables variables = new EnvironmentVariables();
        String plainText = "plainText";
        String key = "key";
        variables.add(new EnvironmentVariable(key, plainText, false));
        dao.save(1L, Job, variables);

        EnvironmentVariables variableFromDb = dao.load(1L, Job);
        assertThat(variableFromDb.size(), is(1));

        dao.deleteAll(variableFromDb);

        variableFromDb = dao.load(1L, Job);
        assertThat(variableFromDb.size(), is(0));
    }

}
