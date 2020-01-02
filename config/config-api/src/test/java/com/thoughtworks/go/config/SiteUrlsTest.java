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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class SiteUrlsTest {

    @Test
    void shouldNotAddErrorsIfSiteUrlsAreValid() {
        SiteUrls siteUrls = new SiteUrls(new SiteUrl("http://foo.bar"), new SecureSiteUrl("https://foo.bar"));

        siteUrls.validate(null);

        assertThat(siteUrls.errors())
                .hasSize(0);
    }

    @Test
    void shouldAddErrorsIfSiteUrlIsInvalid() {
        SiteUrls siteUrls = new SiteUrls(new SiteUrl("htp://foo.bar"), new SecureSiteUrl("https://foo.bar"));

        siteUrls.validate(null);

        assertThat(siteUrls.getSiteUrl().errors())
                .hasSize(1)
                .containsEntry("siteUrl", Collections.singletonList("Invalid format for site url. 'htp://foo.bar' must start with http/s"));
    }

    @Test
    void shouldAddErrorsIfSecureSiteUrlIsInvalid() {
        SiteUrls siteUrls = new SiteUrls(new SiteUrl("http://foo.bar"), new SecureSiteUrl("http://foo.bar"));

        siteUrls.validate(null);

        assertThat(siteUrls.getSecureSiteUrl().errors())
                .hasSize(1)
                .containsEntry("secureSiteUrl", Collections.singletonList("Invalid format for secure site url. 'http://foo.bar' must start with https"));
    }

    @Test
    void shouldAddErrorsIfSiteUrlsAreInvalid() {
        SiteUrls siteUrls = new SiteUrls(new SiteUrl("htp://foo.bar"), new SecureSiteUrl("http://foo.bar"));

        siteUrls.validate(null);

        assertThat(siteUrls.getSiteUrl().errors())
                .hasSize(1)
                .containsEntry("siteUrl", Collections.singletonList("Invalid format for site url. 'htp://foo.bar' must start with http/s"));
        assertThat(siteUrls.getSecureSiteUrl().errors())
                .hasSize(1)
                .containsEntry("secureSiteUrl", Collections.singletonList("Invalid format for secure site url. 'http://foo.bar' must start with https"));
    }
}