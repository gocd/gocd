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

package com.thoughtworks.go.server.service;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminServiceTest {
    private GoConfigService goConfigService;
    private AdminService adminService;
    private String fileLocation = "file location";
    private MetricsProbeService metricsProbeService;

    @Before
    public void setup() throws Exception {
        goConfigService = mock(GoConfigService.class);
        metricsProbeService = mock(MetricsProbeService.class);
        adminService = new AdminService(goConfigService, metricsProbeService);
    }

    @Test
    public void shouldGenerateConfigurationJson() throws Exception {
        GoConfigService.XmlPartialSaver fileSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(fileSaver.asXml()).thenReturn("xml content");
        when(fileSaver.getMd5()).thenReturn("md5 value");
        when(goConfigService.fileSaver(false)).thenReturn(fileSaver);
        when(goConfigService.fileLocation()).thenReturn(fileLocation);

        final Json json = adminService.configurationJsonForSourceXml();
        JsonMap expectedJson = new JsonMap();
        JsonMap expectedConfig = new JsonMap();
        expectedConfig.put("location", fileLocation);
        expectedConfig.put("content", "xml content");
        expectedConfig.put("md5", "md5 value");
        expectedJson.put("config", expectedConfig);
        assertThat(json.contains(expectedJson), is(true));
    }

    @Test
    public void shouldGenerateConfigurationAsMapForSourceXml() throws Exception {
        Map expected = mock(HashMap.class);
        doNothing().when(goConfigService).populateAdminModel(anyMap());
        Map actual = adminService.configurationMapForSourceXml();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldPopulateAdminModel() throws Exception {
        Map expected = mock(HashMap.class);
        doNothing().when(goConfigService).populateAdminModel(expected);
        Map actual = adminService.populateModel(expected);
        assertThat(actual, is(expected));
        verify(goConfigService).populateAdminModel(expected);
    }

    @Test
    public void shouldUpdateConfig() throws Exception {
        HashMap attributes = new HashMap();
        String content = "config_xml";
        attributes.put("content", content);
        String md5 = "config_md5";
        attributes.put("md5", md5);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        GoConfigService.XmlPartialSaver fileSaver = mock(GoConfigService.XmlPartialSaver.class);
        when(fileSaver.saveXml(content, md5)).thenReturn(GoConfigValidity.valid());
        when(goConfigService.fileSaver(false)).thenReturn(fileSaver);

        adminService.updateConfig(attributes, result);

        assertThat(result.isSuccessful(), is(true));
        verify(fileSaver).saveXml(content, md5);
        verify(goConfigService).fileSaver(false);
    }


    @Test
    public void shouldReturnInvalidIfConfigIsNotSaved() throws Exception {
        HashMap attributes = new HashMap();
        String content = "invalid_config_xml";
        attributes.put("content", content);
        String md5 = "config_md5";
        attributes.put("md5", md5);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        GoConfigService.XmlPartialSaver fileSaver = mock(GoConfigService.XmlPartialSaver.class);
        GoConfigValidity validity = GoConfigValidity.invalid("Wrong config xml");
        when(fileSaver.saveXml(content, md5)).thenReturn(validity);
        when(goConfigService.fileSaver(false)).thenReturn(fileSaver);

        GoConfigValidity actual = adminService.updateConfig(attributes, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(actual.isValid(), is(false));
        assertThat(actual.errorMessage(), is("Wrong config xml"));
        
        verify(fileSaver).saveXml(content, md5);
        verify(goConfigService).fileSaver(false);
    }
}
