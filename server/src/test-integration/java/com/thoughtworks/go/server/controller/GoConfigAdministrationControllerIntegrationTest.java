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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.GoFileConfigDataSource;
import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;

import java.io.ByteArrayOutputStream;

import static com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml"
})
public class GoConfigAdministrationControllerIntegrationTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Autowired private GoConfigAdministrationController controller;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoFileConfigDataSource dataSource;
    @Autowired private ConfigElementImplementationRegistry registry;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private MockHttpServletResponse response;

    @Before public void setup() throws Exception {
        dataSource.reloadEveryTime();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        response = new MockHttpServletResponse();
        configHelper.enableSecurity();
        configHelper.addAdmins("admin");
        SessionUtilsHelper.loginAs("admin");
    }

    @After public void teardown() throws Exception {
        configHelper.onTearDown();
    }

    @Test public void shouldGetConfigAsXml() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");

        controller.getCurrentConfigXml(null, response);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(new ConfigCache(), registry).write(goConfigDao.loadForEditing(), os, true);
        assertValidContentAndStatus(SC_OK, "text/xml", os.toString());
        assertThat(response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5), is(goConfigDao.md5OfConfigFile()));
    }

    @Test public void shouldConflictWhenGivenMd5IsDifferent() throws Exception {
        configHelper.addPipeline("pipeline", "stage", "build1", "build2");

        controller.getCurrentConfigXml("crapy_md5", response);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).write(goConfigDao.loadForEditing(), os, true);
        assertValidContentAndStatus(SC_CONFLICT, "text/plain; charset=utf-8", CONFIG_CHANGED_PLEASE_REFRESH);
        assertThat(response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5), is(goConfigDao.md5OfConfigFile()));
    }

    private void assertValidContentAndStatus(int status, String contentType, String content) throws Exception {
        RestfulActionTestHelper.assertValidContentAndStatus(response, status, contentType, content);
    }

}
