/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class SecretParam implements Serializable {
    private static final Pattern pattern = Pattern.compile("(?:#\\{SECRET\\[(.*?)\\]\\[(.*?)\\]})+");
    private String secretConfigId;
    private String key;
    private String value;

    public SecretParam(String secretConfigId, String key) {
        this.secretConfigId = secretConfigId;
        this.key = key;
    }

    public static List<SecretParam> parse(String str) {
        final List<SecretParam> secretParams = new ArrayList<>();

        if(isBlank(str)) {
            return secretParams;
        }

        final Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            secretParams.add(new SecretParam(matcher.group(1), matcher.group(2)));
        }

        return secretParams;
    }

    public String getSecretConfigId() {
        return secretConfigId;
    }

    public String getKey() {
        return key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecretParam that = (SecretParam) o;
        return Objects.equals(secretConfigId, that.secretConfigId) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretConfigId, key);
    }
}
