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
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.domain.materials.Modification;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeXml11;

public class GitModificationParser {
    private Yaml yaml = new Yaml();

    public List<Modification> parse(String output) {
        if (StringUtils.isBlank(output)) {
            return Collections.emptyList();
        }

        List<GitLog> gitLogs = yaml.load(escapeXml11(output));

        if (gitLogs.isEmpty()) {
            return Collections.emptyList();
        }

        return gitLogs.stream().map(GitLog::toModification).collect(Collectors.toList());
    }
}
