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
package com.thoughtworks.go.config.registry;

import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Deny;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.rules.Directive;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugins.presentation.BuiltinTaskViewModelFactory;
import com.thoughtworks.go.plugins.presentation.PluggableTaskViewModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigElementImplementationRegistrar {
    private ConfigElementImplementationRegistry registry;

    @Autowired
    public ConfigElementImplementationRegistrar(ConfigElementImplementationRegistry registry) {
        this.registry = registry;
    }

    public void initialize() {
        registerBuiltinTasks();
        registerBuiltinMaterials();
        registerBuiltinUserTypes();
        registerBuiltinArtifactTypes();
        registerRoleTypes();
        registerDirectiveTypes();
    }

    private void registerRoleTypes() {
        registry.registerImplementer(Role.class, RoleConfig.class, PluginRoleConfig.class);
    }

    private void registerDirectiveTypes() {
        registry.registerImplementer(Directive.class, Allow.class, Deny.class);
    }

    private void registerBuiltinTasks() {
        registry.registerImplementer(Task.class, AntTask.class, NantTask.class, ExecTask.class, RakeTask.class, FetchTask.class, PluggableTask.class, FetchPluggableArtifactTask.class);
        registry.registerView(AntTask.class, new BuiltinTaskViewModelFactory("ant"));
        registry.registerView(NantTask.class, new BuiltinTaskViewModelFactory("nant"));
        registry.registerView(ExecTask.class, new BuiltinTaskViewModelFactory("exec"));
        registry.registerView(RakeTask.class, new BuiltinTaskViewModelFactory("rake"));
        registry.registerView(FetchTaskAdapter.class, new BuiltinTaskViewModelFactory("fetch"));
        registry.registerView(FetchTask.class, new BuiltinTaskViewModelFactory("fetch"));
        registry.registerView(FetchPluggableArtifactTask.class, new BuiltinTaskViewModelFactory("fetch"));
        registry.registerView(PluggableTask.class, new PluggableTaskViewModelFactory());
    }

    private void registerBuiltinArtifactTypes() {
        registry.registerImplementer(ArtifactTypeConfig.class, TestArtifactConfig.class, BuildArtifactConfig.class, PluggableArtifactConfig.class);
    }

    private void registerBuiltinUserTypes() {
        registry.registerImplementer(Admin.class, AdminUser.class, AdminRole.class);
    }

    private void registerBuiltinMaterials() {
        registry.registerImplementer(MaterialConfig.class, SvnMaterialConfig.class, HgMaterialConfig.class, GitMaterialConfig.class, DependencyMaterialConfig.class, P4MaterialConfig.class,
                TfsMaterialConfig.class, PackageMaterialConfig.class, PluggableSCMMaterialConfig.class);
    }
}
