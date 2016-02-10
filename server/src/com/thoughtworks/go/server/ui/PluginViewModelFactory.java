/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.presentation.GoPluginDescriptorModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PluginViewModelFactory {
    HashMap<String, String> typeToViewModelMap;
    DefaultPluginManager defaultPluginManager;

    public PluginViewModelFactory(DefaultPluginManager defaultPluginManager) {
        typeToViewModelMap = new HashMap<>();
        typeToViewModelMap.put(SCMPluginViewModel.TYPE, SCMPluginViewModel.class.getCanonicalName());
        typeToViewModelMap.put(PackageRepositoryPluginViewModel.TYPE, PackageRepositoryPluginViewModel.class.getCanonicalName());
        typeToViewModelMap.put(TaskPluginViewModel.TYPE, TaskPluginViewModel.class.getCanonicalName());

        this.defaultPluginManager = defaultPluginManager;
    }

    public PluginViewModel getPluginViewModel(String type, String pluginId) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (!isValidPluginType(type) || !defaultPluginManager.hasReferenceFor(GoPlugin.class, pluginId)) {
            return null;
        }

        PluginViewModel pluginViewModel = (PluginViewModel) Class.forName(typeToViewModelMap.get(type)).newInstance();
        pluginViewModel.setViewModel(pluginId, getVersion(pluginId), getMessages(pluginId));
        return pluginViewModel;
    }

    public List<PluginViewModel> getPluginViewModelsOfType(String type) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<PluginViewModel> pluginViewModels = new ArrayList<>();

        if (!isValidPluginType(type)) {
            return null;
        }

        List<GoPluginDescriptor> plugins = defaultPluginManager.plugins();
        for (GoPluginDescriptor descriptor : plugins) {
            String pluginId = descriptor.id();
            if (defaultPluginManager.isPluginOfType(type, pluginId)) {
                PluginViewModel pluginViewModel = getPluginViewModel(type, pluginId);
                pluginViewModels.add(pluginViewModel);
            }
        }
        return pluginViewModels;
    }

    public List<PluginViewModel> getAllPluginViewModels() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        List<PluginViewModel> pluginViewModels = new ArrayList<>();
        for (String type : typeToViewModelMap.keySet()) {
            pluginViewModels.addAll(getPluginViewModelsOfType(type));
        }
        getInvalidPlugins(pluginViewModels);
        return pluginViewModels;
    }

    private List<PluginViewModel> getInvalidPlugins(List<PluginViewModel> pluginViewModels) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        List<GoPluginDescriptor> plugins = defaultPluginManager.plugins();
        for (GoPluginDescriptor descriptor : plugins) {
            String pluginId = descriptor.id();
            if (defaultPluginManager.getPluginDescriptorFor(pluginId).isInvalid()) {
                DisabledPluginViewModel disabledPluginViewModel = (DisabledPluginViewModel) Class.forName(DisabledPluginViewModel.class.getCanonicalName()).newInstance();
                disabledPluginViewModel.setViewModel(pluginId, getVersion(pluginId), getMessages(pluginId));
                pluginViewModels.add(disabledPluginViewModel);
            }
        }
        return pluginViewModels;
    }

    private String getVersion(String pluginId) {
        GoPluginDescriptor goPluginDescriptor = defaultPluginManager.getPluginDescriptorFor(pluginId);
        goPluginDescriptor = GoPluginDescriptorModel.convertToDescriptorWithAllValues(goPluginDescriptor);
        return goPluginDescriptor.about().version();
    }

    private boolean isValidPluginType(String type) {
        return typeToViewModelMap.get(type) != null;
    }

    private String getMessages(String pluginId) {
        if (defaultPluginManager.getPluginDescriptorFor(pluginId).getStatus().getMessages().isEmpty()) {
            return null;
        }
        return defaultPluginManager.getPluginDescriptorFor(pluginId).getStatus().getMessages().toString();
    }
}
