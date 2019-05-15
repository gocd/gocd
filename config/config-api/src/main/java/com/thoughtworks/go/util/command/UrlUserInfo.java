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

package com.thoughtworks.go.util.command;

public class UrlUserInfo {
    private static final String MASKED_VALUE = "******";
    private String username;
    private String password;

    //To handle - BUG #5471
    private boolean possiblyToken;

    public UrlUserInfo(String colonSeparatedCredentials) {
        if (colonSeparatedCredentials != null) {
            final String[] parts = colonSeparatedCredentials.split(":", 2);
            this.username = parts[0];
            this.password = parts.length == 2 ? parts[1] : null;
            this.possiblyToken = (parts.length == 1);
        }
    }

    public UrlUserInfo(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String maskedUserInfo() {
        if (this.username == null && this.password == null) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        if (this.username != null) {
            builder.append(possiblyToken ? MASKED_VALUE : this.username);
        }

        if (this.password != null) {
            builder.append(":").append(MASKED_VALUE);
        }
        return builder.toString();
    }

    public String asString() {
        if (this.username == null && this.password == null) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        if (this.username != null) {
            builder.append(this.username);
        }

        if (this.password != null) {
            builder.append(":").append(this.password);
        }
        return builder.toString();
    }
}