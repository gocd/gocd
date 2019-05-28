/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra.service;

import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultPluginHealthServiceTest {

    private DefaultPluginHealthService serviceDefault;
    private PluginRegistry pluginRegistry;

    @Before
    public void setUp() {
        pluginRegistry = mock(PluginRegistry.class);
        serviceDefault = new DefaultPluginHealthService(pluginRegistry);
    }

    @Test
    public void shouldMarkPluginAsInvalidWhenServiceReportsAnError() throws Exception {
        String pluginId = "plugin-id";
        String message = "plugin is broken beyond repair";
        List<String> reasons = Arrays.asList(message);
        doNothing().when(pluginRegistry).markPluginInvalid(pluginId, reasons);
        serviceDefault.reportErrorAndInvalidate(pluginId, reasons);
        verify(pluginRegistry).markPluginInvalid(pluginId, reasons);
    }

    @Test
    public void shouldNotThrowExceptionWhenPluginIsNotFound() throws Exception {
        String pluginId = "invalid-plugin";
        String message = "some msg";
        List<String> reasons = Arrays.asList(message);
        doThrow(new RuntimeException()).when(pluginRegistry).markPluginInvalid(pluginId, reasons);
        try {
            serviceDefault.reportErrorAndInvalidate(pluginId, reasons);
        } catch(Exception e) {
            fail("Should not have thrown exception");
        }
    }
}
