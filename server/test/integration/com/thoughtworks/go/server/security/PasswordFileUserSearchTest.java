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

package com.thoughtworks.go.server.security;

import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PasswordFileUserSearchTest {
    @Autowired private PasswordFileUserSearch passwordFileUserSearch;
    @Autowired private GoConfigDao goConfigDao;
    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.initializeConfigFile();
        new SystemEnvironment().set(SystemEnvironment.INBUILT_LDAP_PASSWORD_AUTH_ENABLED, true);
        CONFIG_HELPER.addSecurityWithPasswordFile();
    }

    @After
    public void tearDown() throws Exception {
        new SystemEnvironment().set(SystemEnvironment.INBUILT_LDAP_PASSWORD_AUTH_ENABLED, false);
    }

    @Test
    public void shouldSearchUsersInaPasswordFile() throws Exception {

        List<User> models = passwordFileUserSearch.search("jez");

        assertThat(models.size(), is(1));
        assertThat(models.get(0).getName(), is("jez"));
    }

    @Test
    public void shouldIgnoreCaseWhileSearchingForUsersInaPasswordFile() throws Exception {

        List<User> users = passwordFileUserSearch.search("JEZ");

        assertThat(users.size(), is(1));
        assertThat(users.get(0).getName(), is("jez"));
    }

    @Test
    public void shouldSearchUsersInaPasswordFileBasedOnPartialSearchText() throws Exception {

        List<User> models = passwordFileUserSearch.search("ez");

        assertThat(models.size(), is(1));
        assertThat(models.get(0).getName(), is("jez"));
    }
}
