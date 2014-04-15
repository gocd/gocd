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

package com.thoughtworks.go.server.controller.beans;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class MaterialFactory {

    public MaterialConfig getMaterial(String scm, String paramUrl, String paramUsername, String paramPassword, Boolean useTickets, String view, String branch, String projectPath, String domain) {
        if ("svn".equals(scm) || StringUtils.isBlank(scm)) {
            return new SvnMaterialConfig(paramUrl, paramUsername, paramPassword, false);
        }
        if ("hg".equals(scm)) {
            return new HgMaterialConfig(paramUrl, null);
        }
        if ("git".equals(scm)) {
            return new GitMaterialConfig(paramUrl, branch);
        }
        if ("p4".equals(scm)) {
            if (StringUtils.isBlank(view)) {
                bomb("View is required for perforce");
            }
            P4MaterialConfig p4 = new P4MaterialConfig(paramUrl, view, paramUsername);
            p4.setPassword(paramPassword);
            p4.setUseTickets(BooleanUtils.toBoolean(useTickets));
            return p4;
        }
        if ("tfs".equals(scm)) {
            TfsMaterialConfig tfsMaterial = new TfsMaterialConfig(new GoCipher(), new UrlArgument(paramUrl), paramUsername, valueOrEmptyString(domain), paramPassword, projectPath);
            return tfsMaterial;
        }
        throw new RuntimeException("Invalid scm: " + scm);
    }

    private String valueOrEmptyString(String value) {
        return value == null ? "" : value;
    }
}
