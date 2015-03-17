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

package com.thoughtworks.go.server.web;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.Request;
import org.mortbay.util.UrlEncoded;

import static java.net.URLEncoder.encode;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class RedirectDuringBackupTest {

    private BackupStatusProvider provider;
    private RedirectDuringBackup redirectDuringBackup;
    private static final String BACKUP_STARTED_AT = "Some old date";
    private static final String BACKUP_STARTED_BY = "admin";

    @Before
    public void setUp() {
        provider = mock(BackupStatusProvider.class);
        this.redirectDuringBackup = new RedirectDuringBackup() {
            @Override protected BackupStatusProvider getBackupStatusProvider(HttpServletRequest req) {
                return provider;
            }
        };
    }

    @Test
    public void shouldNotRedirectWhenBackupIsNotBeingTaken() {
        when(provider.isBackingUp()).thenReturn(false);
        Request request = request("get", "", "/go/agents");

        redirectDuringBackup.setServerBackupFlag(request);

        assertThat((String) request.getAttribute("backupInProgress"), is(String.valueOf(false)));
        assertThat(request.getAttribute("redirected_from"), is(nullValue()));
    }

    @DataPoint public static ResponseAssertion GET = new ResponseAssertion("get", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion POST = new ResponseAssertion("post", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents?order=DESC&col=status");
    @DataPoint public static ResponseAssertion PUT = new ResponseAssertion("put", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents?order=DESC&col=status");
    @DataPoint public static ResponseAssertion DELETE = new ResponseAssertion("delete", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents?order=DESC&col=status");
    @DataPoint public static ResponseAssertion HEAD = new ResponseAssertion("head", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion OPTIONS = new ResponseAssertion("options", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion CONNECT = new ResponseAssertion("connect", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion TRACE = new ResponseAssertion("trace", "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion NO_REFERER = new ResponseAssertion("get", null, "go/agents/edit", "go/agents/edit");

    @Theory
    public void shouldRedirectWhenBackupIsInProgress(ResponseAssertion responseAssertion) throws UnsupportedEncodingException {
        when(provider.isBackingUp()).thenReturn(true);
        when(provider.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(provider.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);

        Request request = request(responseAssertion.method, responseAssertion.referer, responseAssertion.uri);
        
        redirectDuringBackup.setServerBackupFlag(request);

        assertThat((String) request.getAttribute("backupInProgress"), is("true"));
        assertThat((String) request.getAttribute("redirected_from"), is(encode(responseAssertion.expectedRedirectedFrom, "UTF-8")));
    }

    @Theory
    public void shouldRedirectWhenBackupIsInProgressWithParamsForBackupStartedAtAndBackupStartedBy(ResponseAssertion responseAssertion) throws UnsupportedEncodingException {
        when(provider.isBackingUp()).thenReturn(true);
        when(provider.backupRunningSinceISO8601()).thenReturn(BACKUP_STARTED_AT);
        when(provider.backupStartedBy()).thenReturn(BACKUP_STARTED_BY);

        Request request = request(responseAssertion.method, responseAssertion.referer, responseAssertion.uri);

        redirectDuringBackup.setServerBackupFlag(request);

        assertThat((String) request.getAttribute("backupInProgress"), is("true"));
        assertThat((String) request.getAttribute("redirected_from"), is(encode(responseAssertion.expectedRedirectedFrom, "UTF-8")));
        assertThat((String) request.getAttribute("backup_started_at"), is(UrlEncoded.encodeString(responseAssertion.backupStartedAt)));
        assertThat((String) request.getAttribute("backup_started_by"), is(responseAssertion.backupStartedBy));
    }

    private Request request(String method, final String referer, String uri) {
        Request request = new Request() {
            @Override public String getHeader(String name) {
                if (name.equals("Referer")) {
                    return referer;
                }
                return super.getHeader(name);
            }
        };
        request.setMethod(method);
        request.setUri(new HttpURI(uri));
        return request;
    }

    private static class ResponseAssertion {
        private String method;
        private String referer;
        private String uri;
        private String expectedRedirectedFrom;
        private String backupStartedAt;
        private String backupStartedBy;

        public ResponseAssertion(String method, String referer, String uri, String expectedRedirectedFrom) {
            this.method = method;
            this.referer = referer;
            this.uri = uri;
            this.expectedRedirectedFrom = expectedRedirectedFrom;
            this.backupStartedAt = BACKUP_STARTED_AT;
            this.backupStartedBy = BACKUP_STARTED_BY;
        }

        @Override public String toString() {
            return "method='" + method + '\'' + ", referer='" + referer + '\'' + ", uri='" + uri + '\'' + ", expectedRedirectedFrom='" + expectedRedirectedFrom + '\'';
        }
    }
}
