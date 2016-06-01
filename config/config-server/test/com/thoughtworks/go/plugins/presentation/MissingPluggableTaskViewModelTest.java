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

package com.thoughtworks.go.plugins.presentation;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.presentation.MissingPluggableTaskViewModel;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MissingPluggableTaskViewModelTest {
    @Test
    public void shouldReturnPluginIdAsDisplayValue() throws Exception {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-1", "1.0");
        PluggableTask pluggableTask = new PluggableTask(pluginConfiguration, new Configuration());

        MissingPluggableTaskViewModel viewModel = new MissingPluggableTaskViewModel(pluggableTask, null, null);
        assertThat((String) viewModel.getParameters().get("template"), is(String.format("Associated plugin '%s' not found. Please contact the Go admin to install the plugin.", pluginConfiguration.getId())));
        assertThat(viewModel.getTypeForDisplay(), is(pluginConfiguration.getId()));
    }
}
