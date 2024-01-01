/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.commands.CheckedUpdateCommand;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static com.thoughtworks.go.helper.ConfigFileFixture.BASIC_CONFIG;
import static com.thoughtworks.go.helper.ConfigFileFixture.INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS;
import static com.thoughtworks.go.helper.MaterialConfigsMother.hg;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;


public abstract class GoConfigDaoTestBase {
    protected GoConfigFileHelper configHelper;
    protected GoConfigDao goConfigDao;
    protected CachedGoConfig cachedGoConfig;

    @Test
    public void shouldCreateCruiseConfigFromBasicConfigFile() {
        CruiseConfig cruiseConfig = GoConfigFileHelper.load(BASIC_CONFIG);

        assertThat(cruiseConfig, is(notNullValue()));
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        assertThat(pipelineConfig.size(), is(1));
        StageConfig stageConfig = pipelineConfig.get(0);
        assertThat(stageConfig.name(), is(new CaseInsensitiveString("mingle")));
        assertThat(pipelineConfig.materialConfigs(), is(notNullValue()));
        final JobConfig cardList = stageConfig.jobConfigByInstanceName("cardlist", true);
        assertThat(cardList.name(), is(new CaseInsensitiveString("cardlist")));
        assertThat(stageConfig.jobConfigByInstanceName("bluemonkeybutt", true).name(), is(new CaseInsensitiveString("bluemonkeybutt")));
        assertThat(cardList.tasks(), iterableWithSize(1));
        assertThat(cardList.tasks().first(), instanceOf(AntTask.class));
    }

    @Test
    public void shouldThrowExceptionIfFileIsInvalid() {
        try {
            useConfigString("invalid config file");
            goConfigDao.load();
            fail("Should have thrown a parse exception");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Content is not allowed in prolog."));
        }
    }


    @Test
    public void shouldGetArtifactsFromBuildPlan() {
        CruiseConfig cruiseConfig = GoConfigFileHelper.load(BASIC_CONFIG);

        final ArtifactTypeConfigs cardListArtifacts = cruiseConfig.jobConfigByName("pipeline1", "mingle",
                "cardlist", true).artifactTypeConfigs();
        assertThat(cardListArtifacts.size(), is(0));
        assertThat(cruiseConfig.jobConfigByName("pipeline1", "mingle", "bluemonkeybutt", true).artifactTypeConfigs().size(), is(1));
    }

    @Test
    public void shouldAddPipelineToConfigFile() {
        CruiseConfig cruiseConfig = goConfigDao.load();
        int oldsize = cruiseConfig.numberOfPipelines();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring", "ut",
                "www.spring.com");
        goConfigDao.addPipeline(pipelineConfig, DEFAULT_GROUP);

        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.numberOfPipelines(), is(oldsize + 1));
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("spring")), is(pipelineConfig));
    }

    @Test
    public void shouldFailToAddDuplicatePipelineToConfigFile() {
        CruiseConfig cruiseConfig = goConfigDao.load();
        int oldsize = cruiseConfig.numberOfPipelines();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring", "ut",
                "www.spring.com");
        goConfigDao.addPipeline(pipelineConfig, DEFAULT_GROUP);

        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.numberOfPipelines(), is(oldsize + 1));
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("spring")), is(pipelineConfig));

        PipelineConfig dupPipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring", "ut",
                "www.spring.com");
        try {
            goConfigDao.addPipeline(dupPipelineConfig, DEFAULT_GROUP);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), is("You have defined multiple pipelines called 'spring'. Pipeline names must be unique."));
            return;
        }
        fail("Should have thrown");
    }

    @Test
    public void shouldFailWhenConfigUpdateCannotBeMergedWithLatestRevision() {
        final String originalMd5 = goConfigDao.load().getMd5();
        goConfigDao.updateConfig(configHelper.addPipelineCommand(originalMd5, "p1", "stage1", "build1"));
        final String md5WhenPipelineIsAdded = goConfigDao.load().getMd5();
        goConfigDao.updateConfig(configHelper.changeJobNameCommand(md5WhenPipelineIsAdded, "p1", "stage1", "build1", "new_build"));

        try {
            goConfigDao.updateConfig(new NoOverwriteUpdateConfigCommand() {
                @Override
                public String unmodifiedMd5() {
                    return md5WhenPipelineIsAdded;
                }

                @Override
                public CruiseConfig update(CruiseConfig cruiseConfig) {
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
    public void shouldNotFailNoOverwriteUpdateWhenEditingUnmodifiedCopy() {
        final String md5 = goConfigDao.md5OfConfigFile();
        try {
            ConfigSaveState configSaveState = goConfigDao.updateConfig(new NoOverwriteUpdateConfigCommand() {
                @Override
                public String unmodifiedMd5() {
                    return md5;
                }

                @Override
                public CruiseConfig update(CruiseConfig cruiseConfig) {
                    cruiseConfig.getEnvironments().add(new BasicEnvironmentConfig(new CaseInsensitiveString("foo")));
                    return cruiseConfig;
                }
            });
            assertThat(configSaveState, is(ConfigSaveState.UPDATED));
        } catch (RuntimeException e) {
            fail("should not have failed for edit on unmodified config.");
        }
    }

    @Test
    public void shouldNotFailUpdateWithOverwritePermittedWhenEditingStaleCopy() {
        try {
            goConfigDao.updateConfig(cruiseConfig -> {
                cruiseConfig.getEnvironments().add(new BasicEnvironmentConfig(new CaseInsensitiveString("foo")));
                return cruiseConfig;
            });
        } catch (RuntimeException e) {
            fail("should not have failed for edit when overwrite allowed.");
        }
    }


    @Test
    public void shouldFeedCloneOfConfigBackToCommand() {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), true) {

            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("foo"), "#{bar}-${COUNT}", null, false, new MaterialConfigs(hg("url", null)),
                        List.of(StageConfigMother.custom("stage", "job")));
                pipelineConfig.addParam(new ParamConfig("bar", "baz"));
                cruiseConfig.addPipeline("my-group", pipelineConfig);
                return cruiseConfig;
            }
        };
        goConfigDao.updateConfig(command);
        assertThat(command.after.pipelineConfigByName(new CaseInsensitiveString("foo")).getLabelTemplate(), is("baz-${COUNT}"));

        assertThat(command.after.getEnvironments().size(), is(0));
        command.after.addEnvironment("bar");
        assertThat(cachedGoConfig.currentConfig().getEnvironments().size(), is(0));
    }

    @Test
    public void shouldNotUpdateIfCannotContinueIfTheCommandIsPreprocessable() {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), false);
        try {
            goConfigDao.updateConfig(command);
            fail("should have failed as check returned false");
        } catch (ConfigUpdateCheckFailedException ignored) {
        }
        assertThat(command.wasUpdated, is(false));
        assertThat(command.after, not(nullValue()));
    }

    @Test
    public void shouldPerformUpdateIfCanContinue() {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), true);
        goConfigDao.updateConfig(command);
        assertThat(command.wasUpdated, is(true));
    }

    @Test
    public void shouldBePassedTheLatestCruiseConfigWhileCheckingBeforeUpdate() {
        configHelper.addTemplate("my-template", "my-stage");
        configHelper.addPipeline("pipeline", "stage");
        configHelper.addPipelineWithTemplate(PipelineConfigs.DEFAULT_GROUP, "my-pipeline", "my-template");
        CheckedTestUpdateCommand command = spy(new CheckedTestUpdateCommand(cachedGoConfig.loadForEditing().getMd5(), true));
        goConfigDao.updateConfig(command);
        verify(command).canContinue(cachedGoConfig.currentConfig());
    }

    @Test
    public void shouldAddEnvironmentToConfigFile() {
        CruiseConfig cruiseConfig = goConfigDao.load();
        int oldsize = cruiseConfig.getEnvironments().size();
        goConfigDao.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("foo-environment")));

        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.getEnvironments().size(), is(oldsize + 1));
    }

    @Test
    public void shouldAddPipelineOnTheTopOfSameGroupWhenGivenGroupExist() {
        PipelineConfig springConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring",
                "ut", "www.spring.com");
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("mingle",
                "ut", "www.spring.com");

        goConfigDao.addPipeline(springConfig, "group1");
        goConfigDao.addPipeline(mingleConfig, "group1");

        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.numbersOfPipeline("group1"), is(2));
        assertThat(cruiseConfig.find("group1", 0), is(mingleConfig));
    }

    @Test
    public void shouldAddPipelineToTheNewGroupWhenGivenGroupDoesNotExist() {
        PipelineConfig springConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring",
                "ut", "www.spring.com");
        PipelineConfig mingleConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("mingle",
                "ut", "www.spring.com");

        goConfigDao.addPipeline(springConfig, "group1");
        goConfigDao.addPipeline(mingleConfig, "group");

        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.numbersOfPipeline("group1"), is(1));
        assertThat(cruiseConfig.numbersOfPipeline("group"), is(1));
    }

    @Test
    public void shouldAddPipelineToDefaultGroupWhenNoGroupNameSpecified() {
        PipelineConfig springConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("spring",
                "ut", "www.spring.com");

        goConfigDao.addPipeline(springConfig, null);

        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.numbersOfPipeline(DEFAULT_GROUP), is(1));
    }

    @Test
    public void shouldAddPipelineToTheTopOfConfigFile() throws Exception {
        goConfigDao.load();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("addedFirst",
                "ut", "www.spring.com");
        PipelineConfig pipelineConfig2 = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("addedSecond",
                "ut", "www.spring.com");
        goConfigDao.addPipeline(pipelineConfig, DEFAULT_GROUP);
        goConfigDao.addPipeline(pipelineConfig2, DEFAULT_GROUP);

        goConfigDao.load();
        final File configFile = new File(goConfigDao.fileLocation());
        final String content = FileUtils.readFileToString(configFile, UTF_8);
        final int indexOfSecond = content.indexOf("addedSecond");
        final int indexOfFirst = content.indexOf("addedFirst");
        assertThat(indexOfSecond, is(not(-1)));
        assertThat(indexOfFirst, is(not(-1)));
        assertTrue(indexOfSecond < indexOfFirst);
    }

    @Test
    public void shouldNotAddInvalidPipelineToConfigFile() {
        CruiseConfig cruiseConfig = goConfigDao.load();
        int oldsize = cruiseConfig.numberOfPipelines();
        PipelineConfig pipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("", "ut",
                "www.spring.com");
        try {
            goConfigDao.addPipeline(pipelineConfig, DEFAULT_GROUP);
            fail();
        } catch (Exception ignored) {
        }
        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.numberOfPipelines(), is(oldsize));
    }

    @Test
    public void should_NOT_allowUpdateOf_serverId() throws Exception {
        useConfigString(ConfigFileFixture.CRUISE);
        String oldServerId = goConfigDao.load().server().getServerId();
        Exception ex = null;
        try {
            GoConfigFileHelper.withServerIdImmutability(() -> goConfigDao.updateConfig(cruiseConfig -> {
                ReflectionUtil.setField(cruiseConfig.server(), "serverId", "new-value");
                return cruiseConfig;
            }));
            fail("should not save with modified serverId");
        } catch (Exception e) {
            ex = e;
        }
        assertThat(ex.getMessage(), containsString("The value of 'serverId' uniquely identifies a Go server instance. This field cannot be modified"));
        CruiseConfig config = goConfigDao.load();
        assertThat(config.server().getServerId(), is(oldServerId));
    }

    @Test
    public void shouldNotConfigMultipleTrackingTools() {
        try {
            FileUtils.writeStringToFile(new File(goConfigDao.fileLocation()), INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS, UTF_8);
            goConfigDao.forceReload();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Invalid content was found starting with element 'trackingtool'. One of '{timer, environmentvariables, dependencies, materials}"));
        }
    }

    @Test
    public void shouldMergeWithLatestConfigWhenConfigUpdatedWithOlderMd5() {
        configHelper.addMailHost(getMailhost("mailhost.local.old"));
        final String oldMd5 = goConfigDao.md5OfConfigFile();
        configHelper.addMailHost(getMailhost("mailhost.local"));

        ConfigSaveState configSaveState = goConfigDao.updateConfig(configHelper.addPipelineCommand(oldMd5, "p2", "stage1", "build1"));

        CruiseConfig updatedConfig = goConfigDao.load();
        assertThat(updatedConfig.hasPipelineNamed(new CaseInsensitiveString("p2")), is(true));
        assertThat(updatedConfig.mailHost().getHostName(), is("mailhost.local"));
        assertThat(configSaveState, is(ConfigSaveState.MERGED));
    }

    @Test
    public void shouldNotUpdatePipelineConfigIfUserDoesNotHaveRequiredPermissionsToDoSo() {
        CachedGoConfig cachedConfigService = mock(CachedGoConfig.class);
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(cachedConfigService.currentConfig()).thenReturn(cruiseConfig);
        goConfigDao = new GoConfigDao(cachedConfigService);
        EntityConfigUpdateCommand<?> command = mock(EntityConfigUpdateCommand.class);
        when(command.canContinue(cruiseConfig)).thenReturn(false);
        try {
            goConfigDao.updateConfig(command, new Username(new CaseInsensitiveString("user")));
            fail("Expected to throw exception of type:" + ConfigUpdateCheckFailedException.class.getName());
        } catch (Exception e) {
            assertTrue(e instanceof ConfigUpdateCheckFailedException);
        }
        verify(cachedConfigService).currentConfig();
        verifyNoMoreInteractions(cachedConfigService);
    }

    @Test
    public void shouldUpdateValidEntity() {
        CachedGoConfig cachedConfigService = mock(CachedGoConfig.class);
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(cachedConfigService.currentConfig()).thenReturn(cruiseConfig);
        EntityConfigUpdateCommand<?> saveCommand = mock(EntityConfigUpdateCommand.class);
        when(saveCommand.isValid(cruiseConfig)).thenReturn(true);
        when(saveCommand.canContinue(cruiseConfig)).thenReturn(true);
        goConfigDao = new GoConfigDao(cachedConfigService);
        Username currentUser = new Username(new CaseInsensitiveString("user"));
        goConfigDao.updateConfig(saveCommand, currentUser);

        verify(cachedConfigService).writeEntityWithLock(saveCommand, currentUser);
    }


    public void useConfigString(String config) throws Exception {
        configHelper.writeXmlToConfigFile(ConfigMigrator.migrate(config));
    }

    static class CheckedTestUpdateCommand implements NoOverwriteUpdateConfigCommand, CheckedUpdateCommand, ConfigAwareUpdate {

        private final String md5;
        private final boolean canContinue;
        private boolean wasUpdated;
        private CruiseConfig after;

        CheckedTestUpdateCommand(String md5, boolean canContinue) {
            this.md5 = md5;
            this.canContinue = canContinue;
        }

        @Override
        public boolean canContinue(CruiseConfig cruiseConfig) {
            return canContinue;
        }

        @Override
        public String unmodifiedMd5() {
            return md5;
        }

        @Override
        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            wasUpdated = true;
            return cruiseConfig;
        }

        @Override
        public void afterUpdate(CruiseConfig cruiseConfig) {
            after = cruiseConfig;
        }

        @Override
        public CruiseConfig configAfter() {
            return after;
        }

    }

    private MailHost getMailhost(String hostname) {
        return new MailHost(hostname, 9999, "user", "password", true, false, "from@local", "admin@local");
    }

}
