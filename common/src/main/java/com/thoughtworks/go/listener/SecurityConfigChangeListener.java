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
package com.thoughtworks.go.listener;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;

import java.util.Arrays;
import java.util.List;

public abstract class SecurityConfigChangeListener extends EntityConfigChangedListener<Object> {
    private final List<Class<?>> securityConfigClasses = Arrays.asList(
            SecurityAuthConfig.class,
            Role.class,
            AdminsConfig.class,
            RolesConfig.class
    );

    @Override
    public boolean shouldCareAbout(Object entity) {
        return securityConfigClasses.stream().anyMatch(aClass -> aClass.isAssignableFrom(entity.getClass()));
    }
}
