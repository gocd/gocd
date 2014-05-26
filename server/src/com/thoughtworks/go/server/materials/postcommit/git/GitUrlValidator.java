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

package com.thoughtworks.go.server.materials.postcommit.git;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUrlValidator {

    private final ArrayList<GitValidator> validators;

    public GitUrlValidator() {
        validators = new ArrayList<GitValidator>();
        validators.add(new GitUrlWithFullAuthValidator());
        validators.add(new GitUrlWithUserNameAuthValidator());
        validators.add(new GitUrlWithNoAuthValidator());
    }

    public boolean perform(String paramUrl, String materialUrl) {
        for (GitValidator validator : validators) {
            if (validator.isValid(paramUrl, materialUrl)) {
                return true;
            }
        }
        return false;
    }
}

interface GitValidator {
    public boolean isValid(String paramUrl, String materialUrl);
}

class GitUrlWithFullAuthValidator implements GitValidator {

    private static final Pattern URL_WITH_FULL_AUTH_PATTERN = Pattern.compile("^(.+?//)(.+?):(.+?)@(.+)$");

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        Matcher fullAuthMatcher = URL_WITH_FULL_AUTH_PATTERN.matcher(materialUrl);
        if (fullAuthMatcher.matches()) {
            String protocolField = fullAuthMatcher.group(1);
            String urlField = fullAuthMatcher.group(4);
            return String.format("%s%s", protocolField, urlField).equalsIgnoreCase(paramRepoUrl);
        }
        return false;
    }
}

class GitUrlWithUserNameAuthValidator implements GitValidator {

    private static final Pattern URL_WITH_USERNAME_AUTH_PATTERN = Pattern.compile("^(.+?//)(.+?):@(.+)$");

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        Matcher userNameAuthMatcher = URL_WITH_USERNAME_AUTH_PATTERN.matcher(materialUrl);
        if (userNameAuthMatcher.matches()) {
            String protocolField = userNameAuthMatcher.group(1);
            String urlField = userNameAuthMatcher.group(3);
            return String.format("%s%s", protocolField, urlField).equalsIgnoreCase(paramRepoUrl);
        }
        return false;
    }
}

class GitUrlWithNoAuthValidator implements GitValidator {

    private static final Pattern URL_WITH_NO_AUTH_PATTERN = Pattern.compile("^(.+?//)(.+)$");

    @Override
    public boolean isValid(String paramRepoUrl, String materialUrl) {
        Matcher urlMatcher = URL_WITH_NO_AUTH_PATTERN.matcher(materialUrl);
        if (urlMatcher.matches()) {
            String protocolField = urlMatcher.group(1);
            String urlField = urlMatcher.group(2);
            return String.format("%s%s", protocolField, urlField).equalsIgnoreCase(paramRepoUrl);
        }
        return false;
    }
}
