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
package com.thoughtworks.go.plugin.access.authorization.v2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.util.json.JsonHelper;

import java.util.List;
import java.util.Objects;

class UserDTO {

    @Expose
    @SerializedName("username")
    private String username;
    @Expose
    @SerializedName("display_name")
    private String displayName;
    @Expose
    @SerializedName("email")
    private String emailId;

    public UserDTO(String username, String displayName, String emailId) {
        this.username = username;
        this.displayName = displayName;
        this.emailId = emailId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserDTO user = (UserDTO) o;

        return Objects.equals(displayName, user.displayName) &&
            Objects.equals(emailId, user.emailId) &&
            Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (emailId != null ? emailId.hashCode() : 0);
        return result;
    }

    public static List<UserDTO> fromJSONList(String json) {
        return JsonHelper.fromJsonExposeOnly(json, new TypeToken<List<UserDTO>>() {}.getType());
    }

    public User toDomainModel() {
        return new User(this.username, this.displayName, this.emailId);
    }
}
