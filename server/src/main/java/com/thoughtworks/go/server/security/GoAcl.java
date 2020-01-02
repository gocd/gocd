/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.security;

import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;

public class GoAcl {
    private List<CaseInsensitiveString> authorizedUsers;

    public GoAcl(List<CaseInsensitiveString> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    public Boolean isGranted(final CaseInsensitiveString userName) {
        return contains(userName);
    }

    private boolean contains(final CaseInsensitiveString userName) {
        for (CaseInsensitiveString authorizedUser : authorizedUsers) {
            if (authorizedUser.equals(userName)) {
                return true;
            }
        }
        return false;
    }
}
