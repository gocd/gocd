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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.domain.cctray.CcTrayCache;
import com.thoughtworks.go.domain.cctray.viewers.AllowedViewers;
import com.thoughtworks.go.domain.cctray.viewers.Viewers;
import com.thoughtworks.go.util.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.fixture.IntegrationTestsFixture.login;
import static com.thoughtworks.go.fixture.IntegrationTestsFixture.resetSecurityContext;
import static com.thoughtworks.go.util.DataStructureUtils.s;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CcTrayServiceTest {
    @Mock
    private CcTrayCache ccTrayCache;
    @Mock
    private GoConfigService goConfigService;

    private CcTrayService ccTrayService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ccTrayService = new CcTrayService(ccTrayCache, goConfigService);
    }

    @After
    public void tearDown() throws Exception {
        resetSecurityContext();
    }

    @Test
    public void shouldGenerateCcTrayXMLForAnyUserWhenSecurityIsDisabled() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(false);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user1")));
        login("other_user", "password");

        String xml = ccTrayService.getCcTrayXml("some-prefix");

        assertCcTrayXmlFor(xml, "some-prefix", "proj1", "proj2");
    }

    @Test
    public void shouldGenerateCcTrayXMLForCurrentUserWhenSecurityIsEnabled() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user2")));

        login("USER1", "password");
        String xml = ccTrayService.getCcTrayXml("some-prefix");
        assertCcTrayXmlFor(xml, "some-prefix", "proj1");

        login("uSEr2", "password");
        xml = ccTrayService.getCcTrayXml("some-prefix");
        assertCcTrayXmlFor(xml, "some-prefix", "proj2");
    }

    @Test
    public void shouldGenerateEmptyCcTrayXMLWhenCurrentUserIsNotAuthorizedToViewAnyProjects() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user2")));

        login("some-user-without-permissions", "password");
        String xml = ccTrayService.getCcTrayXml("some-prefix");
        assertCcTrayXmlFor(xml, "some-prefix");
    }

    @Test
    public void shouldAllowSiteURLPrefixToBeChangedPerCall() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user2")));

        login("user1", "password");
        String xml = ccTrayService.getCcTrayXml("prefix1");
        assertCcTrayXmlFor(xml, "prefix1", "proj1");

        login("user2", "password");
        xml = ccTrayService.getCcTrayXml("prefix2");
        assertCcTrayXmlFor(xml, "prefix2", "proj2");
    }

    @Test
    public void shouldNotAppendNewLinesForNullProjectStatusesInList() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), new ProjectStatus.NullProjectStatus("proj1").updateViewers(viewers("user1"))));

        login("user1", "password");
        String xml = ccTrayService.getCcTrayXml("prefix1");

        assertThat(xml, is("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<Projects>\n" +
                "  <Project name=\"proj1\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" lastBuildTime=\"2010-05-23T00:00:00\" webUrl=\"" + "prefix1" + "/web-url\" />\n" +
                "</Projects>"));
    }

    private ProjectStatus statusFor(String projectName, String... allowedUsers) throws Exception {
        ProjectStatus status = new ProjectStatus(projectName, "activity1", "build-status-1", "build-label-1", DateUtils.parseYYYYMMDD("2010-05-23"), "web-url");
        status.updateViewers(viewers(allowedUsers));
        return status;
    }

    private void assertCcTrayXmlFor(String actualXml, final String siteUrlPrefix, final String... projects) {
        StringBuilder expectedXml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<Projects>\n");
        for (String project : projects) {
            expectedXml.append("  <Project name=\"").append(project).append("\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" lastBuildTime=\"2010-05-23T00:00:00\" webUrl=\"" + siteUrlPrefix + "/web-url\" />\n");
        }
        expectedXml.append("</Projects>");

        assertThat(actualXml, is(expectedXml.toString()));
    }

    private Viewers viewers(String... users) {
        return new AllowedViewers(s(users));
    }
}