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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class RemoveAdminPermissionFilterTest {

    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PluginRoleService pluginRoleService;
    @Captor
    private ArgumentCaptor<ConfigChangedListener> configChangedListenerArgumentCaptor;
    private RemoveAdminPermissionFilter removeAdminPermissionFilter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        removeAdminPermissionFilter = new RemoveAdminPermissionFilter(goConfigService, null, pluginRoleService);
    }

    @Test
    public void shouldInitializeRemoveAdminPermissionFilterWithSListeners() throws Exception {
        removeAdminPermissionFilter.initialize();

        verify(goConfigService, times(2)).register(configChangedListenerArgumentCaptor.capture());

        List<ConfigChangedListener> registeredListeners = configChangedListenerArgumentCaptor.getAllValues();

        assertThat(registeredListeners.get(0), CoreMatchers.instanceOf(RemoveAdminPermissionFilter.class));
        assertThat(registeredListeners.get(0), is(removeAdminPermissionFilter));
        assertThat(registeredListeners.get(1), CoreMatchers.instanceOf(SecurityConfigChangeListener.class));
    }
}