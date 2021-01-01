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
package com.thoughtworks.go.domain.config;

import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigInterface;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.Validatable;

@ConfigInterface
public interface Admin extends Validatable {
    String GO_SYSTEM_ADMIN = "Go System Administrator";
    String NAME = "name";

    boolean isSameAs(Admin admin, List<Role> memberRoles);

    CaseInsensitiveString getName();

    String describe();

    boolean belongsTo(Role role);

    void addError(String message);
}
