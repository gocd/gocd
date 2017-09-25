/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleResult;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.helper.ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoRepoConfigDataSourceIntegrationTest {

    @Autowired
    private ServerHealthService serverHealthService;

    @Autowired
    private GoConfigWatchList configWatchList;

    @Autowired
    private GoConfigPluginService configPluginService;

    @Autowired
    private GoConfigService goConfigService;

    @Autowired
    private CachedGoPartials cachedGoPartials;

    @Autowired
    private GoConfigDao goConfigDao;


    @Before
    public void setUp() throws Exception {
        GoConfigFileHelper configHelper = new GoConfigFileHelper(DEFAULT_XML_WITH_2_AGENTS);
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

        GoRepoConfigDataSource repoConfigDataSource = new GoRepoConfigDataSource(configWatchList, configPluginService, serverHealthService);
        repoConfigDataSource.registerListener(new GoPartialConfig(repoConfigDataSource, configWatchList, goConfigService, cachedGoPartials, serverHealthService));

        configHelper.addTemplate("t1", "param1", "stage");
        File templateConfigRepo = temporaryFolder.newFolder();
        String latestRevision = setupExternalConfigRepo(templateConfigRepo, "external_git_config_repo_referencing_template_with_params");
        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(new GitMaterialConfig(templateConfigRepo.getAbsolutePath()), "gocd-xml");
        configHelper.addConfigRepo(configRepoConfig);

        goConfigService.forceNotifyListeners();
        repoConfigDataSource.onCheckoutComplete(configRepoConfig.getMaterialConfig(), templateConfigRepo, latestRevision);

    }

    @After
    public void tearDown() throws Exception {
        cachedGoPartials.clear();
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void shouldLoadACRPipelineWithParams() throws Exception {
        ParamsConfig paramConfigs = new ParamsConfig(new ParamConfig("foo", "foo"));
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();

        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe-with-params")), is(true));
        assertThat(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe-with-params")).getParams(), is(paramConfigs));
    }

    @Test
    public void shouldLoadACRPipelineReferencingATemplateWithParams() throws Exception {
        ParamsConfig paramConfigs = new ParamsConfig(new ParamConfig("param1", "foo"));
        CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();

        assertThat(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipe-with-template")), is(true));
        assertThat(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe-with-template")).getParams(), is(paramConfigs));
    }

    private String setupExternalConfigRepo(File configRepo, String configRepoTestResource) throws IOException {
        ClassPathResource resource = new ClassPathResource(configRepoTestResource);
        FileUtils.copyDirectory(resource.getFile(), configRepo);
        CommandLine.createCommandLine("git").withArg("init").withArg(configRepo.getAbsolutePath()).runOrBomb("");
        gitAddDotAndCommit(configRepo);
        ConsoleResult consoleResult = CommandLine.createCommandLine("git").withArg("log").withArg("-1").withArg("--pretty=format:%h").withWorkingDir(configRepo).runOrBomb("");

        return consoleResult.outputAsString();
    }

    private void gitAddDotAndCommit(File configRepo) {
        CommandLine.createCommandLine("git").withArg("add").withArg("-A").withArg(".").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withArg("config").withArg("user.email").withArg("go_test@go_test.me").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withArg("config").withArg("user.name").withArg("user").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withArg("commit").withArg("-m").withArg("initial commit").withWorkingDir(configRepo).runOrBomb("");
    }

}