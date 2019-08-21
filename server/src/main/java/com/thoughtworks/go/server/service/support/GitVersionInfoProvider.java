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

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.domain.materials.git.GitVersion;
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.CommandLine;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GitVersionInfoProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 7.5;
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        String gitVersionString = CommandLine.createCommandLine("git").withEncoding("UTF-8").withArgs("version").runOrBomb(new NamedProcessTag("git version check")).outputAsString();
        json.put("Version", GitVersion.parse(gitVersionString).getVersion());
        return json;
    }

    @Override
    public String name() {
        return "Git Version Information";
    }
}
