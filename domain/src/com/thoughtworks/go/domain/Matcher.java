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

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.validation.Validator;
import org.apache.commons.lang.StringUtils;

public class Matcher {
    private LinkedHashSet<String> matchers = new LinkedHashSet<>();
    private static final String SEPARATOR = ",";
    private static final String[] SPECIAL_CHARS = new String[]{"\\", "[", "]", "^", "$", ".", "|", "?", "*", "+", "(", ")"};

    public Matcher(String matcherString) {
        if (StringUtils.isNotEmpty(matcherString)) {
            trimAndAdd(matcherString);
        }
    }

    private void trimAndAdd(String matcherString) {
        for (String part : StringUtils.split(matcherString, SEPARATOR)) {
            this.matchers.add(part.trim());
        }
    }

    public Matcher(String[] array) {
        this(StringUtils.join(array, SEPARATOR));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Matcher matcher = (Matcher) o;
        return matchers.equals(matcher.matchers);
    }

    public int hashCode() {
        return (matchers != null ? matchers.hashCode() : 0);
    }

    public String toString() {
        return StringUtils.join(matchers, ',');
    }

    public List<String> toCollection() {
        return new ArrayList<>(matchers);
    }

    public void validateUsing(Validator<String> validator) throws ValidationException {
        for (String matcher : matchers) {
            ValidationBean validationBean = validator.validate(matcher);
            if (!validationBean.isValid()) {
                throw new ValidationException(validationBean.getError());
            }
        }
    }

    public boolean matches(String comment) {
        for (String escapedMatcher : escapeMatchers()) {
            Pattern pattern = Pattern.compile("\\B" + escapedMatcher + "\\B|\\b" + escapedMatcher + "\\b");
            if (pattern.matcher(comment).find()) {
                return true;
            }
        }
        return false;
    }

    private List<String> escapeMatchers() {
        List<String> escapedMatchers = new ArrayList<>();
        for (String matcher : matchers) {
            for (String specialChar : SPECIAL_CHARS) {
                matcher = StringUtils.replace(matcher, specialChar, "\\" + specialChar);
            }
            escapedMatchers.add(matcher);
        }
        return escapedMatchers;
    }
}
