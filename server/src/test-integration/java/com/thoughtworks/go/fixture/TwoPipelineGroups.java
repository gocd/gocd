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
package com.thoughtworks.go.fixture;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.rules.TemporaryFolder;

import java.util.UUID;

public class TwoPipelineGroups implements PreCondition {
    private GoConfigFileHelper configHelper;
    private TestRepo svnTestRepo;
    private boolean isSetup = false;
    private final TemporaryFolder temporaryFolder;

    public TwoPipelineGroups(GoConfigFileHelper configHelper, TemporaryFolder temporaryFolder) {
        this.configHelper = configHelper;
        this.temporaryFolder = temporaryFolder;
    }

    @Override
    public void onSetUp() throws Exception {
        this.isSetup = true;
        configHelper.initializeConfigFile();

        svnTestRepo = new SvnTestRepo(temporaryFolder);
        SvnCommand svnCommand = new SvnCommand(null, svnTestRepo.projectRepositoryUrl());

        configHelper.addPipelineWithGroup("group1", "pipeline_" + UUID.randomUUID(), svnCommand, "defaultStage",
                "defaultJob");
        configHelper.addPipelineWithGroup("group2", "pipeline_" + UUID.randomUUID(), svnCommand, "defaultStage",
                "defaultJob");
    }

    @Override
    public void onTearDown() throws Exception {
        if (isSetup) {
            configHelper.initializeConfigFile();
            svnTestRepo.tearDown();
        }
    }

    public CaseInsensitiveString pipelineInFirstGroup() {
        return configHelper.currentConfig().getGroups().get(0).get(0).name();
    }

    public CaseInsensitiveString pipelineInSecondGroup() {
        return configHelper.currentConfig().getGroups().get(1).get(0).name();
    }
}
