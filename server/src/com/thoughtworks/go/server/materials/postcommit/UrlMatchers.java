/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.materials.postcommit;


import java.util.ArrayList;
import java.util.regex.Pattern;

public class UrlMatchers {

    private final ArrayList<UrlMatcher> validators;

    public UrlMatchers() {
        validators = new ArrayList<>();
        validators.add(new UrlExactMatcher());
        validators.add(new UrlWithFullAuthMatcher());
        validators.add(new UrlWithUserNameAndEmptyPasswordAuthMatcher());
        validators.add(new UrlWithUserNameAndNoPasswordAuthMatcher());
        validators.add(new UrlWithNoAuthMatcher());
    }

    public boolean perform(String paramUrl, String materialUrl) {
        for (UrlMatcher validator : validators) {
            if (validator.isValid(paramUrl, materialUrl)) {
                return true;
            }
        }
        return false;
    }
}

interface UrlMatcher {
    boolean isValid(String paramUrl, String materialUrl);
}

class UrlWithFullAuthMatcher implements UrlMatcher {

    private static final Pattern URL_WITH_FULL_AUTH_PATTERN = Pattern.compile("^(.+?//)(.+?):(.+?)@(.+)$");

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        java.util.regex.Matcher fullAuthMatcher = URL_WITH_FULL_AUTH_PATTERN.matcher(materialUrl);
        if (fullAuthMatcher.matches()) {
            String protocolField = fullAuthMatcher.group(1);
            String urlField = fullAuthMatcher.group(4);
            return String.format("%s%s", protocolField, urlField).equalsIgnoreCase(paramRepoUrl);
        }
        return false;
    }
}

class UrlWithUserNameAndEmptyPasswordAuthMatcher implements UrlMatcher {

    private static final Pattern URL_WITH_USERNAME_AUTH_PATTERN = Pattern.compile("^(.+?//)(.+?):@(.+)$");

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        java.util.regex.Matcher userNameAuthMatcher = URL_WITH_USERNAME_AUTH_PATTERN.matcher(materialUrl);
        if (userNameAuthMatcher.matches()) {
            String protocolField = userNameAuthMatcher.group(1);
            String urlField = userNameAuthMatcher.group(3);
            return String.format("%s%s", protocolField, urlField).equalsIgnoreCase(paramRepoUrl);
        }
        return false;
    }
}

class UrlWithUserNameAndNoPasswordAuthMatcher implements UrlMatcher {

    private static final Pattern URL_WITH_USERNAME_AUTH_PATTERN = Pattern.compile("^(.+?//)(.+?)@(.+)$");

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        java.util.regex.Matcher userNameAuthMatcher = URL_WITH_USERNAME_AUTH_PATTERN.matcher(materialUrl);
        if (userNameAuthMatcher.matches()) {
            String protocolField = userNameAuthMatcher.group(1);
            String urlField = userNameAuthMatcher.group(3);
            return String.format("%s%s", protocolField, urlField).equalsIgnoreCase(paramRepoUrl);
        }
        return false;
    }
}

class UrlWithNoAuthMatcher implements UrlMatcher {

    private static final Pattern URL_WITH_NO_AUTH_PATTERN = Pattern.compile("^(.+?//)(.+)$");

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        java.util.regex.Matcher urlMatcher = URL_WITH_NO_AUTH_PATTERN.matcher(materialUrl);
        if (urlMatcher.matches()) {
            String protocolField = urlMatcher.group(1);
            String urlField = urlMatcher.group(2);
            return String.format("%s%s", protocolField, urlField).equalsIgnoreCase(paramRepoUrl);
        }
        return false;
    }
}

class UrlExactMatcher implements UrlMatcher {

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        return paramRepoUrl.equalsIgnoreCase(materialUrl);
    }
}
