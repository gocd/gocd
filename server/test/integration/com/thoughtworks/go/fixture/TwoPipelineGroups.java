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

package com.thoughtworks.go.fixture;

import java.util.UUID;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;

public class TwoPipelineGroups implements PreCondition {
    private GoConfigFileHelper configHelper;
    private TestRepo svnTestRepo;
    private boolean isSetup = false;

    public TwoPipelineGroups(GoConfigFileHelper configHelper) {
        this.configHelper = configHelper;
    }

    public void onSetUp() throws Exception {
        this.isSetup = true;
        configHelper.initializeConfigFile();

        svnTestRepo = new SvnTestRepo("testsvnrepo");
        SvnCommand svnCommand = new SvnCommand(null, svnTestRepo.projectRepositoryUrl());

        configHelper.addPipelineWithGroup("group1", "pipeline_" + UUID.randomUUID(), svnCommand, "defaultStage",
                "defaultJob");
        configHelper.addPipelineWithGroup("group2", "pipeline_" + UUID.randomUUID(), svnCommand, "defaultStage",
                "defaultJob");
    }

    public void onTearDown() throws Exception {
        if (isSetup) {
            configHelper.initializeConfigFile();
            svnTestRepo.tearDown();
        }
    }

    public String pipelineInFirstGroup() {
        return CaseInsensitiveString.str(configHelper.currentConfig().getGroups().first().first().name());
    }

    public String pipelineInSecondGroup() {
        return CaseInsensitiveString.str(configHelper.currentConfig().getGroups().get(1).first().name());
    }
}
