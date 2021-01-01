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
package com.thoughtworks.go.config;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ConfigAttributeValue(fieldName = "name", createForNull = false)
public class CaseInsensitiveString implements Comparable<CaseInsensitiveString>, Serializable {

    private final String name;
    private final String lowerCaseName; //used only for comparison

    public CaseInsensitiveString(String name) {
        this.name = name;
        this.lowerCaseName = name == null ? null : name.toLowerCase();
    }

    @Override
    public String toString() {
        return name;
    }

    public String toLower() {
        return lowerCaseName;
    }

    public boolean startsWith(String string) {
       return lowerCaseName.startsWith(string.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseInsensitiveString that = (CaseInsensitiveString) o;
        return Objects.equals(lowerCaseName, that.lowerCaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerCaseName);
    }

    public boolean isBlank() {
        return StringUtils.isEmpty(name);
    }

    @Override
    protected Object clone() {
        return new CaseInsensitiveString(name);
    }

    @Override
    public int compareTo(CaseInsensitiveString other) {
        return toLower().compareTo(other.toLower());
    }

    public static boolean isBlank(CaseInsensitiveString string) {
        return string == null || string.isBlank();
    }

    public static boolean areEqual(CaseInsensitiveString one, CaseInsensitiveString other) {
        return one == null ? other == null : one.equals(other);
    }

    public String toUpper() {
        return name.toUpperCase();
    }

    public static String str(CaseInsensitiveString str) {
        return str == null ? null : str.name;
    }

    public static List<CaseInsensitiveString> list(List<String> strings) {
        return toList(strings.stream());
    }

    public static List<CaseInsensitiveString> list(String... strings) {
        return toList(Arrays.stream(strings));
    }

    public static List<String> toStringList(Collection<CaseInsensitiveString> strings) {
        return toStringList(strings.stream());
    }

    public static List<String> toStringList(CaseInsensitiveString... strings) {
        return toStringList(Arrays.stream(strings));
    }

    private static List<CaseInsensitiveString> toList(Stream<String> stream) {
        return stream.map(CaseInsensitiveString::new).collect(Collectors.toList());
    }

    private static List<String> toStringList(Stream<CaseInsensitiveString> stream) {
        return stream.map(CaseInsensitiveString::toString).collect(Collectors.toList());
    }
}
