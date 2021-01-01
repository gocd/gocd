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
package com.thoughtworks.go.plugins.presentation;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.presentation.MissingPluggableTaskViewModel;
import com.thoughtworks.go.presentation.PluggableTaskViewModel;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @understands creating a view model for a pluggable task.
 */
public class PluggableTaskViewModelFactory implements PluggableViewModelFactory<PluggableTask> {

    private static final Logger LOG = LoggerFactory.getLogger(PluggableTaskViewModelFactory.class);

    private Map<String, String> viewTemplates = new HashMap<>();
    private static final Pattern CLASSPATH_MATCHER_PATTERN = Pattern.compile("^classpath:(.+)");

    public PluggableTaskViewModelFactory() {
        viewTemplates.put("new", "admin/tasks/pluggable_task/new");
        viewTemplates.put("edit", "admin/tasks/pluggable_task/edit");
        viewTemplates.put("list-entry", "admin/tasks/pluggable_task/_list_entry.html");
    }

    @Override
    public PluggableViewModel<PluggableTask> viewModelFor(final PluggableTask pluggableTask, String actionName) {
        if (PluggableTaskConfigStore.store().hasPreferenceFor(pluggableTask.getPluginConfiguration().getId())) {
            TaskPreference taskPreference = PluggableTaskConfigStore.store().preferenceFor(pluggableTask.getPluginConfiguration().getId());
            return new PluggableTaskViewModel(pluggableTask, viewTemplates.get(actionName), taskPreference.getView().displayValue(), getTemplate(taskPreference.getView()));
        }
        return new MissingPluggableTaskViewModel(pluggableTask, viewTemplates.get(actionName));
    }

    private String getTemplate(final TaskView view) {
        final String templateString = view.template();
        if (templateString == null) {
            return "View template provided by plugin is null.";
        }

        final Matcher matcher = CLASSPATH_MATCHER_PATTERN.matcher(templateString);
        if (matcher.matches()) {
            return loadTemplateFromClasspath(matcher.group(1), view);
        } else return templateString;
    }

    private String loadTemplateFromClasspath(final String filepath, final TaskView view) {
        try(InputStream in = view.getClass().getResourceAsStream(filepath)) {
            return in != null ? IOUtils.toString(in, UTF_8) : String.format("Template \"%s\" is missing.", filepath);
        } catch (IOException e) {
            LOG.error("Failed to load template from view from path \"{}\". Make sure your the template is on the classpath of your plugin", filepath, e);
            return String.format("Template \"%s\" failed to load.", filepath);
        }
    }
}
