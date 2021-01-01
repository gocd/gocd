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
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.TimeProvider;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class FullConfigSaveNormalFlowTest {
    private MagicalGoConfigXmlLoader loader;
    private MagicalGoConfigXmlWriter writer;
    private ConfigElementImplementationRegistry configElementImplementationRegistry;
    private FullConfigSaveNormalFlow flow;
    private FullConfigUpdateCommand updateConfigCommand;
    private CruiseConfig configForEdit;
    private Document document;
    private GoConfigFileWriter fileWriter;
    private TimeProvider timeProvider;
    private ConfigRepository configRepository;
    private CachedGoPartials cachedGoPartials;
    private List<PartialConfig> partials;

    @Before
    public void setup() throws Exception {
        configForEdit = new BasicCruiseConfig();
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

        flow = new FullConfigSaveNormalFlow(loader, writer, configElementImplementationRegistry, timeProvider,
                configRepository, cachedGoPartials, fileWriter);

        when(writer.documentFrom(configForEdit)).thenReturn(document);
        when(loader.preprocessAndValidate(configForEdit)).thenReturn(new BasicCruiseConfig());
        when(writer.toString(document)).thenReturn("cruise_config");
    }

    @Test
    public void shouldUpdateGivenConfigWithPartials() throws Exception {
        flow.execute(updateConfigCommand, partials, null);

        assertThat(configForEdit.getPartials(), is(partials));
    }

    @Test
    public void shouldPreprocessAndValidateTheUpdatedConfigForEdit() throws Exception {
        flow.execute(updateConfigCommand, partials, null);

        verify(loader).preprocessAndValidate(configForEdit);
    }

    @Test
    public void shouldValidateDomRepresentationOfCruiseConfig() throws Exception {
        flow.execute(updateConfigCommand, partials, null);

        verify(writer).verifyXsdValid(document);
    }

    @Test
    public void shouldPersistXmlRepresentationOfConfigForEdit() throws Exception {
        String configAsXml = "cruise config as xml";

        when(writer.toString(document)).thenReturn(configAsXml);

        flow.execute(updateConfigCommand, partials, null);

        verify(fileWriter).writeToConfigXmlFile(configAsXml);
    }

    @Test
    public void shouldCheckinGeneratedConfigXMLToConfigRepo() throws Exception {
        String configAsXml = "cruise config as xml";
        Date currentTime = mock(Date.class);
        ArgumentCaptor<GoConfigRevision> revisionArgumentCaptor = ArgumentCaptor.forClass(GoConfigRevision.class);

        when(writer.toString(document)).thenReturn(configAsXml);
        when(timeProvider.currentTime()).thenReturn(currentTime);
        doNothing().when(configRepository).checkin(revisionArgumentCaptor.capture());

        flow.execute(updateConfigCommand, partials, "test_user");


        GoConfigRevision goConfigRevision = revisionArgumentCaptor.getValue();
        assertThat(goConfigRevision.getContent(), is(configAsXml));
        assertThat(goConfigRevision.getUsername(), is("test_user"));
        assertThat(goConfigRevision.getMd5(), is(updateConfigCommand.configForEdit().getMd5()));
        assertThat(goConfigRevision.getGoVersion(), is(CurrentGoCDVersion.getInstance().formatted()));
        assertThat(goConfigRevision.getTime(), is(currentTime));
    }

    @Test
    public void shouldUpdateCachedGoPartialsWithValidPartialsPostAllSteps() throws Exception {
        String configAsXml = "cruise config as xml";

        when(writer.toString(document)).thenReturn(configAsXml);
        InOrder inOrder = inOrder(configRepository, fileWriter, cachedGoPartials);

        flow.execute(updateConfigCommand, partials, null);

        inOrder.verify(configRepository).checkin(any(GoConfigRevision.class));
        inOrder.verify(fileWriter).writeToConfigXmlFile(any(String.class));
        inOrder.verify(cachedGoPartials).markAsValid(partials);
    }
}
