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

package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AuthPluginInfoViewModelTest {

    @Test
    public void shouldGetDetailsAboutThePlugin() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("GitHub Auth Plugin", "1.0", null, null, null, null);
        String pluginId = "github";
        GoPluginDescriptor descriptor = new GoPluginDescriptor(pluginId, "1.0", about, null, null, false);
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(descriptor, null, null, new Image("svg", "data", "hash"), new Capabilities(SupportedAuthType.Web, true, true), null);
        AuthPluginInfoViewModel model = new AuthPluginInfoViewModel(pluginInfo);
        assertThat(model.imageUrl(), is("/go/api/plugin_images/github/hash"));
        assertThat(model.pluginId(), is("github"));
        assertThat(model.name(), is("GitHub Auth Plugin"));
    }
}
