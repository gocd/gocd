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

package com.thoughtworks.go.server.security;

import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.util.GoConfigFileHelper;
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
        CONFIG_HELPER.addSecurityWithPasswordFile();
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
