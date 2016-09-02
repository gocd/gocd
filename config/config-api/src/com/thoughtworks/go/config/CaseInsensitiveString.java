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

package com.thoughtworks.go.config;

import java.io.Serializable;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

@ConfigAttributeValue(fieldName = "name", createForNull = false)
public class CaseInsensitiveString implements Comparable<CaseInsensitiveString>, Serializable {

    private final String name;
    private final String lowerCaseName;//used only for comparison

    public CaseInsensitiveString(String name) {
        this.name = name;
        this.lowerCaseName = StringUtils.lowerCase(name);
    }

    @Override
    public String toString() {
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

        return StringUtils.equals(toLower(), that.toLower());
    }

    public String toLower() {
        return lowerCaseName;
    }

    @Override
    public int hashCode() {
        return toLower() != null ? toLower().hashCode() : 0;
    }

    public boolean isBlank() {
        return StringUtils.isEmpty(name);
    }

    @Override protected Object clone() throws CloneNotSupportedException {
        return new CaseInsensitiveString(name);
    }

    public int compareTo(CaseInsensitiveString other) {
        return ObjectUtils.compare(toLower(), other.toLower());
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
}
