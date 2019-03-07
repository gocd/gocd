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
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

public class SecretParam implements Serializable {
    private String secretConfigId;
    private String key;
    private String value;

    public SecretParam(String secretConfigId, String key) {
        this.secretConfigId = secretConfigId;
        this.key = key;
    }

    public SecretParam(String secretParam) {

    }

    public static List<SecretParam> parse(String str) {
        return null;
    }

    public static boolean isSecretParam(String secretParam) {
        return false;
    }

    public String getSecretConfigId() {
        return secretConfigId;
    }

    public String getKey() {
        return key;
    }

    public String toSecretParam() {
        return format("#{SECRET[%s][%s]}", secretConfigId, key);
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

    public void setValue(String value) {
        this.value = value;
    }
}
