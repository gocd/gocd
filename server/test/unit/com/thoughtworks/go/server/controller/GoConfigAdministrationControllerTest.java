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

package com.thoughtworks.go.server.controller;

import java.util.Map;

import com.thoughtworks.go.config.GoMailSender;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.controller.beans.GoMailSenderProvider;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.licensing.Edition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoConfigAdministrationControllerTest {
    private GoMailSender sender;
    private GoConfigAdministrationController controller;
    private GoMailSenderProvider provider;
    private GoConfigService goConfigService;
    private SecurityService securityService;

    @Before
    public void setUp() {
        sender = mock(GoMailSender.class);
        provider = mock(GoMailSenderProvider.class);
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        controller = new GoConfigAdministrationController(provider, goConfigService, securityService);
    }

    @Test
    public void shouldReportValidIfMailSentSuccessfully() {
        when(provider.createSender(new MailHost("smtp.company.com", 25, "smtpuser", "password", true, true, "cruise@me.com", "jez@me.com"))).thenReturn(sender);

        when(sender.send(any(String.class), any(String.class), eq("jez@me.com"))).thenReturn(ValidationBean.valid());

        ModelAndView json = controller.sendTestEmailToAdministrator("smtp.company.com", "25", "smtpuser", "password", true, "cruise@me.com", "jez@me.com", new MockHttpServletResponse());
        Map map = json.getModel();
        JsonMap jsonMap = (JsonMap) map.get("json");
        assertThat(jsonMap.get("isValid").toString(), is("\"true\""));
        verify(provider).createSender(new MailHost("smtp.company.com", 25, "smtpuser", "password", true, true, "cruise@me.com", "jez@me.com"));
        verify(sender).send(any(String.class), any(String.class), eq("jez@me.com"));
    }


    @Test
    public void shouldLoadSpecificConfigVersionWhenHistoricalVersionIsRequested() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String configXml = "config-content";

        when(goConfigService.getConfigAtVersion("some-md5")).thenReturn(new GoConfigRevision(configXml, "some-md5", "loser", "100.9.3.1", Edition.Enterprise, new TimeProvider()));

        controller.getConfigRevision("some-md5", response);

        assertThat(response.getContentAsString(), is(configXml));
        assertThat(response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5).toString(), is("some-md5"));
    }
}
