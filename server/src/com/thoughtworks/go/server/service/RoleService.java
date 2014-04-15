/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import java.util.Collections;
import java.util.List;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands: RoleService answers to ...
 */
@Service
public class RoleService {

    private final GoConfigService goConfigService;

    @Autowired
    public RoleService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public List<RoleUser> usersInRole(String roleName) {
        CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
        Role role = cruiseConfig.server().security().roleNamed(roleName);
        if (role != null) {
            return role.getUsers();
        }
        return Collections.emptyList();
    }
}
