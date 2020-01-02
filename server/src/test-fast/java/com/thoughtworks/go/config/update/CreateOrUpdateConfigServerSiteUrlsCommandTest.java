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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.SiteUrls;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateOrUpdateConfigServerSiteUrlsCommandTest {
    private BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();

    @Test
    void shouldAddBackupConfig() throws Exception {
        SiteUrls siteUrls = new SiteUrls(new SiteUrl("http://foo"), new SecureSiteUrl("https://bar"));
        CreateOrUpdateConfigServerSiteUrlsCommand command = new CreateOrUpdateConfigServerSiteUrlsCommand(siteUrls);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().getSiteUrls()).isEqualTo(siteUrls);
    }

    @Nested
    class Validate {
        @Test
        void shouldReturnTrueForValidSiteUrls() {
            SiteUrls siteUrls = new SiteUrls(new SiteUrl("http://foo.bar"), new SecureSiteUrl("https://foo.bar"));
            cruiseConfig.server().setSiteUrl(siteUrls.getSiteUrl().toString());
            cruiseConfig.server().setSecureSiteUrl(siteUrls.getSecureSiteUrl().toString());
            CreateOrUpdateConfigServerSiteUrlsCommand command = new CreateOrUpdateConfigServerSiteUrlsCommand(siteUrls);

            assertTrue(command.isValid(cruiseConfig));
        }

        @Test
        void shouldReturnFalseForInvalidSiteUrls() {
            SiteUrls siteUrls = new SiteUrls(new SiteUrl("htp://foo.bar"), new SecureSiteUrl("http://foo.bar"));
            cruiseConfig.server().setSiteUrl(siteUrls.getSiteUrl().toString());
            cruiseConfig.server().setSecureSiteUrl(siteUrls.getSecureSiteUrl().toString());
            CreateOrUpdateConfigServerSiteUrlsCommand command = new CreateOrUpdateConfigServerSiteUrlsCommand(siteUrls);

            assertFalse(command.isValid(cruiseConfig));
        }
    }
}
