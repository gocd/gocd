/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.domain.cctray.viewers.AllowedViewers;
import com.thoughtworks.go.domain.cctray.viewers.Everyone;
import com.thoughtworks.go.domain.cctray.viewers.Viewers;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/* Understands which users can view which pipelines and pipeline groups. */
@Service
public class CcTrayViewAuthority {
    private GoConfigService goConfigService;

    @Autowired
    public CcTrayViewAuthority(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Map<String, Viewers> groupsAndTheirViewers() {
        final Map<String, Viewers> pipelinesAndViewers = new HashMap<>();

        SecurityConfig security = goConfigService.security();
        final Map<String, Set<String>> rolesToUsers = rolesToUsers(security);
        final Set<String> superAdmins = namesOf(security.adminsConfig(), rolesToUsers);

        goConfigService.groups().accept(new PipelineGroupVisitor() {
            @Override
            public void visit(PipelineConfigs pipelineConfigs) {
                if (!pipelineConfigs.hasAuthorizationDefined()) {
                    pipelinesAndViewers.put(pipelineConfigs.getGroup(), Everyone.INSTANCE);
                    return;
                }

                Set<String> pipelineGroupAdmins = namesOf(pipelineConfigs.getAuthorization().getAdminsConfig(), rolesToUsers);
                Set<String> pipelineGroupViewers = namesOf(pipelineConfigs.getAuthorization().getViewConfig(), rolesToUsers);

                Set<String> viewers = new HashSet<>();
                viewers.addAll(superAdmins);
                viewers.addAll(pipelineGroupAdmins);
                viewers.addAll(pipelineGroupViewers);

                pipelinesAndViewers.put(pipelineConfigs.getGroup(), new AllowedViewers(viewers));
            }
        });

        return pipelinesAndViewers;
    }

    private Set<String> namesOf(AdminsConfig adminsConfig, Map<String, Set<String>> rolesToUsers) {
        List<AdminUser> superAdmins = adminsConfig.getUsers();
        Set<String> superAdminNames = new HashSet<>();

        for (AdminUser superAdminUser : superAdmins) {
            superAdminNames.add(superAdminUser.getName().toString());
        }

        for (AdminRole superAdminRole : adminsConfig.getRoles()) {
            superAdminNames.addAll(rolesToUsers.get(superAdminRole.getName().toString()));
        }

        return superAdminNames;
    }

    private Map<String, Set<String>> rolesToUsers(SecurityConfig securityConfig) {
        Map<String, Set<String>> rolesToUsers = new HashMap<>();
        for (Role role : securityConfig.getRoles()) {
            rolesToUsers.put(role.getName().toString(), role.usersOfRole());
        }
        return rolesToUsers;
    }
}
