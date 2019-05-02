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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SimpleUrlArgument extends CommandArgument {
    private final String url;

    public SimpleUrlArgument(String url) {
        bombIfNull(url, "Url cannot be null.");
        this.url = url;
    }

    @Override
    public String originalArgument() {
        throw new UnsupportedOperationException("The method was introduced for fingerprint generation when material has secret params. Now we can remove this now as it is not needed.");
    }

    @Override
    public String forDisplay() {
        if (isBlank(this.url)) {
            return this.url;
        }

        try {
            final URIBuilder uriBuilder = new URIBuilder(this.url);
            final UrlUserInfo urlUserInfo = new UrlUserInfo(uriBuilder.getUserInfo());
            uriBuilder.setUserInfo(urlUserInfo.maskedUserInfo());
            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            return this.url;
        }
    }

    @Override
    public String forCommandLine() {
        return this.url;
    }

    @Override
    public boolean equal(CommandArgument that) {
        //BUG #3276 - on windows svn info includes a password in svn+ssh
        if (url.startsWith("svn+ssh")) {
            return this.forCommandLine().equals(that.forCommandLine());
        }
        return cleanPath(this).equals(cleanPath(that));
    }

    private String cleanPath(CommandArgument commandArgument) {
        String path = commandArgument.forCommandLine();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String replaceSecretInfo(String line) {
        if (StringUtils.isBlank(line)) {
            return line;
        }

        if (isBlank(this.url)) {
            return line;
        }

        try {
            final URIBuilder uriBuilder = new URIBuilder(this.url).setPath(null).setCustomQuery(null).setFragment(null);
            final UrlUserInfo urlUserInfo = new UrlUserInfo(uriBuilder.getUserInfo());
            if (uriBuilder.getUserInfo() != null) {
                line = line.replace(uriBuilder.getUserInfo(), urlUserInfo.maskedUserInfo());
            }
        } catch (URISyntaxException e) {
            //Ignore as url is not according to URI specs
        }

        return line;
    }
}
