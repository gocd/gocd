/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.thoughtworks.go.helper.ConfigFileFixture.BASIC_CONFIG;
import static com.thoughtworks.go.helper.ConfigFileFixture.INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;


public abstract class GoConfigDaoTestBase {
    protected GoConfigFileHelper configHelper;
    protected GoConfigDao goConfigDao;

    @Test
    public void shouldCreateCruiseConfigFromBasicConfigFile() {
        CruiseConfig cruiseConfig = GoConfigFileHelper.load(BASIC_CONFIG);

        assertThat(cruiseConfig).isNotNull();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        assertThat(pipelineConfig.size()).isEqualTo(1);
        StageConfig stageConfig = pipelineConfig.get(0);
        assertThat(stageConfig.name()).isEqualTo(new CaseInsensitiveString("mingle"));
        assertThat(pipelineConfig.materialConfigs()).isNotNull();
        final JobConfig cardList = stageConfig.jobConfigByInstanceName("cardlist", true);
        assertThat(cardList.name()).isEqualTo(new CaseInsensitiveString("cardlist"));
        assertThat(stageConfig.jobConfigByInstanceName("bluemonkeybutt", true).name()).isEqualTo(new CaseInsensitiveString("bluemonkeybutt"));
        assertThat(cardList.tasks()).hasSize(1);
        assertThat(cardList.tasks().getFirstOrNull()).isInstanceOf(AntTask.class);
    }

    @Test
    public void shouldThrowExceptionIfFileIsInvalid() {
        try {
            useConfigString("invalid config file");
            goConfigDao.currentConfig();
            fail("Should have thrown a parse exception");
        } catch (Exception expected) {
            assertThat(expected.getMessage()).contains("Content is not allowed in prolog.");
        }
    }

    @Test
    public void shouldGetArtifactsFromBuildPlan() {
        CruiseConfig cruiseConfig = GoConfigFileHelper.load(BASIC_CONFIG);

        final ArtifactTypeConfigs cardListArtifacts = cruiseConfig.jobConfigByName("pipeline1", "mingle",
                "cardlist", true).artifactTypeConfigs();
        assertThat(cardListArtifacts.size()).isEqualTo(0);
        assertThat(cruiseConfig.jobConfigByName("pipeline1", "mingle", "bluemonkeybutt", true).artifactTypeConfigs().size()).isEqualTo(1);
    }

    @Test
    public void shouldFailWhenConfigUpdateCannotBeMergedWithLatestRevision() {
        final String originalMd5 = goConfigDao.currentConfig().getMd5();
        goConfigDao.updateConfig(configHelper.addPipelineCommand(originalMd5, "p1", "stage1", "build1"));
        final String md5WhenPipelineIsAdded = goConfigDao.currentConfig().getMd5();
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
            assertThat(e.getMessage()).isEqualTo(ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH);
        }
    }

    @Test
    public void shouldNotFailNoOverwriteUpdateWhenEditingUnmodifiedCopy() {
        try {
            ConfigSaveState configSaveState = goConfigDao.updateConfig(new NoOverwriteUpdateConfigCommand() {
                @Override
                public String unmodifiedMd5() {
                    return configHelper.currentConfig().getMd5();
                }

                @Override
                public CruiseConfig update(CruiseConfig cruiseConfig) {
                    cruiseConfig.getEnvironments().add(new BasicEnvironmentConfig(new CaseInsensitiveString("foo")));
                    return cruiseConfig;
                }
            });
            assertThat(configSaveState).isEqualTo(ConfigSaveState.UPDATED);
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
    public void shouldNotUpdateIfCannotContinueIfTheCommandIsPreprocessable() {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(goConfigDao.loadForEditing().getMd5(), false);
        try {
            goConfigDao.updateConfig(command);
            fail("should have failed as check returned false");
        } catch (ConfigUpdateCheckFailedException ignored) {
        }
        assertThat(command.wasUpdated).isFalse();
    }

    @Test
    public void shouldPerformUpdateIfCanContinue() {
        CheckedTestUpdateCommand command = new CheckedTestUpdateCommand(goConfigDao.loadForEditing().getMd5(), true);
        goConfigDao.updateConfig(command);
        assertThat(command.wasUpdated).isTrue();
    }

    @Test
    public void shouldBePassedTheLatestCruiseConfigWhileCheckingBeforeUpdate() {
        configHelper.addTemplate("my-template", "my-stage");
        configHelper.addPipeline("pipeline", "stage");
        configHelper.addPipelineWithTemplate(PipelineConfigs.DEFAULT_GROUP, "my-pipeline", "my-template");
        CheckedTestUpdateCommand command = spy(new CheckedTestUpdateCommand(goConfigDao.loadForEditing().getMd5(), true));
        goConfigDao.updateConfig(command);
        verify(command).canContinue(goConfigDao.currentConfig());
    }

    @Test
    public void should_NOT_allowUpdateOf_serverId() throws Exception {
        useConfigString(ConfigFileFixture.CRUISE);
        String oldServerId = goConfigDao.currentConfig().server().getServerId();
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
        assertThat(ex.getMessage()).contains("The value of 'serverId' uniquely identifies a Go server instance. This field cannot be modified");
        CruiseConfig config = goConfigDao.currentConfig();
        assertThat(config.server().getServerId()).isEqualTo(oldServerId);
    }

    @Test
    public void shouldNotConfigMultipleTrackingTools() {
        try {
            Files.writeString(Path.of(goConfigDao.fileLocation()), INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS, UTF_8);
            goConfigDao.forceReload();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Invalid content was found starting with element 'trackingtool'. One of '{timer, environmentvariables, dependencies, materials}");
        }
    }

    @Test
    public void shouldMergeWithLatestConfigWhenConfigUpdatedWithOlderMd5() {
        configHelper.addMailHost(getMailhost("mailhost.local.old"));
        final String oldMd5 = configHelper.currentConfig().getMd5();
        configHelper.addMailHost(getMailhost("mailhost.local"));

        ConfigSaveState configSaveState = goConfigDao.updateConfig(configHelper.addPipelineCommand(oldMd5,"p2", "stage1", "build1"));

        CruiseConfig updatedConfig = goConfigDao.currentConfig();
        assertThat(updatedConfig.hasPipelineNamed(new CaseInsensitiveString("p2"))).isTrue();
        assertThat(updatedConfig.mailHost().getHostName()).isEqualTo("mailhost.local");
        assertThat(configSaveState).isEqualTo(ConfigSaveState.MERGED);
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
            assertInstanceOf(ConfigUpdateCheckFailedException.class, e);
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

    static class CheckedTestUpdateCommand implements NoOverwriteUpdateConfigCommand, CheckedUpdateCommand {

        private final String md5;
        private final boolean canContinue;
        private boolean wasUpdated;

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
        public CruiseConfig update(CruiseConfig cruiseConfig) {
            wasUpdated = true;
            return cruiseConfig;
        }
    }

    private MailHost getMailhost(String hostname) {
        return new MailHost(hostname, 9999, "user", "password", true, false, "from@local", "admin@local");
    }

}
