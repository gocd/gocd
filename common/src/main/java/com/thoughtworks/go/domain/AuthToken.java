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

package com.thoughtworks.go.domain;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class AuthToken extends PersistentObject {
    private String name;
    //this is the hashed token value
    private String value;
    private String originalValue;
    private String description;
    private Boolean isRevoked = false;
    private Timestamp revokedAt;
    private Timestamp createdAt;
    private Timestamp lastUsed;
    private String username;
    private String saltId;
    private String saltValue;
    private String authConfigId;

    public AuthToken() {
    }

    public AuthToken(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public AuthToken(String name, String value, String description) {
        this(name, value);
        this.description = description;
    }

    public AuthToken(String name, String value, String description, Boolean isRevoked) {
        this(name, value, description);
        this.isRevoked = isRevoked;
    }

    public AuthToken(String name, String value, String description, Boolean isRevoked, Date createdAt, Date lastUsed) {
        this(name, value, description, isRevoked);
        this.createdAt = new Timestamp(createdAt.getTime());
        setLastUsed(lastUsed);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(Boolean revoked) {
        isRevoked = revoked;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed != null ? new Timestamp(lastUsed.getTime()) : null;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = new Timestamp(createdAt.getTime());
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Timestamp revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getSaltId() {
        return saltId;
    }

    public void setSaltId(String saltId) {
        this.saltId = saltId;
    }

    public String getSaltValue() {
        return saltValue;
    }

    public void setSaltValue(String saltValue) {
        this.saltValue = saltValue;
    }

    public String getAuthConfigId() {
        return authConfigId;
    }

    public void setAuthConfigId(String authConfigId) {
        this.authConfigId = authConfigId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AuthToken authToken = (AuthToken) o;
        return Objects.equals(name, authToken.name) &&
                Objects.equals(value, authToken.value) &&
                Objects.equals(originalValue, authToken.originalValue) &&
                Objects.equals(description, authToken.description) &&
                Objects.equals(isRevoked, authToken.isRevoked) &&
                Objects.equals(revokedAt, authToken.revokedAt) &&
                Objects.equals(createdAt, authToken.createdAt) &&
                Objects.equals(lastUsed, authToken.lastUsed) &&
                Objects.equals(username, authToken.username) &&
                Objects.equals(saltId, authToken.saltId) &&
                Objects.equals(saltValue, authToken.saltValue) &&
                Objects.equals(authConfigId, authToken.authConfigId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, value, originalValue, description, isRevoked, revokedAt, createdAt, lastUsed, username, saltId, saltValue, authConfigId);
    }

    @Override
    public String toString() {
        return "AuthToken{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", originalValue='" + originalValue + '\'' +
                ", description='" + description + '\'' +
                ", isRevoked=" + isRevoked +
                ", revokedAt=" + revokedAt +
                ", createdAt=" + createdAt +
                ", lastUsed=" + lastUsed +
                ", username='" + username + '\'' +
                ", saltId='" + saltId + '\'' +
                ", saltValue='" + saltValue + '\'' +
                ", authConfigId='" + authConfigId + '\'' +
                '}';
    }
}
