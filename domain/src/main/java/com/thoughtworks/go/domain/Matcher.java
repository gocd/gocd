/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Matcher {
    private static final char SEPARATOR = ',';
    private static final String[] SPECIAL_CHARS = new String[]{"\\", "[", "]", "^", "$", ".", "|", "?", "*", "+", "(", ")"};
    private static final String[] SPECIAL_CHAR_REPLACEMENTS = Arrays.stream(SPECIAL_CHARS).map(s -> "\\" + s).toArray(String[]::new);

    @NotNull
    private final String matcherPattern;

    private List<Pattern> patterns;

    public Matcher(String matcherPattern) {
        this.matcherPattern = normalize(matcherPattern);
    }

    public static String normalize(@Nullable String matcherPattern) {
        return matcherPattern == null || matcherPattern.isBlank()
            ? ""
            : matchersFrom(matcherPattern).collect(Collectors.joining(SEPARATOR + ""));
    }

    private static @NotNull Stream<String> matchersFrom(@NotNull String matcherPattern) {
        return Arrays.stream(StringUtils.split(matcherPattern, SEPARATOR)).map(String::trim).filter(s -> !s.isEmpty()).distinct();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Matcher matcher = (Matcher) o;
        return this.matcherPattern.equals(matcher.matcherPattern);
    }

    @Override
    public int hashCode() {
        return matcherPattern.hashCode();
    }

    @Override
    public String toString() {
        return matcherPattern;
    }

    public List<String> toCollection() {
        return matchersFrom(matcherPattern).sorted().collect(Collectors.toList());
    }

    public void validateUsing(Validator<String> validator) throws ValidationException {
        Optional<ValidationBean> result = matchersFrom(matcherPattern)
            .map(validator::validate)
            .filter(v -> !v.isValid())
            .findFirst();

        if (result.isPresent()) {
            throw new ValidationException(result.get().getError());
        }
    }

    public boolean matches(String comment) {
        compilePatternsIfNecessary();
        return patterns.stream().anyMatch(pattern -> pattern.matcher(comment).find());
    }

    private void compilePatternsIfNecessary() {
        if (patterns == null) {
            patterns = escapedMatchers()
                .map(matcher -> Pattern.compile(String.join(matcher, "\\b", "\\b|\\B", "\\B")))
                .collect(Collectors.toList());
        }
    }

    private Stream<String> escapedMatchers() {
        return matchersFrom(matcherPattern).map(matcher -> StringUtils.replaceEach(matcher, SPECIAL_CHARS, SPECIAL_CHAR_REPLACEMENTS));
    }
}
