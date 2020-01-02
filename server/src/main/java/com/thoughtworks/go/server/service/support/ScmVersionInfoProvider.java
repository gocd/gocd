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
package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.CommandLine;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class ScmVersionInfoProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 7.5;
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        String gitVersion = getGitVersion();
        if (isNotBlank(gitVersion)) {
            json.put("Git Version", gitVersion);
        }
        String hgVersion = getHgVersion();
        if (isNotBlank(hgVersion)) {
            json.put("Hg Version", hgVersion);
        }
        String svnVersion = getSvnVersion();
        if (isNotBlank(svnVersion)) {
            json.put("Svn Version", svnVersion);
        }
        String p4Version = getP4Version();
        if(isNotBlank(p4Version)) {
            json.put("P4 Version", p4Version);
        }

        return json;
    }

    private String getGitVersion() {
        try {
            return createCommandLine("git").withEncoding("UTF-8").withArgs("version").runOrBomb(new NamedProcessTag("git version check")).outputAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getHgVersion() {
        try {
            CommandLine hg = createCommandLine("hg").withArgs("version").withEncoding("utf-8");
            String hgOut = hg.runOrBomb(new NamedProcessTag("hg version check")).outputAsString();
            return hgOut;
        } catch (Exception e) {
            return null;
        }
    }

    private String getSvnVersion() {
        try {
            CommandLine svn = createCommandLine("svn").withArgs("--version").withEncoding("utf-8");
            return svn.runOrBomb(new NamedProcessTag("svn version check")).outputAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getP4Version() {
        try {
            CommandLine p4 = createCommandLine("p4").withArgs("-V").withEncoding("utf-8");
            return p4.runOrBomb(new NamedProcessTag("p4 version check")).outputAsString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String name() {
        return "SCM Version Information";
    }
}
