/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.ui;

import java.util.regex.Pattern;


public class SearchCriteria {
    private static final Pattern PATTERN= Pattern.compile("^\".*\"$");
    private String searchToken;

    public SearchCriteria(String searchToken) {
        if (searchToken == null) throw new IllegalArgumentException("Search token cannot be null");
        this.searchToken = searchToken.toLowerCase().trim();
    }

    public boolean matches(String value) {
        value = value.toLowerCase();
        if(isQuotedString())
            return unQuoteToken().equals(value);
        return value.contains(searchToken);
    }

    private String unQuoteToken() {
        return searchToken.substring(1, searchToken.length() - 1);
    }

    private boolean isQuotedString() {
        return PATTERN.matcher(searchToken).matches();
    }
}
