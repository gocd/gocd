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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.domain.cctray.CcTrayCache;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs;
import static com.thoughtworks.go.util.DataStructureUtils.s;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CcTrayServiceTest {
    @Mock
    private CcTrayCache ccTrayCache;
    @Mock
    private GoConfigService goConfigService;

    private CcTrayService ccTrayService;

    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ccTrayService = new CcTrayService(ccTrayCache, goConfigService);
    }

    @Test
    public void shouldGenerateCcTrayXMLForAnyUserWhenSecurityIsDisabled() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(false);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user1")));
        loginAs("other_user");

        String xml = ccTrayService.renderCCTrayXML("some-prefix", Username.ANONYMOUS.getUsername().toString(), new StringBuilder(), etag -> {
        }).toString();

        assertCcTrayXmlFor(xml, "some-prefix", "proj1", "proj2");
    }

    @Test
    public void shouldGenerateCcTrayXMLForCurrentUserWhenSecurityIsEnabled() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user2")));

        loginAs("USER1");
        String xml = ccTrayService.renderCCTrayXML("some-prefix", "USER1", new StringBuilder(), etag -> {
        }).toString();
        assertCcTrayXmlFor(xml, "some-prefix", "proj1");

        loginAs("uSEr2");
        xml = ccTrayService.renderCCTrayXML("some-prefix", "uSEr2", new StringBuilder(), etag -> {
        }).toString();
        assertCcTrayXmlFor(xml, "some-prefix", "proj2");
    }

    @Test
    public void shouldGenerateEmptyCcTrayXMLWhenCurrentUserIsNotAuthorizedToViewAnyProjects() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user2")));

        loginAs("some-user-without-permissions");
        String xml = ccTrayService.renderCCTrayXML("some-prefix", "some-user-without-permissions", new StringBuilder(), etag -> {
        }).toString();
        assertCcTrayXmlFor(xml, "some-prefix");
    }

    @Test
    public void shouldAllowSiteURLPrefixToBeChangedPerCall() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), statusFor("proj2", "user2")));

        loginAs("user1");
        String xml = ccTrayService.renderCCTrayXML("prefix1", "user1", new StringBuilder(), etag -> {
        }).toString();
        assertCcTrayXmlFor(xml, "prefix1", "proj1");

        loginAs("user2");
        xml = ccTrayService.renderCCTrayXML("prefix2", "user2", new StringBuilder(), etag -> {
        }).toString();
        assertCcTrayXmlFor(xml, "prefix2", "proj2");
    }

    @Test
    public void shouldNotAppendNewLinesForNullProjectStatusesInList() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), new ProjectStatus.NullProjectStatus("proj1").updateViewers(viewers("user1"))));

        loginAs("user1");
        String xml = ccTrayService.renderCCTrayXML("prefix1", "user1", new StringBuilder(), etag -> {
        }).toString();

        assertThat(xml).isEqualTo("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<Projects>\n" +
                "  <Project name=\"proj1\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"" + "prefix1" + "/web-url\" />\n" +
                "</Projects>");
    }

    @Test
    public void shouldChangeEtagIfSitePrefixChanges() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder()).thenReturn(asList(statusFor("proj1", "user1"), new ProjectStatus.NullProjectStatus("proj1").updateViewers(viewers("user1"))));

        AtomicReference<String> originalEtag = new AtomicReference<>();
        String originalXML = ccTrayService.renderCCTrayXML("prefix1", "user1", new StringBuilder(), etag -> {
            originalEtag.set(etag);
        }).toString();

        AtomicReference<String> newEtag = new AtomicReference<>();
        String newXML = ccTrayService.renderCCTrayXML("prefix2", "user1", new StringBuilder(), etag -> {
            newEtag.set(etag);
        }).toString();

        assertThat(originalEtag.get()).isNotEqualTo(newEtag.get());
        assertThat(originalXML).isNotEqualTo(newXML);
    }

    @Test
    public void shouldChangeEtagIfProjectStatusChanges() throws Exception {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(ccTrayCache.allEntriesInOrder())
                .thenReturn(asList(statusFor("proj1", "user1"), new ProjectStatus.NullProjectStatus("proj1").updateViewers(viewers("user1"))))
                .thenReturn(asList(statusFor("proj2", "user1"), new ProjectStatus.NullProjectStatus("proj1").updateViewers(viewers("user1"))));

        AtomicReference<String> originalEtag = new AtomicReference<>();
        String originalXML = ccTrayService.renderCCTrayXML("prefix1", "user1", new StringBuilder(), etag -> {
            originalEtag.set(etag);
        }).toString();


        AtomicReference<String> newEtag = new AtomicReference<>();
        String newXML = ccTrayService.renderCCTrayXML("prefix1", "user1", new StringBuilder(), etag -> {
            newEtag.set(etag);
        }).toString();

        assertThat(originalEtag.get()).isNotEqualTo(newEtag.get());
        assertThat(originalXML).isNotEqualTo(newXML);
    }

    private ProjectStatus statusFor(String projectName, String... allowedUsers) throws Exception {
        ProjectStatus status = new ProjectStatus(projectName, "activity1", "build-status-1", "build-label-1", DateUtils.parseRFC822("Sun, 23 May 2010 10:00:00 +0200"), "web-url");
        status.updateViewers(viewers(allowedUsers));
        return status;
    }

    private void assertCcTrayXmlFor(String actualXml, final String siteUrlPrefix, final String... projects) {
        StringBuilder expectedXml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<Projects>\n");
        for (String project : projects) {
            expectedXml.append("  <Project name=\"").append(project).append("\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"" + siteUrlPrefix + "/web-url\" />\n");
        }
        expectedXml.append("</Projects>");

        assertThat(actualXml).isEqualTo(expectedXml.toString());
    }

    private Users viewers(String... users) {
        return new AllowedUsers(s(users), Collections.emptySet());
    }
}
