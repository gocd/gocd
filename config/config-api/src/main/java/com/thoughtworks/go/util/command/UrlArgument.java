/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util.command;

import com.thoughtworks.go.config.ConfigAttributeValue;
import com.thoughtworks.go.config.SecretParamAware;
import com.thoughtworks.go.config.SecretParams;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

@ConfigAttributeValue(fieldName = "url")
public class UrlArgument extends CommandArgument implements SecretParamAware {
    protected String url;
    private SecretParams secretParams;

    public UrlArgument(String url) {
        bombIfNull(url, "null url");
        this.url = url;
        secretParams = SecretParams.parse(this.url);
    }

    @Override
    public boolean hasSecretParams() {
        return secretParams != null && !secretParams.isEmpty();
    }

    @Override
    public SecretParams getSecretParams() {
        return secretParams;
    }

    @Override
    public String originalArgument() {
        return url;
    }

    @Override
    public String forDisplay() {
        final String maskedUrl = secretParams.mask(sanitizeUrl());
        final UriComponents uriComponents = UriComponentsBuilder.fromUriString(maskedUrl).build();

        try {
            if (uriComponents.getUserInfo() != null) {
                final String userInfo = clean(uriComponents.getScheme(), uriComponents.getUserInfo());
                return new URI(uriComponents.getScheme(), userInfo, uriComponents.getHost(), uriComponents.getPort(), uriComponents.getPath(), uriComponents.getQuery(), uriComponents.getFragment())
                        .toString();
            }
            return maskedUrl;
        } catch (URISyntaxException e) {
            // In subversion we may have a file path that is not actually a URL
            return url;
        }
    }

    @Override
    public String forCommandLine() {
        return secretParams.substitute(this.originalArgument());
    }

    protected String sanitizeUrl() {
        return url;
    }

    protected String hostInfoForDisplay() {
        try {
            URI uri = new URI(forDisplay());
            if (uri.getUserInfo() != null) {
                uri = new URI(uri.getScheme(), clean(uri.getScheme(), uri.getUserInfo()), uri.getHost(), uri.getPort(), null, null, null);
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    protected String hostInfoForCommandline() {
        try {
            URI uri = new URI(forCommandLine());
            if (uri.getUserInfo() != null) {
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    private String clean(String scheme, String userInfo) {
        if (userInfo.contains(":")) {
            return userInfo.replaceFirst(":.*", ":******");
        } else if ("ssh".equals(scheme) || "svn+ssh".equals(scheme)) {
            return userInfo;
        }
        return "******";
    }

    public static UrlArgument create(String url) {
        return new UrlArgument(url);
    }

    public String replaceSecretInfo(String line) {
        if (originalArgument().length() > 0) {
            line = line.replace(hostInfoForCommandline(), hostInfoForDisplay());
        }

        return line;
    }

    @Override
    public boolean equal(CommandArgument that) {
        //BUG #3276 - on windows svn info includes a password in svn+ssh
        if (url.startsWith("svn+ssh")) {
            return this.originalArgument().equals(that.originalArgument());
        }
        return cleanPath(this).equals(cleanPath(that));
    }

    private String cleanPath(CommandArgument commandArgument) {
        String path = commandArgument.originalArgument();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String withoutCredentials() {
        try {
            URI uri = new URI(forDisplay());
            uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            return uri.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    public boolean isValid() {
        String url = this.sanitizeUrl();
        if (this.sanitizeUrl().contains("@")) {
            url = sanitizeUrl().split("@")[1];
        }

        return !SecretParams.parse(url).hasSecretParams();
    }
}
