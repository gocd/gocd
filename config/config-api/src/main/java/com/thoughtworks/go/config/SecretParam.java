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

import java.io.Serializable;
import java.util.Objects;

import static java.lang.String.format;

public class SecretParam implements Serializable {
    private String secretConfigId;
    private String key;
    private String value;
    private boolean resolved = false;

    public SecretParam(String secretConfigId, String key) {
        this.secretConfigId = secretConfigId;
        this.key = key;
    }

    public String getSecretConfigId() {
        return secretConfigId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
        this.resolved = true;
    }

    // TODO: Do we need resolved flag or can we rely on value to derive this?
    public boolean isUnresolved() {
        return !resolved;
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

    @Override
    public String toString() {
        return asString();
    }

    public String asString() {
        return format("{{SECRET:[%s][%s]}}", this.secretConfigId, this.key);
    }
}
