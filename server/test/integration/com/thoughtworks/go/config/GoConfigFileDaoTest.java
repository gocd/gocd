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

package com.thoughtworks.go.config;

import java.io.File;
import java.util.List;

import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG_WITH_ANT_BUILDER;
import static com.thoughtworks.go.helper.ConfigFileFixture.INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS;
import static com.thoughtworks.go.helper.ConfigFileFixture.INVALID_CONFIG_WITH_TYPE_FOR_ARTIFACT;
import static com.thoughtworks.go.helper.ConfigFileFixture.WITH_3_AGENT_CONFIG;
import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.TestUtils.assertContains;
import static com.thoughtworks.go.util.TestUtils.sizeIs;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class GoConfigFileDaoTest {
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private GoConfigFileDao goConfigFileDao = configHelper.getGoConfigFileDao();
    private LogFixture logger;
    private CachedGoConfig cachedGoConfig = configHelper.getCachedGoConfig();

    @Before
    public void setup() throws Exception {
        configHelper.initializeConfigFile();
        logger = LogFixture.startListening();
    }

    @After
    public void teardown() throws Exception {
        logger.stopListening();
    }

    @Test
    public void shouldCreateCruiseConfigFromBasicConfigFile() throws Exception {
        CruiseConfig cruiseConfig = GoConfigFileHelper.load(WITH_3_AGENT_CONFIG);

        assertThat(cruiseConfig, is(notNullValue()));
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        assertThat(pipelineConfig.size(), is(1));
        StageConfig stageConfig = pipelineConfig.get(0);
        assertThat(stageConfig.name(), is(new CaseInsensitiveString("mingle")));
        assertThat(pipelineConfig.materialConfigs(), is(notNullValue()));
        final JobConfig cardList = stageConfig.jobConfigByInstanceName("cardlist", true);
        assertThat(cardList.name(), is(new CaseInsensitiveString("cardlist")));
        assertThat(stageConfig.jobConfigByInstanceName("bluemonkeybutt", true).name(), is(new CaseInsensitiveString("bluemonkeybutt")));

        assertThat(cardList.tasks(), sizeIs(1));
        assertThat(cardList.tasks().first(), instanceOf(NullTask.class));
    }

    @Test
    public void shouldGetAgents() throws Exception {
        CruiseConfig cruiseConfig = GoConfigFileHelper.load(WITH_3_AGENT_CONFIG);

        Agents agents = cruiseConfig.agents();
        assertThat(agents.size(), is(3));
        final AgentConfig approvedAgentConfig = agents.getAgentByUuid("3");
        assertThat(approvedAgentConfig.getHostname(), is("test3.com"));
        assertThat(approvedAgentConfig.getIpAddress(), is("192.168.0.3"));
        assertThat(approvedAgentConfig.getResources().toString(), is("jdk1.4"));

        final AgentConfig deniedAgentConfig = agents.getAgentByUuid("2");
        assertThat(deniedAgentConfig.isDisabled(), is(true));
    }

    @Test
    public void shouldThrowExceptionIfFileIsInvalid() throws Exception {
        try {
            useConfigString("invalid config file");
            goConfigFileDao.load();
            fail("Should have thrown a parse exception");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Content is not allowed in prolog."));
        }
    }


    @Test
    public void shouldGetArtifactsFromBuildPlan() throws Exception {
        CruiseConfig cruiseConfig = GoConfigFileHelper.load(WITH_3_AGENT_CONFIG);

        final ArtifactPlans cardListArtifacts = cruiseConfig.jobConfigByName("pipeline1", "mingle",
                "cardlist", true).artifactPlans();
        assertThat(cardListArtifacts.size(), is(0));
        assertThat(cruiseConfig.jobConfigByName("pipeline1", "mingle", "bluemonkeybutt", true).artifactPlans().size(), is(1));
    }

    @Test
    public void shouldAddAgentToConfigFile() throws Exception {
        Resources resources = new Resources("java");
        AgentConfig approvedAgentConfig = new AgentConfig("uuid", "test1", "192.168.0.1", resources);
        AgentConfig deniedAgentConfig = new AgentConfig("", "test2", "192.168.0.2", resources);
        deniedAgentConfig.disable();
        goConfigFileDao.addAgent(approvedAgentConfig);
        goConfigFileDao.addAgent(deniedAgentConfig);
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.agents().size(), is(2));
        assertThat(cruiseConfig.agents().get(0), is(approvedAgentConfig));
        assertThat(cruiseConfig.agents().get(0).getResources(), is(resources));
        assertThat(cruiseConfig.agents().get(1), is(deniedAgentConfig));
        assertThat(cruiseConfig.agents().get(1).isDisabled(), is(Boolean.TRUE));
        assertThat(cruiseConfig.agents().get(1).getResources(), is(resources));
    }

    @Test
    public void shouldDeleteMultipleAgents() {
        AgentConfig agentConfig1 = new AgentConfig("UUID1", "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig("UUID2", "remote-host2", "50.40.30.22");
        agentConfig1.disable();
        agentConfig2.disable();
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment());
        AgentInstance fromConfigFile2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment());

        GoConfigFileDao.CompositeConfigCommand command = goConfigFileDao.commandForDeletingAgents(fromConfigFile1, fromConfigFile2);

        List<UpdateConfigCommand> commands = command.getCommands();
        assertThat(commands.size(), is(2));
        String uuid1 = (String) ReflectionUtil.getField(commands.get(0), "uuid");
        String uuid2 = (String) ReflectionUtil.getField(commands.get(1), "uuid");
        assertThat(uuid1, is("UUID1"));
        assertThat(uuid2, is("UUID2"));
    }

    @Test
    public void shouldDeleteAgentFromConfigFileGivenUUID() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig("uuid1", "test1", "192.168.0.1");
        AgentConfig agentConfig2 = new AgentConfig("uuid2", "test2", "192.168.0.2");
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment());
        AgentInstance fromConfigFile2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment());

        goConfigFileDao.addAgent(agentConfig1);
        goConfigFileDao.addAgent(agentConfig2);

        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.agents().size(), is(2));

        goConfigFileDao.deleteAgents(fromConfigFile1);

        cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.agents().size(), is(1));
        assertThat(cruiseConfig.agents().get(0), is(agentConfig2));
    }

    @Test
    public void shouldRemoveAgentFromEnvironmentBeforeDeletingAgent() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig("uuid1", "hostname", "127.0.0.1");
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment());

        goConfigFileDao.addAgent(agentConfig1);
        goConfigFileDao.addAgent(new AgentConfig("uuid2", "hostname", "127.0.0.1"));

        EnvironmentConfig env = new EnvironmentConfig(new CaseInsensitiveString("foo-environment"));
        env.addAgent("uuid1");
        env.addAgent("uuid2");
        goConfigFileDao.addEnvironment(env);
        CruiseConfig cruiseConfig = goConfigFileDao.load();

        assertThat(cruiseConfig.getEnvironments().get(0).getAgents().size(), is(2));

        goConfigFileDao.deleteAgents(fromConfigFile1);

        cruiseConfig = goConfigFileDao.load();

        assertThat(cruiseConfig.getEnvironments().get(0).getAgents().size(), is(1));
        assertThat(cruiseConfig.getEnvironments().get(0).getAgents().get(0).getUuid(), is("uuid2"));
    }

    @Test
    public void shouldAddPipelineToConfigFile() throws Exception {
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        int oldsize = cruiseConfig.numberOfPipelines();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring", "ut",
                "www.spring.com");
        goConfigFileDao.addPipeline(pipelineConfig, DEFAULT_GROUP);

        cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.numberOfPipelines(), is(oldsize + 1));
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("spring")), is(pipelineConfig));
    }

    @Test
    public void shouldFailWhenConfigUpdateCannotBeMergedWithLatestRevision() throws Exception {
        final String originalMd5 = goConfigFileDao.load().getMd5();
        goConfigFileDao.updateConfig(configHelper.addPipelineCommand(originalMd5, "p1", "stage1", "build1"));
        final String md5WhenPipelineIsAdded = goConfigFileDao.load().getMd5();
        goConfigFileDao.updateConfig(configHelper.changeJobNameCommand(md5WhenPipelineIsAdded, "p1", "stage1", "build1", "new_build"));

        try {
            goConfigFileDao.updateConfig(new NoOverwriteUpdateConfigCommand() {
                public String unmodifiedMd5() {
                    return md5WhenPipelineIsAdded;
                }

                public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                    deletePipeline(cruiseConfig);
                    return cruiseConfig;
                }

                private void deletePipeline(CruiseConfig cruiseConfig) {
                    cruiseConfig.getGroups().get(0).remove(0);
                }

            });
            fail("should not have allowed no-overwrite stale update");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is(ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH));
        }
    }

    @Test
    public void shouldNotFailNoOverwriteUpdateWhenEditingUnmodifiedCopy() throws Exception {
        final String md5 = goConfigFileDao.md5OfConfigFile();
        try {
            ConfigSaveState configSaveState = goConfigFileDao.updateConfig(new NoOverwriteUpdateConfigCommand() {
                public String unmodifiedMd5() {
                    return md5;
                }

                public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                    cruiseConfig.getEnvironments().add(new EnvironmentConfig(new CaseInsensitiveString("foo")));
                    return cruiseConfig;
                }
            });
            assertThat(configSaveState, is(ConfigSaveState.UPDATED));
        } catch (RuntimeException e) {
            fail("should not have failed for edit on unmodified config.");
        }
    }

    @Test
    public void shouldNotFailUpdateWithOverwritePermittedWhenEditingStaleCopy() throws Exception {
        try {
            goConfigFileDao.updateConfig(new UpdateConfigCommand() {
                public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                    cruiseConfig.getEnvironments().add(new EnvironmentConfig(new CaseInsensitiveString("foo")));
                    return cruiseConfig;
                }
            });
        } catch (RuntimeException e) {
            fail("should not have failed for edit when overwrite allowed.");
        }
    }


    @Test
    public void shouldFeedCloneOfConfigBackToCommand() throws Exception {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), true) {

            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("foo"), "#{bar}-${COUNT}", null, false, new MaterialConfigs(new HgMaterialConfig("url", null)),
                        a(StageConfigMother.custom("stage", "job")));
                pipelineConfig.addParam(new ParamConfig("bar", "baz"));
                cruiseConfig.addPipeline("my-group", pipelineConfig);
                return cruiseConfig;
            }
        };
        goConfigFileDao.updateConfig(command);
        assertThat(command.after.pipelineConfigByName(new CaseInsensitiveString("foo")).getLabelTemplate(), is("baz-${COUNT}"));

        assertThat(command.after.getEnvironments().size(), is(0));
        command.after.addEnvironment("bar");
        assertThat(cachedGoConfig.currentConfig().getEnvironments().size(), is(0));
    }

    @Test
    public void shouldNotUpdateIfCannotContinueIfTheCommandIsPreprocessable() throws Exception {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), false);
        try {
            goConfigFileDao.updateConfig(command);
            fail("should have failed as check returned false");
        } catch (ConfigUpdateCheckFailedException expected) {
        }
        assertThat(command.wasUpdated, is(false));
        assertThat(command.after, not(nullValue()));
    }

    @Test
    public void shouldPerformUpdateIfCanContinue() throws Exception {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), true);
        goConfigFileDao.updateConfig(command);
        assertThat(command.wasUpdated, is(true));
    }

    @Test
    public void shouldBePassedTheLatestCruiseConfigWhileCheckingBeforeUpdate() {
        configHelper.addTemplate("my-template", "my-stage");
        configHelper.addPipeline("pipeline", "stage");
        configHelper.addPipelineWithTemplate(PipelineConfigs.DEFAULT_GROUP, "my-pipeline", "my-template");
        CheckedTestUpdateCommand command = spy(new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), true));
        goConfigFileDao.updateConfig(command);
        verify(command).canContinue(cachedGoConfig.currentConfig());
    }

    @Test
    public void shouldAddEnvironmentToConfigFile() throws Exception {
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        int oldsize = cruiseConfig.getEnvironments().size();
        goConfigFileDao.addEnvironment(new EnvironmentConfig(new CaseInsensitiveString("foo-environment")));

        cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.getEnvironments().size(), is(oldsize + 1));
    }

    @Test
    public void shouldAddPipelineOnTheTopOfSameGroupWhenGivenGroupExist() throws Exception {
        PipelineConfig springConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring",
                "ut", "www.spring.com");
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("mingle",
                "ut", "www.spring.com");

        goConfigFileDao.addPipeline(springConfig, "group1");
        goConfigFileDao.addPipeline(mingleConfig, "group1");

        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.numbersOfPipeline("group1"), is(2));
        assertThat(cruiseConfig.find("group1", 0), is(mingleConfig));
    }

    @Test
    public void shouldAddPipelineToTheNewGroupWhenGivenGroupDoesNotExist() throws Exception {
        PipelineConfig springConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring",
                "ut", "www.spring.com");
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("mingle",
                "ut", "www.spring.com");

        goConfigFileDao.addPipeline(springConfig, "group1");
        goConfigFileDao.addPipeline(mingleConfig, "group");

        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.numbersOfPipeline("group1"), is(1));
        assertThat(cruiseConfig.numbersOfPipeline("group"), is(1));
    }

    @Test
    public void shouldAddPipelineToDefaultGroupWhenNoGroupNameSpecified() throws Exception {
        PipelineConfig springConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring",
                "ut", "www.spring.com");

        goConfigFileDao.addPipeline(springConfig, null);

        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.numbersOfPipeline(DEFAULT_GROUP), is(1));
    }

    @Test
    public void shouldAddPipelineToTheTopOfConfigFile() throws Exception {
        goConfigFileDao.load();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("addedFirst",
                "ut", "www.spring.com");
        PipelineConfig pipelineConfig2 = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("addedSecond",
                "ut", "www.spring.com");
        goConfigFileDao.addPipeline(pipelineConfig, DEFAULT_GROUP);
        goConfigFileDao.addPipeline(pipelineConfig2, DEFAULT_GROUP);

        goConfigFileDao.load();
        final File configFile = new File(goConfigFileDao.fileLocation());
        final String content = FileUtils.readFileToString(configFile);
        final int indexOfSecond = content.indexOf("addedSecond");
        final int indexOfFirst = content.indexOf("addedFirst");
        assertThat(indexOfSecond, is(not(-1)));
        assertThat(indexOfFirst, is(not(-1)));
        assertTrue(indexOfSecond < indexOfFirst);

    }

    @Test
    public void shouldNotAddInvalidPipelineToConfigFile() throws Exception {
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        int oldsize = cruiseConfig.numberOfPipelines();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("", "ut",
                "www.spring.com");
        try {
            goConfigFileDao.addPipeline(pipelineConfig, DEFAULT_GROUP);
            fail();
        } catch (Exception e) {

        }
        cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.numberOfPipelines(), is(oldsize));
    }

    @Test
    public void shouldUpdateAgentResourcesToConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "test", "127.0.0.1", new Resources("java"));
        goConfigFileDao.addAgent(agentConfig);
        Resources newResources = new Resources("firefox");
        goConfigFileDao.updateAgentResources(agentConfig.getUuid(), newResources);
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.agents().get(0).getResources(), is(newResources));
    }

    @Test
    public void shouldUpdateAgentApprovalStatusByUuidToConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "test", "127.0.0.1", new Resources("java"));
        goConfigFileDao.addAgent(agentConfig);
        goConfigFileDao.updateAgentApprovalStatus(agentConfig.getUuid(), Boolean.TRUE);

        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.agents().get(0).isDisabled(), is(true));
    }

    @Test
    public void shouldRemoveAgentResourcesInConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "test", "127.0.0.1", new Resources("java, resource1, resource2"));
        goConfigFileDao.addAgent(agentConfig);
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.agents().get(0).getResources().size(), is(3));
        goConfigFileDao.updateAgentResources(agentConfig.getUuid(), new Resources("java"));
        cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.agents().get(0).getResources().size(), is(1));
    }

    @Test
    public void shouldOverwriteConfigContentAfterSave() throws Exception {
        useConfigString(WITH_3_AGENT_CONFIG);
        cachedGoConfig.save(CONFIG_WITH_ANT_BUILDER, false);
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.jobConfigByName("pipeline1", "mingle", "cardlist", true).tasks().size(), is(1));
    }

    @Test
    public void shouldNotChangeCurrentConfigIfInvalid() throws Exception {
        useConfigString(WITH_3_AGENT_CONFIG);
        CruiseConfig cruiseConfig = goConfigFileDao.load();

        try {
            cachedGoConfig.save("This is invalid Cruise", false);
            fail();
        } catch (Exception e) {

        }
        assertCurrentConfigIs(cruiseConfig);
    }

    @Test
    public void shouldNotAllowTypeForArtifactsBecausePolymorphismIsUsedInstead() throws Exception {
        try {
            cachedGoConfig.save(INVALID_CONFIG_WITH_TYPE_FOR_ARTIFACT, false);
            fail();
        } catch (Exception e) {
            assertContains(e.toString(), "'type' is not allowed");
        }
    }

    @Test
    public void shouldNotAllowOldXml() throws Exception {
        try {
            cachedGoConfig.save(ConfigFileFixture.VERSION_5, false);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Value '5' of attribute 'schemaVersion' of element 'cruise' is not valid"));
        }
    }

    @Test
    public void shouldUpgradeOldXmlWhenRequestedTo() throws Exception {
        cachedGoConfig.save(ConfigFileFixture.VERSION_5, true);
        CruiseConfig cruiseConfig = goConfigFileDao.load();
        assertThat(cruiseConfig.getAllPipelineConfigs().size(), is(1));
        assertThat(cruiseConfig.getAllPipelineConfigs().get(0).name(), is(new CaseInsensitiveString("framework")));
    }

    @Test
    public void shouldLogAnyErrorMessageIncludingTheValidationError() throws Exception {
        try {
            cachedGoConfig.save(INVALID_CONFIG_WITH_TYPE_FOR_ARTIFACT, false);
            fail();
        } catch (Exception e) {
            assertThat(logger.getLog(),
                    containsString(
                            "'type' is not allowed to appear in element 'test'."));
        }
    }

    @Test
    public void shouldUpdateBuildInConfigFileWithXmlPartial() throws Exception {
        useConfigString(ConfigFileFixture.CRUISE);
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("mac"), new Resources("osx"), new ArtifactPlans());
        goConfigFileDao.updateBuild("cruise", "dev", 1, jobConfig, goConfigFileDao.md5OfConfigFile());
        CruiseConfig config = goConfigFileDao.load();
        JobConfigs jobConfigs = config.stageConfigByName(new CaseInsensitiveString("cruise"), new CaseInsensitiveString("dev")).allBuildPlans();
        assertThat(jobConfigs.size(), is(2));
        assertThat(jobConfigs.get(0).name(), is(new CaseInsensitiveString("linux")));
        assertThat(jobConfigs.get(1).name(), is(new CaseInsensitiveString("mac")));
    }

    @Test
    public void should_NOT_allowUpdateOf_serverId() throws Exception {
        useConfigString(ConfigFileFixture.CRUISE);
        String oldServerId = goConfigFileDao.load().server().getServerId();
        Exception ex = null;
        try {
            GoConfigFileHelper.withServerIdImmutability(new Procedure() {
                public void call() {
                    goConfigFileDao.updateConfig(new UpdateConfigCommand() {
                        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                            ReflectionUtil.setField(cruiseConfig.server(), "serverId", "new-value");
                            return cruiseConfig;
                        }
                    });
                }
            });
            fail("should not save with modified serverId");
        } catch (Exception e) {
            ex = e;
        }
        assertThat(ex.getMessage(), is("The value of 'serverId' uniquely identifies a Go server instance. This field cannot be modified."));
        CruiseConfig config = goConfigFileDao.load();
        assertThat(config.server().getServerId(), is(oldServerId));
    }

    @Test
    public void shouldUpdateStageInConfigFileWithXmlPartial() throws Exception {
        useConfigString(ConfigFileFixture.CRUISE);
        JobConfigs jobConfigs = goConfigFileDao.load().stageConfigByName(new CaseInsensitiveString("cruise"), new CaseInsensitiveString("dev")).allBuildPlans();
        StageConfig stage = new StageConfig(new CaseInsensitiveString("ft"), jobConfigs);
        goConfigFileDao.updateStage("cruise", 0, stage, goConfigFileDao.md5OfConfigFile());
        CruiseConfig config = goConfigFileDao.load();
        PipelineConfig pipeline = config.pipelineConfigByName(new CaseInsensitiveString("cruise"));
        assertThat(pipeline.size(), is(1));
        assertThat(pipeline.get(0).name(), is(new CaseInsensitiveString("ft")));
    }

    @Test
    public void shouldNotConfigMultipleTrackingTools() throws Exception {
        try {
            useConfigString(INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("One of '{materials}' is expected"));
        }
    }

    @Test
    public void shouldMergeWithLatestConfigWhenConfigUpdatedWithOlderMd5() {
        configHelper.addMailHost(getMailhost("mailhost.local.old"));
        final String oldMd5 = goConfigFileDao.md5OfConfigFile();
        configHelper.addMailHost(getMailhost("mailhost.local"));

        ConfigSaveState configSaveState = goConfigFileDao.updateConfig(configHelper.addPipelineCommand(oldMd5, "p2", "stage1", "build1"));

        CruiseConfig updatedConfig = goConfigFileDao.load();
        assertThat(updatedConfig.hasPipelineNamed(new CaseInsensitiveString("p2")), is(true));
        assertThat(updatedConfig.mailHost().getHostName(), is("mailhost.local"));
        assertThat(configSaveState, is(ConfigSaveState.MERGED));
    }

    private void assertCurrentConfigIs(CruiseConfig cruiseConfig) throws Exception {
        CruiseConfig currentConfig = goConfigFileDao.load();
        assertThat(currentConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).size(),
                is(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).size()));
    }


    public void useConfigString(String config) throws Exception {
        configHelper.writeXmlToConfigFile(ConfigMigrator.migrate(config));
    }

    class CheckedTestUpdateCommand implements NoOverwriteUpdateConfigCommand, CheckedUpdateCommand, ConfigAwareUpdate {

        private final String md5;
        private final boolean canContinue;
        private boolean wasUpdated;
        private CruiseConfig after;
        CheckedTestUpdateCommand(String md5, boolean canContinue) {
            this.md5 = md5;
            this.canContinue = canContinue;
        }

        public boolean canContinue(CruiseConfig cruiseConfig) {
            return canContinue;
        }

        public String unmodifiedMd5() {
            return md5;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            wasUpdated = true;
            return cruiseConfig;
        }

        public void afterUpdate(CruiseConfig cruiseConfig) {
            after = cruiseConfig;
        }

        public CruiseConfig configAfter() {
            return after;
        }

    }
    private MailHost getMailhost(String hostname) {
        return new MailHost(hostname, 9999, "user", "password", true, false, "from@local", "admin@local");
    }

}
