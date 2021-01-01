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
package com.thoughtworks.go.plugin.domain.authorization;

public class Capabilities {
    private final SupportedAuthType supportedAuthType;
    private final boolean canSearch;
    private final boolean canAuthorize;
    private final boolean canGetUserRoles;

    public Capabilities(SupportedAuthType supportedAuthType, boolean canSearch, boolean canAuthorize, boolean canGetUserRoles) {
        this.supportedAuthType = supportedAuthType;
        this.canSearch = canSearch;
        this.canAuthorize = canAuthorize;
        this.canGetUserRoles = canGetUserRoles;
    }

    public boolean canGetUserRoles() {
        return canGetUserRoles;
    }

    public SupportedAuthType getSupportedAuthType() {
        return supportedAuthType;
    }

    public boolean canSearch() {
        return canSearch;
    }

    public boolean canAuthorize() {
        return canAuthorize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Capabilities that = (Capabilities) o;

        if (canSearch != that.canSearch) return false;
        if (canAuthorize != that.canAuthorize) return false;
        if (canGetUserRoles != that.canGetUserRoles) return false;
        return supportedAuthType == that.supportedAuthType;
    }

    @Override
    public int hashCode() {
        int result = supportedAuthType != null ? supportedAuthType.hashCode() : 0;
        result = 31 * result + (canSearch ? 1 : 0);
        result = 31 * result + (canAuthorize ? 1 : 0);
        result = 31 * result + (canGetUserRoles ? 1 : 0);
        return result;
    }
}
