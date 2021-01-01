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
package com.thoughtworks.go.server.web;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.SessionTrackingMode;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class WebappSessionConfigIntegrationTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File webapp;

    @Before
    public void setup() throws IOException {
        webapp = temporaryFolder.newFolder();
        File webInf = new File(webapp, "WEB-INF");
        webInf.mkdirs();
        File webXmlForTest = new File(webInf, "web.xml");
        File srcWebXMlFile = new File("src/main/webapp/WEB-INF/web.xml");
        FileUtils.copyFile(srcWebXMlFile, webXmlForTest);
    }

    @Test
    public void shouldSetSessionTrackingModeToCookieOnly() throws Exception {
        Server server = new Server(1234);
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setWar(webapp.getAbsolutePath());
        webAppContext.setContextPath("/");
        server.setHandler(webAppContext);
        try {
            server.start();
            Set<SessionTrackingMode> effectiveSessionTrackingModes = ((WebAppContext) server.getHandlers()[0]).getServletContext().getEffectiveSessionTrackingModes();
            assertThat(effectiveSessionTrackingModes.size(), is(1));
            assertThat(effectiveSessionTrackingModes.contains(SessionTrackingMode.COOKIE), is(true));
        } finally {
            server.stop();
        }
    }
}
