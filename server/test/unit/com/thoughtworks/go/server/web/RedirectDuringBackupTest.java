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

import com.thoughtworks.go.server.util.ServletHelper;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

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
        ServletHelper.init();
        provider = mock(BackupStatusProvider.class);
        this.redirectDuringBackup = new RedirectDuringBackup() {
            @Override protected BackupStatusProvider getBackupStatusProvider(HttpServletRequest req) {
                return provider;
            }
        };
    }

    @Test
    public void shouldNotRedirectWhenBackupIsNotBeingTaken() throws Exception {
        when(provider.isBackingUp()).thenReturn(false);
        Request request = request(HttpMethod.GET, "", "/go/agents");

        redirectDuringBackup.setServerBackupFlag(request);

        assertThat((String) request.getAttribute("backupInProgress"), is(String.valueOf(false)));
        assertThat(request.getAttribute("redirected_from"), is(nullValue()));
    }

    @DataPoint public static ResponseAssertion GET = new ResponseAssertion(HttpMethod.GET, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion POST = new ResponseAssertion(HttpMethod.POST, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents?order=DESC&col=status");
    @DataPoint public static ResponseAssertion PUT = new ResponseAssertion(HttpMethod.PUT, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents?order=DESC&col=status");
    @DataPoint public static ResponseAssertion DELETE = new ResponseAssertion(HttpMethod.DELETE, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents?order=DESC&col=status");
    @DataPoint public static ResponseAssertion HEAD = new ResponseAssertion(HttpMethod.HEAD, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion OPTIONS = new ResponseAssertion(HttpMethod.OPTIONS, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion CONNECT = new ResponseAssertion(HttpMethod.CONNECT, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion TRACE = new ResponseAssertion(HttpMethod.TRACE, "go/agents?order=DESC&col=status", "go/agents/edit", "go/agents/edit");
    @DataPoint public static ResponseAssertion NO_REFERER = new ResponseAssertion(HttpMethod.GET, null, "go/agents/edit", "go/agents/edit");

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
        assertThat((String) request.getAttribute("backup_started_at"), is(encode(responseAssertion.backupStartedAt, "UTF-8")));
        assertThat((String) request.getAttribute("backup_started_by"), is(responseAssertion.backupStartedBy));
    }

    private Request request(HttpMethod method, final String referer, String uri) {
        Request request = new Request(mock(HttpChannel.class), mock(HttpInput.class)) {
            @Override public String getHeader(String name) {
                if (name.equals("Referer")) {
                    return referer;
                }
                return super.getHeader(name);
            }
        };
		request.setMethod(method, method.asString());
        request.setUri(new HttpURI(uri));
        return request;
    }

    private static class ResponseAssertion {
        private HttpMethod method;
        private String referer;
        private String uri;
        private String expectedRedirectedFrom;
        private String backupStartedAt;
        private String backupStartedBy;

        public ResponseAssertion(HttpMethod method, String referer, String uri, String expectedRedirectedFrom) {
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
