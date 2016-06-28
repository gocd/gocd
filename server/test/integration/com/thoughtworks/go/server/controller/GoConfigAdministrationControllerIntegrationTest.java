/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayOutputStream;

import static com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH;
import static javax.servlet.http.HttpServletResponse.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/spring-tabs-servlet.xml",
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoConfigAdministrationControllerIntegrationTest {
    @Autowired private GoConfigAdministrationController controller;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoFileConfigDataSource dataSource;
    @Autowired private ConfigElementImplementationRegistry registry;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private MockHttpServletResponse response;

    private static final String NEW_STAGE =
            "<stage name=\"dev\">\n"
                    + "  <jobs>\n"
                    + "    <job name=\"build1\" />\n"
                    + "    <job name=\"build2\" />\n"
                    + "  </jobs>\n"
                    + "</stage>";

    private static final String NEW_PIPELINE =
            "<pipeline name=\"cruise\">\n"
                    + "  <materials>\n"
                    + "    <svn url=\"file:///tmp/foo\" checkexternals=\"true\" />\n"
                    + "  </materials>\n"
                    + "  <stage name=\"dev\">\n"
                    + "    <jobs>\n"
                    + "      <job name=\"linux\" />\n"
                    + "      <job name=\"windows\" />\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>";

    private static final String NEW_GROUP =
            "<pipelines group=\"group\">\n"
                    + "  <pipeline name=\"pipeline\">\n"
                    + "    <materials>\n"
                    + "      <svn url=\"file:///tmp/foo\" />\n"
                    + "    </materials>\n"
                    + "    <stage name=\"dev\">\n"
                    + "      <jobs>\n"
                    + "        <job name=\"linux\" />\n"
                    + "        <job name=\"windows\" />\n"
                    + "      </jobs>\n"
                    + "    </stage>\n"
                    + "  </pipeline>\n"
                    + "</pipelines>";

    private static final String NEW_BUILD =
            "        <job name=\"new_job\" />\n";

    private static final String NEW_PIPELINE_TEMPLATE =
            "<pipeline name=\"cruise\">\n"
                    + "  <stage name=\"dev\">\n"
                    + "    <jobs>\n"
                    + "      <job name=\"linux\" />\n"
                    + "      <job name=\"windows\" />\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>";

    private String groupName;
    private SecurityContext originalSecurityContext;

    @Before public void setup() throws Exception {
        dataSource.reloadEveryTime();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        response = new MockHttpServletResponse();
        configHelper.addSecurityWithPasswordFile();
        configHelper.addAdmins("admin");
        originalSecurityContext = SecurityContextHolder.getContext();
        setCurrentUser("admin");
    }

    @After public void teardown() throws Exception {
        if (originalSecurityContext != null) {
            SecurityContextHolder.setContext(originalSecurityContext);
        }
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

    private void setCurrentUser(String username) {
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new User(username, "", true, new GrantedAuthority[]{}), null));
        SecurityContextHolder.setContext(context);
    }

    private void assertValidContentAndStatus(int status, String contentType, String content) throws Exception {
        RestfulActionTestHelper.assertValidContentAndStatus(response, status, contentType, content);
    }

}
