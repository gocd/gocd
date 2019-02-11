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

import java.util.Date;
import java.util.Objects;

public class AccessToken extends PersistentObject {
    private String name;
    //this is the hashed token value
    private String value;
    private String originalValue;
    private String description;
    private boolean isRevoked;
    private Date revokedAt;
    private Date createdAt;
    private Date lastUsed;
    private String username;
    private String saltId;
    private String saltValue;
    private String authConfigId;
    private String revokeCause;
    private String revokedBy;

    public AccessToken() {
    }

    public AccessToken(String name, String value, String description, boolean isRevoked, Date createdAt, Date lastUsed) {
        this.name = name;
        this.value = value;
        this.description = description;
        this.isRevoked = isRevoked;
        this.createdAt = createdAt;
        this.lastUsed = lastUsed;
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

    public boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(boolean revoked) {
        isRevoked = revoked;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public Date getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Date revokedAt) {
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

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }

    public String getRevokeCause() {
        return revokeCause;
    }

    public void setRevokeCause(String revokeCause) {
        this.revokeCause = revokeCause;
    }

    public void revoke(String username, String revokeCause, Date revokedAt) {
        setRevoked(true);
        setRevokedBy(username);
        setRevokeCause(revokeCause);
        setRevokedAt(revokedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessToken)) return false;
        if (!super.equals(o)) return false;
        AccessToken that = (AccessToken) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(originalValue, that.originalValue) &&
                Objects.equals(description, that.description) &&
                Objects.equals(isRevoked, that.isRevoked) &&
                Objects.equals(revokedAt, that.revokedAt) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(lastUsed, that.lastUsed) &&
                Objects.equals(username, that.username) &&
                Objects.equals(saltId, that.saltId) &&
                Objects.equals(saltValue, that.saltValue) &&
                Objects.equals(authConfigId, that.authConfigId) &&
                Objects.equals(revokeCause, that.revokeCause) &&
                Objects.equals(revokedBy, that.revokedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, value, originalValue, description, isRevoked, revokedAt, createdAt, lastUsed, username, saltId, saltValue, authConfigId, revokeCause, revokedBy);
    }

    @Override
    public String toString() {
        return "AccessToken{" +
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
                ", revokeCause='" + revokeCause + '\'' +
                ", revokedBy='" + revokedBy + '\'' +
                '}';
    }

}
