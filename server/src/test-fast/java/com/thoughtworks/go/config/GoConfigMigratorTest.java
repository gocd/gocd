/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoConfigMigratorTest {

    @Mock private GoConfigMigration goConfigMigration;
    @Mock private SystemEnvironment systemEnvironment;
    @Mock private FullConfigSaveNormalFlow fullConfigSaveNormalFlow;
    @Mock MagicalGoConfigXmlLoader loader;
    @Mock GoConfigFileReader reader;
    @Mock ConfigRepository configRepository;
    @Mock ServerHealthService serverHealthService;

    private GoConfigMigrator goConfigMigrator;

    @BeforeEach
    public void setup() {
        goConfigMigrator = new GoConfigMigrator(goConfigMigration, systemEnvironment, fullConfigSaveNormalFlow, loader,
                reader, configRepository, serverHealthService, null);
    }

    @Test
    public void shouldUpgradeConfigToLatestVersion() throws Exception {
        String configXml = "cruise_config_xml_contents";

        when(reader.configXml()).thenReturn(configXml);

        goConfigMigrator.migrate();

        verify(goConfigMigration).upgradeIfNecessary(configXml);
    }

    @Test
    public void shouldExecuteFullConfigSaveWithNormalFlowPostConfigUpgrade() throws Exception {
        String configXml = "cruise_config_xml_contents";
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        GoConfigHolder goConfigHolder = mock(GoConfigHolder.class);

        when(reader.configXml()).thenReturn(configXml);
        when(goConfigMigration.upgradeIfNecessary(configXml)).thenReturn(configXml);
        when(loader.deserializeConfig(configXml)).thenReturn(cruiseConfig);
        when(fullConfigSaveNormalFlow.execute(commandArgumentCaptor.capture(), listArgumentCaptor.capture(), stringArgumentCaptor.capture())).thenReturn(goConfigHolder);

        GoConfigHolder configHolder = goConfigMigrator.migrate();

        assertThat(configHolder, is(goConfigHolder));
        assertThat(stringArgumentCaptor.getValue(), is("Upgrade"));
        assertThat(commandArgumentCaptor.getValue().configForEdit(), is(cruiseConfig));
        assertThat(listArgumentCaptor.getValue().size(), is(0));
    }

    @Test
    public void shouldUpgradeConfigUsingLatestConfigRevisionInRepoIfUpgradingConfigFileFails() throws Exception {
        String configInFile = "cruise_config_in_file";
        String versionedConfig = "versioned_cruise_config";
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        when(systemEnvironment.getCruiseConfigFile()).thenReturn("");
        when(reader.configXml()).thenReturn(configInFile).thenReturn(versionedConfig);
        when(configRepository.getCurrentRevision()).thenReturn(mock(GoConfigRevision.class));
        when(goConfigMigration.upgradeIfNecessary(configInFile)).thenThrow(new RuntimeException());
        when(goConfigMigration.upgradeIfNecessary(versionedConfig)).thenReturn(versionedConfig);
        when(loader.deserializeConfig(versionedConfig)).thenReturn(cruiseConfig);
        when(goConfigMigration.revertFileToVersion(any(File.class), any(GoConfigRevision.class))).thenReturn(new File("path_to_backup_file"));
        when(fullConfigSaveNormalFlow.execute(commandArgumentCaptor.capture(), listArgumentCaptor.capture(), stringArgumentCaptor.capture())).thenReturn(mock(GoConfigHolder.class));

        goConfigMigrator.migrate();

        assertThat(stringArgumentCaptor.getValue(), is("Upgrade"));
        assertThat(commandArgumentCaptor.getValue().configForEdit(), is(cruiseConfig));
        assertThat(listArgumentCaptor.getValue().size(), is(0));
        verify(goConfigMigration).revertFileToVersion(any(File.class), any(GoConfigRevision.class));
    }


//    TODO: Test shouldErrorOutIfConfigFileUpgradeFailsAndInAbsenceOfVersionedConfigFile
}
