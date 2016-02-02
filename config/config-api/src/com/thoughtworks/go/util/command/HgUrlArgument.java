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

package com.thoughtworks.go.util.command;

import com.thoughtworks.go.config.ConfigAttributeValue;

import java.net.URI;
import java.net.URISyntaxException;

@ConfigAttributeValue(fieldName = "url")
public class HgUrlArgument extends UrlArgument {
    public static final String DOUBLE_HASH = "##";
    private final String SINGLE_HASH = "#";

    public HgUrlArgument(String url) {
        super(url);
    }

    protected String uriToDisplay(URI uri) {
        return uri.toString().replace(SINGLE_HASH, DOUBLE_HASH);
    }

    protected String sanitizeUrl() {
        return url.replace(DOUBLE_HASH, SINGLE_HASH);
    }

    private String removePassword(String userInfo) {
        return userInfo.split(":")[0];
    }

    public String defaultRemoteUrl() {
        final String sanitizedUrl = sanitizeUrl();
        try {
            URI uri = new URI(sanitizedUrl);
            if (uri.getUserInfo() != null) {
                uri = new URI(uri.getScheme(), removePassword(uri.getUserInfo()), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                return uri.toString();
            }
        } catch (URISyntaxException e) {
            return sanitizedUrl;
        }
        return sanitizedUrl;
    }
}
