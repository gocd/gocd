/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ConfigValue;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public abstract class ServerSiteUrlConfig {
    private static final Pattern HTTPS_URL_REGEX = Pattern.compile("^https://.+");
    @ConfigValue
    protected String url;

    public ServerSiteUrlConfig() {}

    public ServerSiteUrlConfig(String url) {
        this.url = url;
    }

    public boolean isBlank() {
        return StringUtils.isBlank(url);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerSiteUrlConfig that = (ServerSiteUrlConfig) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }

    public Optional<URL> withPath(String absolutePathQueryFragment) throws URISyntaxException, MalformedURLException {
        if (isBlank()) {
            return Optional.empty();
        }

        URI baseUri = new URI(url);
        URI givenUri = new URI(absolutePathQueryFragment);

        return Optional.of(new URI(
            baseUri.getScheme(),
            null,
            baseUri.getHost(),
            baseUri.getPort(),
            givenUri.getPath(),
            givenUri.getQuery(),
            givenUri.getFragment()
        ).toURL());
    }

    public boolean isHttps() {
        return !isBlank() && HTTPS_URL_REGEX.matcher(url).matches();
    }

    @Override
    public String toString() {
        return isBlank() ? "" : url;
    }
}

