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

import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.server.util.ServletHelper;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SiteUrlProviderTest {

    private ServerConfigService configService;

    @Before
    public void setUp() throws Exception {
        configService = mock(ServerConfigService.class);
        ServletHelper.init();
    }

    @Test
    public void siteUrl_shouldFetchTheSecureSiteUrl() throws Exception {
        SiteUrlProvider siteUrlProvider = new SiteUrlProvider(configService);
        Request httpRequest = mock(Request.class);
        String requestRootUrl = "http://localhost:8153";

        when(httpRequest.getRootURL()).thenReturn(new StringBuilder(requestRootUrl));
        when(configService.siteUrlFor(requestRootUrl, true)).thenReturn("https://secure_site_url");

        String siteUrl = siteUrlProvider.siteUrl(httpRequest);

        assertThat(siteUrl, is("https://secure_site_url"));
    }

    @Test
    public void siteUrl_shouldFetchSiteUrlInAbsenceOfSecureSiteUrl() throws Exception {
        SiteUrlProvider siteUrlProvider = new SiteUrlProvider(configService);
        Request httpRequest = mock(Request.class);
        String requestRootUrl = "http://localhost:8153";

        when(httpRequest.getRootURL()).thenReturn(new StringBuilder(requestRootUrl));
        when(configService.siteUrlFor(requestRootUrl, true)).thenReturn(requestRootUrl);
        when(configService.siteUrlFor(requestRootUrl, false)).thenReturn("http://site_url");

        String siteUrl = siteUrlProvider.siteUrl(httpRequest);

        assertThat(siteUrl, is("http://site_url"));
    }

    @Test
    public void siteUrl_shouldReturnRequestRootUrlInAbsenceOfSiteUrls() throws Exception {
        SiteUrlProvider siteUrlProvider = new SiteUrlProvider(configService);
        Request httpRequest = mock(Request.class);
        String requestRootUrl = "http://localhost:8153";

        when(httpRequest.getRootURL()).thenReturn(new StringBuilder(requestRootUrl));
        when(configService.siteUrlFor(requestRootUrl, true)).thenReturn(requestRootUrl);
        when(configService.siteUrlFor(requestRootUrl, false)).thenReturn(requestRootUrl);

        String siteUrl = siteUrlProvider.siteUrl(httpRequest);

        assertThat(siteUrl, is(requestRootUrl));
    }
}
