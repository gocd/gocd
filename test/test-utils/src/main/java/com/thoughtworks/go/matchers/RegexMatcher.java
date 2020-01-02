/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.matchers;

import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class RegexMatcher extends TypeSafeMatcher<String> {
    private final String regex;

    public RegexMatcher(String regex){
        this.regex = regex;
    }

    @Override
    public boolean matchesSafely(String item) {
        return Pattern.compile(regex, Pattern.DOTALL).matcher(item).find();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches regex=" + regex);
    }

    public static RegexMatcher matches(String regex){
        return new RegexMatcher(regex);
    }
}
