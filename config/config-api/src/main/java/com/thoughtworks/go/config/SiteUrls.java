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

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;

import java.util.Objects;

@ConfigTag("siteUrls")
public class SiteUrls implements Validatable {
    @ConfigSubtag
    private SiteUrl siteUrl;

    @ConfigSubtag
    private SecureSiteUrl secureSiteUrl;

    private ConfigErrors errors = new ConfigErrors();

    public SiteUrls() {
        this.siteUrl = new SiteUrl();
        this.secureSiteUrl = new SecureSiteUrl();
    }

    public SiteUrls(SiteUrl serverSiteUrl, SecureSiteUrl secureSiteUrl) {
        this.siteUrl = serverSiteUrl;
        this.secureSiteUrl = secureSiteUrl;
    }

    public SiteUrl getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(SiteUrl siteUrl) {
        this.siteUrl = siteUrl;
    }

    public SecureSiteUrl getSecureSiteUrl() {
        return secureSiteUrl;
    }

    public void setSecureSiteUrl(SecureSiteUrl secureSiteUrl) {
        this.secureSiteUrl = secureSiteUrl;
    }

    public void validate(ValidationContext validationContext) {
        siteUrl.validate(validationContext);
        secureSiteUrl.validate(validationContext);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteUrls siteUrls = (SiteUrls) o;
        return Objects.equals(siteUrl, siteUrls.siteUrl) &&
                Objects.equals(secureSiteUrl, siteUrls.secureSiteUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteUrl, secureSiteUrl);
    }
}
