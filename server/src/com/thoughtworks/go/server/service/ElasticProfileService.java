/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ElasticProfileService {

    private final GoConfigService goConfigService;

    @Autowired
    public ElasticProfileService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public ElasticProfile findProfile(String id) {
        ElasticProfiles profiles = goConfigService.serverConfig().getElasticConfig().getProfiles();
        return profiles.find(id);
    }

    public Map<String, ElasticProfile> allProfiles() {
        ElasticProfiles profiles = goConfigService.serverConfig().getElasticConfig().getProfiles();

        HashMap<String, ElasticProfile> result = new HashMap<>();
        for (ElasticProfile profile : profiles) {
            result.put(profile.getId(), new ElasticProfile(profile));
        }

        return result;
    }
}
