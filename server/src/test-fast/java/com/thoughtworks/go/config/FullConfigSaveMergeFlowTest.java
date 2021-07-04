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

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.exceptions.ConfigMergeException;
import com.thoughtworks.go.config.exceptions.ConfigMergePostValidationException;
import com.thoughtworks.go.config.exceptions.ConfigMergePreValidationException;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.TimeProvider;
import org.jdom2.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class FullConfigSaveMergeFlowTest {
    private MagicalGoConfigXmlLoader loader;
    private MagicalGoConfigXmlWriter writer;
    private ConfigElementImplementationRegistry configElementImplementationRegistry;
    private FullConfigSaveMergeFlow flow;
    private FullConfigUpdateCommand updateConfigCommand;
    private CruiseConfig configForEdit;
    private Document document;
    private GoConfigFileWriter fileWriter;
    private TimeProvider timeProvider;
    private ConfigRepository configRepository;
    private CachedGoPartials cachedGoPartials;
    private List<PartialConfig> partials;

    @BeforeEach
    public void setup() throws Exception {
        configForEdit = mock(CruiseConfig.class);
        updateConfigCommand = new FullConfigUpdateCommand(configForEdit, "md5");
        loader = mock(MagicalGoConfigXmlLoader.class);
        writer = mock(MagicalGoConfigXmlWriter.class);
        document = mock(Document.class);
        fileWriter = mock(GoConfigFileWriter.class);
        timeProvider = mock(TimeProvider.class);
        configRepository = mock(ConfigRepository.class);
        cachedGoPartials = mock(CachedGoPartials.class);
        configElementImplementationRegistry = mock(ConfigElementImplementationRegistry.class);
        partials = new ArrayList<>();

        flow = new FullConfigSaveMergeFlow(loader, writer, configElementImplementationRegistry, timeProvider,
                configRepository, cachedGoPartials, fileWriter);
        when(writer.documentFrom(configForEdit)).thenReturn(document);
        when(writer.toString(document)).thenReturn("cruise_config");
    }

    @Test
    public void shouldUpdateGivenConfigWithPartials() throws Exception {
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        flow.execute(updateConfigCommand, partials, null);

        verify(configForEdit).setPartials(partials);
    }

    @Test
    public void shouldPreprocessAndValidateTheUpdatedConfig() throws Exception {
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        flow.execute(updateConfigCommand, partials, null);

        verify(loader).preprocessAndValidate(configForEdit);
    }

    @Test
    public void shouldValidateDomRepresentationOfCruiseConfig() throws Exception {
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        flow.execute(updateConfigCommand, partials, null);

        verify(writer).verifyXsdValid(document);
    }

    @Test
    public void shouldErrorOutIfPreprocessOrValidateFails() throws Exception {
        when(loader.preprocessAndValidate(configForEdit)).thenThrow(new Exception());
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        assertThatThrownBy(() -> flow.execute(updateConfigCommand, partials, null))
                .isInstanceOf(ConfigMergePreValidationException.class);
    }

    @Test
    public void shouldErrorOutIfSavingConfigPostValidationFails() throws Exception {
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenThrow(new Exception());
        when(configRepository.getConfigMergedWithLatestRevision(any(GoConfigRevision.class), anyString())).thenReturn("merged_config");

        assertThatThrownBy(() -> flow.execute(updateConfigCommand, partials, null))
                .isInstanceOf(ConfigMergePostValidationException.class);
    }

    @Test
    public void shouldErrorOutIfConfigMergeFails() throws Exception {
        when(configRepository.getConfigMergedWithLatestRevision(any(GoConfigRevision.class), anyString())).thenThrow(new ConfigMergeException("merge fails"));

        assertThatThrownBy(() -> flow.execute(updateConfigCommand, partials, null))
                .isInstanceOf(ConfigMergeException.class);
    }

    @Test
    public void shouldMergeConfigWithLatestRevision() throws Exception {
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        flow.execute(updateConfigCommand, partials, null);

        verify(configRepository).getConfigMergedWithLatestRevision(new GoConfigRevision("config", "temporary-md5-for-branch", "user", "version", new TimeProvider()), "md5");
    }

    @Test
    public void shouldLoadConfigHolderFromTheMergedConfigXML() throws Exception {
        String mergedConfig = "merged_config";

        when(configRepository.getConfigMergedWithLatestRevision(any(GoConfigRevision.class), any(String.class))).thenReturn(mergedConfig);
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        flow.execute(updateConfigCommand, partials, null);

        verify(loader).loadConfigHolder(any(String.class), any(MagicalGoConfigXmlLoader.Callback.class));
    }

    @Test
    public void shouldPersistXmlRepresentationOfMergedCruiseConfig() throws Exception {
        String mergedConfig = "merged_config";

        when(configRepository.getConfigMergedWithLatestRevision(any(GoConfigRevision.class), any(String.class))).thenReturn(mergedConfig);
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        flow.execute(updateConfigCommand, partials, null);

        verify(fileWriter).writeToConfigXmlFile(mergedConfig);
    }

    @Test
    public void shouldCheckinGeneratedConfigXMLToConfigRepo() throws Exception {
        String mergedConfig = "merged_config";
        Date currentTime = mock(Date.class);
        ArgumentCaptor<GoConfigRevision> revisionArgumentCaptor = ArgumentCaptor.forClass(GoConfigRevision.class);

        when(configRepository.getConfigMergedWithLatestRevision(any(GoConfigRevision.class), any(String.class))).thenReturn(mergedConfig);
        when(timeProvider.currentTime()).thenReturn(currentTime);
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));
        doNothing().when(configRepository).checkin(revisionArgumentCaptor.capture());

        flow.execute(updateConfigCommand, partials, "test_user");

        GoConfigRevision goConfigRevision = revisionArgumentCaptor.getValue();
        assertThat(goConfigRevision.getContent(), is(mergedConfig));
        assertThat(goConfigRevision.getUsername(), is("test_user"));
        assertThat(goConfigRevision.getMd5(), is(updateConfigCommand.configForEdit().getMd5()));
        assertThat(goConfigRevision.getGoVersion(), is(CurrentGoCDVersion.getInstance().formatted()));
    }

    @Test
    public void shouldUpdateCachedGoPartialsWithValidPartials() throws Exception {
        String mergedConfig = "merged_config";
        ArrayList<PartialConfig> partials = new ArrayList<>();

        when(configRepository.getConfigMergedWithLatestRevision(any(GoConfigRevision.class), any(String.class))).thenReturn(mergedConfig);
        when(loader.loadConfigHolder(nullable(String.class), any(MagicalGoConfigXmlLoader.Callback.class)))
                .thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));
        InOrder inOrder = inOrder(configRepository, fileWriter, cachedGoPartials);

        flow.execute(updateConfigCommand, partials, null);

        inOrder.verify(configRepository).checkin(any(GoConfigRevision.class));
        inOrder.verify(fileWriter).writeToConfigXmlFile(any(String.class));
        inOrder.verify(cachedGoPartials).markAsValid(partials);
    }
}
