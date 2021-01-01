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
package com.thoughtworks.go.server.service.permissions;

import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionsService {
    private List<PermissionProvider> providers = new ArrayList<>();

    @Autowired
    public PermissionsService(PermissionProvider... availableProviders) {
        providers.addAll(Arrays.asList(availableProviders));
    }

    public List<String> allEntitiesSupportsPermission() {
        return providers.stream().map(PermissionProvider::name).collect(Collectors.toList());
    }

    public Map<String, Object> getPermissions(List<String> requestedTypes) {
        Map<String, Object> json = new LinkedHashMap<>();

        providers.stream().filter(p -> requestedTypes.contains(p.name()))
                .forEach(p -> json.put(p.name(), p.permissions(SessionUtils.currentUsername())));

        return json;
    }
}
