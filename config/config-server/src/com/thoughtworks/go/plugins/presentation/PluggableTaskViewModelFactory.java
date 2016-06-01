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
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.presentation.MissingPluggableTaskViewModel;
import com.thoughtworks.go.presentation.PluggableTaskViewModel;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands creating a view model for a pluggable task.
 */
public class PluggableTaskViewModelFactory implements PluggableViewModelFactory<PluggableTask> {

    private static final Logger LOG = Logger.getLogger(PluggableTaskViewModelFactory.class);

    private Map<String, String> viewTemplates = new HashMap<>();
    private static final Pattern CLASSPATH_MATCHER_PATTERN = Pattern.compile("^classpath:(.+)");

    public PluggableTaskViewModelFactory() {
        viewTemplates.put("new", "admin/tasks/pluggable_task/new");
        viewTemplates.put("edit", "admin/tasks/pluggable_task/edit");
        viewTemplates.put("list-entry", "admin/tasks/pluggable_task/_list_entry.html");
    }

    public PluggableViewModel<PluggableTask> viewModelFor(final PluggableTask pluggableTask, String actionName) {
        if (PluggableTaskConfigStore.store().hasPreferenceFor(pluggableTask.getPluginConfiguration().getId())) {
            TaskPreference taskPreference = PluggableTaskConfigStore.store().preferenceFor(pluggableTask.getPluginConfiguration().getId());
            return new PluggableTaskViewModel(pluggableTask, viewTemplates.get(actionName), Renderer.ERB, taskPreference.getView().displayValue(), getTemplate(taskPreference.getView()));
        }
        return new MissingPluggableTaskViewModel(pluggableTask, viewTemplates.get(actionName), Renderer.ERB);
    }

    private String getTemplate(final TaskView view) {
        final String templateString = view.template();
        if (templateString == null) {
            return "View template provided by plugin is null.";
        }

        final Matcher matcher = CLASSPATH_MATCHER_PATTERN.matcher(templateString);
        if (matcher.matches()) {
            return loadTemplateFromClasspath(matcher.group(1), view);
        }
        else return templateString;
    }

    private String loadTemplateFromClasspath(final String filepath, final TaskView view) {
        InputStream in = null;
        try {
            in = view.getClass().getResourceAsStream(filepath);
            return in != null ? IOUtils.toString(in) : String.format("Template \"%s\" is missing.", filepath);
        } catch (IOException e) {
            LOG.error(String.format("Failed to load template from view from path \"%s\". Make sure your the template is" +
                    " on the classpath of your plugin", filepath), e);
            return String.format("Template \"%s\" failed to load.", filepath);
        } finally {
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }
    }
}
