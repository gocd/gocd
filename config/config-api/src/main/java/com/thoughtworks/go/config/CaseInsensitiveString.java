/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConfigAttributeValue(fieldName = "name", createForNull = false)
public class CaseInsensitiveString implements Comparable<CaseInsensitiveString>, Serializable {

    private final String name;
    private final String lowerCaseName;//used only for comparison

    public CaseInsensitiveString(String name){
        this.name = name;
        this.lowerCaseName = name == null ? null : name.toLowerCase();
    }

    @Override public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CaseInsensitiveString)) {
            return false;
        }

        CaseInsensitiveString that = (CaseInsensitiveString) o;

        if (name != null ? !toLower().equals(that.toLower()) : that.name != null) {
            return false;
        }

        return true;
    }

    public String toLower(){
        return lowerCaseName;
    }

    @Override
    public int hashCode() {
        return name != null ? toLower().hashCode() : 0;
    }

    public boolean isBlank() {
        return StringUtils.isEmpty(name);
    }

    @Override protected Object clone() throws CloneNotSupportedException {
        return new CaseInsensitiveString(name);
    }

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

    public static List<CaseInsensitiveString> caseInsensitiveStrings(List<String> roles) {
        return roles.stream().map(new Function<String, CaseInsensitiveString>() {
            @Override
            public CaseInsensitiveString apply(String obj) {
                return new CaseInsensitiveString(obj);
            }
        }).collect(Collectors.toList());
    }

    public static List<CaseInsensitiveString> caseInsensitiveStrings(String... roles) {
        return caseInsensitiveStrings(Arrays.asList(roles));
    }
}
