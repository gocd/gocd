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

import java.net.URI;
import java.net.URISyntaxException;

import com.thoughtworks.go.config.ConfigAttributeValue;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

@ConfigAttributeValue(fieldName = "url")
public class UrlArgument extends CommandArgument {
    protected String url;

    public UrlArgument(String url) {
        bombIfNull(url, "null url");
        this.url = url;
    }

    public String forCommandline() {
        return url;
    }

    public String forDisplay() {
        try {
            URI uri = new URI(sanitizeUrl());
            if (uri.getUserInfo() != null) {
                //(String scheme, String userInfo, String host, int port, String path, String query, String fragment)
                uri = new URI(uri.getScheme(), clean(uri.getUserInfo()), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            }
            return uriToDisplay(uri);
        } catch (URISyntaxException e) {
            // In subversion we may have a file path that is not actually a URL
            return url;
        }
    }

    protected String uriToDisplay(URI uri) {
        return uri.toString();
    }

    protected String sanitizeUrl() {
        return url;
    }

    protected String hostInfoForDisplay() {
        try {
            URI uri = new URI(url);
            if (uri.getUserInfo()!=null) {
                //(String scheme, String userInfo, String host, int port, String path, String query, String fragment)
                uri = new URI(uri.getScheme(), clean(uri.getUserInfo()), uri.getHost(), uri.getPort(), null, null, null);
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            // In subversion we may have a file path that is not actually a URL
            return url;
        }
    }

    protected String hostInfoForCommandline() {
        try {
            URI uri = new URI(url);
            if (uri.getUserInfo()!=null) {
                //(String scheme, String userInfo, String host, int port, String path, String query, String fragment)
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            // In subversion we may have a file path that is not actually a URL
            return url;
        }
    }

    private String clean(String userInfo) {
        String[] userAndPassword = userInfo.split(":");
        StringBuilder result = new StringBuilder();
        result.append(userAndPassword[0]);
        if (userAndPassword.length>1) {
            result.append(":");
            result.append("******");
        }
        return result.toString();
    }

    public static UrlArgument create(String url) {
        return new UrlArgument(url);
    }

    public String replaceSecretInfo(String line) {
        if(forCommandline().length() > 0){
            line = line.replace(hostInfoForCommandline(), hostInfoForDisplay());
        }

        return line;
    }

    @Override
    public boolean equal(CommandArgument that) {
        //BUG #3276 - on windows svn info includes a password in svn+ssh
        if (url.startsWith("svn+ssh")) {
            return this.forDisplay().equals(that.forDisplay());
        }
        return cleanPath(this).equals(cleanPath(that));
    }

    private String cleanPath(CommandArgument commandArgument) {
        String path = commandArgument.forCommandline();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
