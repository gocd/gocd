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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeleteConfigRepoCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private ConfigRepoConfig configRepo;
    private String repoId;
    private HttpLocalizedOperationResult result;

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private SecurityService securityService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        repoId = "repo-1";
        configRepo = ConfigRepoConfig.createConfigRepoConfig(git("http://foo.git", "master"), "plugin-id", repoId);
        result = new HttpLocalizedOperationResult();
        cruiseConfig.getConfigRepos().add(configRepo);
    }

    @Test
    public void shouldDeleteTheSpecifiedConfigRepo() throws Exception {
        DeleteConfigRepoCommand command = new DeleteConfigRepoCommand(securityService, repoId, currentUser, result);
        assertNotNull(cruiseConfig.getConfigRepos().getConfigRepo(repoId));
        command.update(cruiseConfig);
        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(repoId));
    }

    @Test
    public void shouldNotContinueWhenConfigRepoNoLongerExists() {
        cruiseConfig.getConfigRepos().remove(0);
        DeleteConfigRepoCommand command = new DeleteConfigRepoCommand(securityService, repoId, currentUser, result);
        assertThat(command.canContinue(cruiseConfig), is(false));
    }
}
