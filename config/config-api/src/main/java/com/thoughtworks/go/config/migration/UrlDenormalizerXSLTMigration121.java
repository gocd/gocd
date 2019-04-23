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

package com.thoughtworks.go.config.migration;

import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class UrlDenormalizerXSLTMigration121 {

    public static String urlWithoutCredentials(String originalUrl) {
        try {
            if (isSupportedUrl(originalUrl)) {
                String[] credentials = getCredentials(originalUrl);
                if (credentials != null) {

                    URI url = new URI(originalUrl);
                    return new URI(url.getScheme(), null, url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getFragment()).toString();
                }
            }
            return originalUrl;
        } catch (URISyntaxException e) {
            return originalUrl;
        }
    }

    public static String urlWithCredentials(String urlWithoutCredentials, String username, String password) {
        try {
            if (isSupportedUrl(urlWithoutCredentials)) {
                String credentials = "";
                // intentionally not checking `blank` whitespace is still a valid (though unlikely) username/password
                if (username != null && !username.equals("")) {
                    credentials += username;
                }

                if (password != null && !password.equals("")) {
                    credentials += ":" + password;
                }

                URI url = new URI(urlWithoutCredentials);

                return new URI(url.getScheme(), credentials.equals("") ? null : credentials, url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getFragment()).toString();
            }
            return urlWithoutCredentials;
        } catch (URISyntaxException e) {
            return urlWithoutCredentials;
        }
    }

    public static String getUsername(String originalUrl) {
        try {
            if (isSupportedUrl(originalUrl)) {
                String[] credentials = getCredentials(originalUrl);
                if (credentials != null) {
                    if ("".equals(credentials[0])) {
                        return null;
                    } else {
                        return credentials[0];
                    }
                }
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static String getPassword(String originalUrl) {
        try {
            if (isSupportedUrl(originalUrl)) {
                String[] credentials = getCredentials(originalUrl);
                if (credentials != null && credentials.length >= 2) {
                    if ("".equals(credentials[1])) {
                        return null;
                    } else {
                        return credentials[1];
                    }
                }
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isSupportedUrl(String originalUrl) throws URISyntaxException {
        if (isNotBlank(originalUrl) && (originalUrl.startsWith("http") || originalUrl.startsWith("https"))) {
            new URI(originalUrl);
            return true;
        }

        return false;
    }

    private static String[] getCredentials(String originalUrl) throws URISyntaxException {

        String userInfo = new URI(originalUrl).getUserInfo();
        if (isNotBlank(userInfo)) {
            return userInfo.split(":", 2);
        }
        return null;
    }

}
