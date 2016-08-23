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

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.service.ConfigRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ConfigRepositoryProvider implements ServerInfoProvider {

    private final ConfigRepository configRepository;

    @Autowired
    public ConfigRepositoryProvider(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public double priority() {
        return 6.5;
    }

    @Override
    public void appendInformation(InformationStringBuilder infoCollector) {
        infoCollector.addSection(name());
        try {
            infoCollector.append("Number of commits :" + configRepository.commitCountOnMaster()).append("\n");

            infoCollector.addSubSection("GC Statistics");
            infoCollector.append(configRepository.getStatistics().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        try {
            json.put("Number of commits", configRepository.commitCountOnMaster());
            json.put("GC Statistics", configRepository.getStatistics());
        } catch (GitAPIException | IncorrectObjectTypeException | MissingObjectException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Override
    public String name() {
        return "Config Git Repository";
    }
}
