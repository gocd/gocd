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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ConfigAttributeValue;

import java.net.URI;
import java.net.URISyntaxException;

@ConfigAttributeValue(fieldName = "url")
public class ServerSiteUrlConfig {
    private static final String HTTPS_URL_REGEX = "^https://.+";
    protected String url;

    public ServerSiteUrlConfig() {
    }

    public ServerSiteUrlConfig(String url) {
        this.url = url;
    }

    public boolean hasNonNullUrl() {
        return getUrl() != null;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o.getClass().equals(this.getClass()))) {
            return false;
        }

        ServerSiteUrlConfig that = (ServerSiteUrlConfig) o;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }

    public String siteUrlFor(String givenUrl) throws URISyntaxException {
        return siteUrlFor(givenUrl, false);
    }

    public String siteUrlFor(String givenUrl, boolean honorGivenHostName) throws URISyntaxException {
        if (url == null || isPath(givenUrl)) {
            return givenUrl; //it is a path
        }

        URI baseUri = new URI(url);
        URI givenUri = new URI(givenUrl);

        return new URI(baseUri.getScheme(), getOrDefault(givenUri, baseUri, new Getter() {
            public String get(URI uri) {
                return uri.getUserInfo();
            }
        }), honorGivenHostName ? givenUri.getHost() : baseUri.getHost(), baseUri.getPort(), getOrDefault(givenUri, baseUri, new Getter() {
            public String get(URI uri) {
                return uri.getPath();
            }
        }), getOrDefault(givenUri, baseUri, new Getter() {
            public String get(URI uri) {
                return uri.getQuery();
            }
        }), getOrDefault(givenUri, baseUri, new Getter() {
            public String get(URI uri) {
                return uri.getFragment();
            }
        })).toString();
    }

    private boolean isPath(String givenUrl) {
        return !givenUrl.matches("^https?://.+");
    }

    private String getOrDefault(URI givenUri, URI baseUri, Getter getter) {
        String given = getter.get(givenUri);
        return given == null ? getter.get(baseUri) : given;
    }

    public boolean isAHttpsUrl() {
        return url != null && url.matches( HTTPS_URL_REGEX);
    }

    interface Getter {
        String get(URI uri);
    }

    @Override
    public String toString() {
        return hasNonNullUrl() ? url: "";
    }
}
